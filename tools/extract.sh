#!/bin/sh
adb shell run-as com.yrek.nouncer chmod 664 databases/com.yrek.nouncer.db.DBStore
adb pull /data/data/com.yrek.nouncer/databases/com.yrek.nouncer.db.DBStore n.db
adb shell run-as com.yrek.nouncer chmod 660 databases/com.yrek.nouncer.db.DBStore
