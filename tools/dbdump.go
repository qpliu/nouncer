package main

import (
	"database/sql"
	"fmt"
	"math"
	"os"
	"time"

	_ "github.com/mattn/go-sqlite3"
)

type Pt struct {
	Lat  float64
	Lon  float64
	Elev float64
}

func Dist(pt1, pt2 *Pt) float64 {
	sdlat2 := math.Sin((pt1.Lat - pt2.Lat) / 2 * math.Pi / 180)
	sdlon2 := math.Sin((pt1.Lon - pt2.Lon) / 2 * math.Pi / 180)
	c1 := math.Cos(pt1.Lat * math.Pi / 180)
	c2 := math.Cos(pt2.Lat * math.Pi / 180)
	return 2 * 6371000 * math.Asin(math.Sqrt(sdlat2*sdlat2+c1*c2*sdlon2*sdlon2))
}

type Location struct {
	Id   uint64
	Name string
	Pt
}

type Point struct {
	Pt
	Time time.Time
}

type Track struct {
	Location
	Entry          time.Time
	Exit           time.Time
	EntryTimestamp time.Time
	ExitTimestamp  time.Time
}

func GetLocation(rows *sql.Rows) *Location {
	loc := &Location{}
	if err := rows.Scan(&loc.Id, &loc.Name, &loc.Lat, &loc.Lon, &loc.Elev); err != nil {
		panic(err)
	}
	return loc
}

func GetPoint(rows *sql.Rows) *Point {
	p := &Point{}
	var t int64
	if err := rows.Scan(&p.Lat, &p.Lon, &p.Elev, &t); err != nil {
		panic(err)
	}
	p.Time = time.Unix(t/1000, (t%1000)*1000000)
	return p
}

func GetTrack(rows *sql.Rows) *Track {
	t := &Track{}
	var entry, exit, entryTimestamp, exitTimestamp int64
	if err := rows.Scan(&t.Id, &t.Name, &t.Lat, &t.Lon, &t.Elev, &entry, &exit, &entryTimestamp, &exitTimestamp); err != nil {
		panic(err)
	}
	t.Entry = time.Unix(entry/1000, (entry%1000)*1000000)
	t.Exit = time.Unix(exit/1000, (exit%1000)*1000000)
	t.EntryTimestamp = time.Unix(entryTimestamp/1000, (entryTimestamp%1000)*1000000)
	t.ExitTimestamp = time.Unix(exitTimestamp/1000, (exitTimestamp%1000)*1000000)
	return t
}

func GetLocations(db *sql.DB) []*Location {
	var locs []*Location
	rows, err := db.Query("SELECT id, name, latitude, longitude, elevation FROM location")
	if err != nil {
		panic(err)
	}
	defer rows.Close()
	for rows.Next() {
		locs = append(locs, GetLocation(rows))
	}
	return locs
}

func NearestLocation(p *Point, locs []*Location, maxDist float64) (*Location, float64) {
	var nearest *Location
	for _, loc := range locs {
		dist := Dist(&p.Pt, &loc.Pt)
		if dist < maxDist {
			maxDist = dist
			nearest = loc
		}
	}
	return nearest, maxDist
}

func ExtrapolateTime(p1, p2 *Point, loc *Location) (time.Time, float64) {
	dt := time.Second
	if p1.Time.After(p2.Time) {
		dt = -time.Second
	}
	dlat := (p2.Lat - p1.Lat) / (float64(p2.Time.Sub(p1.Time))) * float64(dt)
	dlon := (p2.Lon - p1.Lon) / (float64(p2.Time.Sub(p1.Time))) * float64(dt)
	p := Point{Pt: Pt{p1.Lat, p1.Lon, p1.Elev}, Time: p1.Time}
	dist := Dist(&p.Pt, &loc.Pt)
	for {
		p.Lat += dlat
		p.Lon += dlon
		d := Dist(&p.Pt, &loc.Pt)
		if d > dist {
			return p.Time, dist
		}
		dist = d
		p.Time = p.Time.Add(dt)
	}
}

func main() {
	db, err := sql.Open("sqlite3", os.Args[1])
	if err != nil {
		panic(err)
	}
	defer db.Close()

	locs := GetLocations(db)
	points, err := db.Query("SELECT latitude, longitude, elevation, time FROM point ORDER BY time ASC")
	if err != nil {
		panic(err)
	}
	defer points.Close()
	tracks, err := db.Query("SELECT location.id, name, latitude, longitude, elevation, entry_time, coalesce(exit_time,entry_time), entry_timestamp, coalesce(exit_timestamp,entry_timestamp) FROM location, track WHERE location.id = location_id ORDER BY track.id ASC")
	if err != nil {
		panic(err)
	}
	defer tracks.Close()

	entered := false
	var p, lastPoint *Point
	var t *Track
	for {
		if p == nil && points.Next() {
			p = GetPoint(points)
		}
		if t == nil && tracks.Next() {
			t = GetTrack(tracks)
			entered = false
		}
		if p == nil && t == nil {
			break
		}
		printPoint := p != nil
		if t != nil {
			if !entered && (p == nil || p.Time.After(t.Entry)) {
				fmt.Printf("%s ENTER: %.23s (timestamp:%s)\n", t.Entry.Format("15:04:05"), t.Name, t.EntryTimestamp.Format("15:04:05"))
				entered = true
			}
			if entered && (p == nil || p.Time.After(t.Exit)) {
				fmt.Printf("%s EXIT: %.23s (timestamp:%s)\n", t.Exit.Format("15:04:05"), t.Name, t.ExitTimestamp.Format("15:04:05"))
				t = nil
				printPoint = false
			}
		}
		if printPoint {
			loc, dist := NearestLocation(p, locs, 500)
			s := fmt.Sprintf("%s (%.0f)", p.Time.Format("15:04:05"), p.Elev*3.28084)
			if loc == nil {
				fmt.Printf("%s\n", s)
			} else {
				fmt.Printf("%-18s %5.2f %.23s (%.0f)", s, dist, loc.Name, loc.Elev*3.28084)
				if lastPoint != nil {
					lastDist := Dist(&lastPoint.Pt, &loc.Pt)
					if lastDist > dist {
						t, d := ExtrapolateTime(lastPoint, p, loc)
						fmt.Printf(" ENTRY:%s %5.2f", t.Format("15:04:05"), d)
					} else {
						t, d := ExtrapolateTime(p, lastPoint, loc)
						fmt.Printf(" EXIT:%s %5.2f", t.Format("15:04:05"), d)
					}
				}
				fmt.Printf("\n")
			}
			lastPoint = p
			p = nil
		}
	}
}
