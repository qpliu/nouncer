package com.yrek.nouncer;

import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.util.JsonReader;

public class Announcements {
    private final ArrayList<Announcement> announcements = new ArrayList<Announcement>();
    private final Announcement defaultInitialExit;
    private final Announcement defaultSubsequentEntry;

    public Announcements(Context context) {
        Announcement defaultInitialExit = null;
        Announcement defaultSubsequentEntry = null;

        try {
            JsonReader in = new JsonReader(new InputStreamReader(context.getResources().openRawResource(R.raw.announcements), "UTF-8"));
            in.beginArray();
            while (in.hasNext()) {
                String name = null;
                String announcement = null;
                boolean custom = false;
                String defaultUse = null;
                in.beginObject();
                while (in.hasNext()) {
                    String key = in.nextName();
                    if ("name".equals(key)) {
                        name = in.nextString();
                    } else if ("announcement".equals(key)) {
                        announcement = in.nextString();
                    } else if ("custom".equals(key)) {
                        custom = in.nextBoolean();
                    } else if ("default".equals(key)) {
                        defaultUse = in.nextString();
                    } else {
                        in.skipValue();
                    }
                }
                in.endObject();
                Announcement a = new Announcement(name, announcement, custom);
                announcements.add(a);
                if ("initial exit".equals(defaultUse)) {
                    defaultInitialExit = a;
                } else if ("subsequent entry".equals(defaultUse)) {
                    defaultSubsequentEntry = a;
                }
            }
            in.endArray();
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.defaultInitialExit = defaultInitialExit;
        this.defaultSubsequentEntry = defaultSubsequentEntry;
    }

    public class Announcement {
        public final String name;
        public final String announcement;
        public final boolean custom;

        Announcement(String name, String announcement, boolean custom) {
            this.name = name;
            this.announcement = announcement;
            this.custom = custom;
        }
    }

    public List<Announcement> getAnnouncements() {
        return new ArrayList<Announcement>(announcements);
    }

    public Announcement getEntryDefault(int routeIndex, int routeCount) {
        if (routeIndex > 0) {
            return defaultSubsequentEntry;
        }
        return null;
    }

    public Announcement getExitDefault(int routeIndex, int routeCount) {
        if (routeIndex == 0) {
            return defaultInitialExit;
        }
        return null;
    }

    public Announcement getByName(String name) {
        for (Announcement a : announcements) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    public Announcement getByAnnouncement(String announcement) {
        for (Announcement a : announcements) {
            if (a.announcement != null && a.announcement.equals(announcement)) {
                return a;
            }
        }
        for (Announcement a : announcements) {
            if (a.custom) {
                return a;
            }
        }
        return null;
    }

    public int getIndexByAnnouncement(String announcement) {
        int customIndex = 0;
        for (int i = 0; i < announcements.size(); i++) {
            Announcement a = announcements.get(i);
            if (a.announcement == null) {
                if (a.custom) {
                    customIndex = i;
                } else if (announcement == null) {
                    return i;
                }
            } else if (a.announcement.equals(announcement)) {
                return i;
            }
        }
        return customIndex;
    }
}
