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
	Tag  string
}

type Track struct {
	Location
	Entry          time.Time
	Exit           time.Time
	EntryHeading   float64
	ExitHeading    float64
	EntrySpeed     float64
	ExitSpeed      float64
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
	if err := rows.Scan(&p.Lat, &p.Lon, &p.Elev, &t, &p.Tag); err != nil {
		panic(err)
	}
	p.Time = time.Unix(t/1000, (t%1000)*1000000)
	return p
}

func GetTrack(rows *sql.Rows) *Track {
	t := &Track{}
	var entry, exit, entryTimestamp, exitTimestamp int64
	if err := rows.Scan(&t.Id, &t.Name, &t.Lat, &t.Lon, &t.Elev, &entry, &exit, &t.EntryHeading, &t.ExitHeading, &t.EntrySpeed, &t.ExitSpeed, &entryTimestamp, &exitTimestamp); err != nil {
		panic(err)
	}
	t.Entry = time.Unix(entry/1000, (entry%1000)*1000000)
	t.Exit = time.Unix(exit/1000, (exit%1000)*1000000)
	t.EntryTimestamp = time.Unix(entryTimestamp/1000, (entryTimestamp%1000)*1000000)
	t.ExitTimestamp = time.Unix(exitTimestamp/1000, (exitTimestamp%1000)*1000000)
	return t
}

func GetUnavailable(rows *sql.Rows) (*time.Time, *time.Time) {
	var start, end int64
	if err := rows.Scan(&start, &end); err != nil {
		panic(err)
	}
	tStart := time.Unix(start/1000, (start%1000)*1000000)
	tEnd := time.Unix(end/1000, (end%1000)*1000000)
	return &tStart, &tEnd
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

func Heading(p1, p2 *Pt) (float64, string) {
	dx := Dist(p1, &Pt{Lat: p1.Lat, Lon: p2.Lon})
	dy := Dist(p1, &Pt{Lat: p2.Lat, Lon: p1.Lon})
	if math.Signbit(p2.Lon - p1.Lon) {
		dx = -dx
	}
	if math.Signbit(p2.Lat - p1.Lat) {
		dy = -dy
	}
	h := 180 / math.Pi * math.Atan2(dx, dy)
	return h, HeadingName(h)
}

func HeadingName(h float64) string {
	switch {
	case h < -157.5 || h > 157.5:
		return "S"
	case h < -112.5:
		return "SW"
	case h < -67.5:
		return "W"
	case h < -22.5:
		return "NW"
	case h < 22.5:
		return "N"
	case h < 67.5:
		return "NE"
	case h < 112.5:
		return "E"
	default:
		return "SE"
	}
}

func Speed(p1, p2 *Point) float64 {
	return Dist(&p1.Pt, &p2.Pt) / p2.Time.Sub(p1.Time).Seconds()
}

func main() {
	db, err := sql.Open("sqlite3", os.Args[1])
	if err != nil {
		panic(err)
	}
	defer db.Close()

	day := ""
	startTime := time.Now().Add(-time.Hour*24*3).Unix() * 1000
	locs := GetLocations(db)
	points, err := db.Query("SELECT latitude, longitude, elevation, time, tag FROM point WHERE time > ? ORDER BY time ASC", startTime)
	if err != nil {
		panic(err)
	}
	defer points.Close()
	tracks, err := db.Query("SELECT location.id, name, latitude, longitude, elevation, entry_time, coalesce(exit_time,entry_time), entry_heading, coalesce(exit_heading,entry_heading), entry_speed, coalesce(exit_speed,entry_speed), entry_timestamp, coalesce(exit_timestamp,entry_timestamp) FROM location, track WHERE location.id = location_id AND entry_timestamp > ? ORDER BY track.id ASC", startTime)
	if err != nil {
		panic(err)
	}
	defer tracks.Close()
	unavailable, err := db.Query("SELECT unavailable_start_time, unavailable_end_time FROM availability WHERE unavailable_start_time > ? ORDER BY unavailable_start_time ASC", startTime)
	if err != nil {
		panic(err)
	}
	defer unavailable.Close()

	entered := false
	var p, lastPoint *Point
	var t *Track
	var uStart, uEnd *time.Time
	for {
		if p == nil && points.Next() {
			p = GetPoint(points)
		}
		if t == nil && tracks.Next() {
			t = GetTrack(tracks)
			entered = false
		}
		if uStart == nil && uEnd == nil && unavailable.Next() {
			uStart, uEnd = GetUnavailable(unavailable)
		}
		if p == nil && t == nil && uStart == nil && uEnd == nil {
			break
		}
		if uStart != nil {
			if p == nil || p.Time.After(*uStart) {
				fmt.Printf("%s UNAVAILABLE (%s-%s)\n", uStart.Format("15:04:05"), uStart.Format("15:04:05"), uEnd.Format("15:04:05"))
				uStart = nil
				continue
			}
		} else if uEnd != nil {
			if p == nil || p.Time.After(*uEnd) {
				fmt.Printf("%s AVAILABLE\n", uEnd.Format("15:04:05"))
				uEnd = nil
				continue
			}
		}
		printPoint := p != nil
		if t != nil {
			if !entered && (p == nil || p.Time.After(t.Entry)) {
				fmt.Printf("%s ENTER: %.23s (timestamp:%s) %.0f%s %.1fmph\n", t.Entry.Format("15:04:05"), t.Name, t.EntryTimestamp.Format("15:04:05"), t.EntryHeading, HeadingName(t.EntryHeading), t.EntrySpeed*2.23694)
				entered = true
			}
			if entered && (p == nil || p.Time.After(t.Exit)) {
				fmt.Printf("%s EXIT: %.23s (timestamp:%s) %.0f%s %.1fmph\n", t.Exit.Format("15:04:05"), t.Name, t.ExitTimestamp.Format("15:04:05"), t.ExitHeading, HeadingName(t.ExitHeading), t.ExitSpeed*2.23694)
				t = nil
				printPoint = false
			}
		}
		if printPoint {
			newDay := p.Time.Format("2006-01-02")
			if newDay != day {
				day = newDay
				fmt.Printf("%s\n", day)
			}
			loc, dist := NearestLocation(p, locs, 999)
			s := fmt.Sprintf("%s%s (%.0f)", p.Time.Format("15:04:05"), p.Tag, p.Elev*3.28084)
			if loc != nil && p.Tag[0] == '+' {
				h, dir := Heading(&loc.Pt, &p.Pt)
				fmt.Printf("%-15s %6.2f % 4.0f%2s %.13s (%.0f)", s, dist, h, dir, loc.Name, loc.Elev*3.28084)
				if lastPoint != nil {
					lastDist := Dist(&lastPoint.Pt, &loc.Pt)
					if lastDist > dist {
						t, d := ExtrapolateTime(lastPoint, p, loc)
						fmt.Printf(" ENTRY:%s %6.2f %.1fmph", t.Format("15:04:05"), d, Speed(lastPoint, p)*2.23694)
					} else {
						t, d := ExtrapolateTime(p, lastPoint, loc)
						fmt.Printf(" EXIT:%s %6.2f %.1fmph", t.Format("15:04:05"), d, Speed(lastPoint, p)*2.23694)
					}
				}
				fmt.Printf("\n")
			}
			if p.Tag[0] == '+' {
				lastPoint = p
			}
			p = nil
		}
	}
}
