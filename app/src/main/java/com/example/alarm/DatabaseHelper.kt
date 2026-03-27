package com.example.alarm

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "UserDatabase.db"
        private const val DATABASE_VERSION = 4
        
        // Bảng Users
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_AVATAR = "avatar_uri"

        // Bảng Alarms
        private const val TABLE_ALARMS = "alarms"
        private const val COLUMN_ALARM_ID = "id"
        private const val COLUMN_ALARM_OWNER = "username"
        private const val COLUMN_HOUR = "hour"
        private const val COLUMN_MINUTE = "minute"
        private const val COLUMN_DAYS = "days_of_week"
        private const val COLUMN_SOUND = "sound_uri"
        private const val COLUMN_ENABLED = "is_enabled"

        // Bảng World Clock
        private const val TABLE_WORLD_CLOCK = "world_clock"
        private const val COLUMN_WC_ID = "id"
        private const val COLUMN_WC_OWNER = "username"
        private const val COLUMN_TIMEZONE_ID = "timezone_id"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUsersTable = ("CREATE TABLE " + TABLE_USERS + " ("
                + COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USERNAME + " TEXT UNIQUE, "
                + COLUMN_PASSWORD + " TEXT, "
                + COLUMN_AVATAR + " TEXT)")
        
        val createAlarmsTable = ("CREATE TABLE " + TABLE_ALARMS + " ("
                + COLUMN_ALARM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ALARM_OWNER + " TEXT, "
                + COLUMN_HOUR + " INTEGER, "
                + COLUMN_MINUTE + " INTEGER, "
                + COLUMN_DAYS + " TEXT, "
                + COLUMN_SOUND + " TEXT, "
                + COLUMN_ENABLED + " INTEGER, "
                + "FOREIGN KEY(" + COLUMN_ALARM_OWNER + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USERNAME + "))")

        val createWorldClockTable = ("CREATE TABLE " + TABLE_WORLD_CLOCK + " ("
                + COLUMN_WC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_WC_OWNER + " TEXT, "
                + COLUMN_TIMEZONE_ID + " TEXT, "
                + "FOREIGN KEY(" + COLUMN_WC_OWNER + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USERNAME + "))")

        db?.execSQL(createUsersTable)
        db?.execSQL(createAlarmsTable)
        db?.execSQL(createWorldClockTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS)
            db?.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS)
            onCreate(db)
        } else {
            if (oldVersion < 3) {
                val createWorldClockTable = ("CREATE TABLE " + TABLE_WORLD_CLOCK + " ("
                        + COLUMN_WC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COLUMN_WC_OWNER + " TEXT, "
                        + COLUMN_TIMEZONE_ID + " TEXT, "
                        + "FOREIGN KEY(" + COLUMN_WC_OWNER + ") REFERENCES " + TABLE_USERS + "(" + COLUMN_USERNAME + "))")
                db?.execSQL(createWorldClockTable)
            }
            if (oldVersion < 4) {
                db?.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_AVATAR TEXT")
            }
        }
    }

    // --- USER METHODS ---
    fun addUser(username: String, password: String): Long {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COLUMN_USERNAME, username)
        contentValues.put(COLUMN_PASSWORD, password)
        val result = db.insert(TABLE_USERS, null, contentValues)
        db.close()
        return result
    }

    fun updateAvatar(username: String, uri: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_AVATAR, uri)
        db.update(TABLE_USERS, values, "$COLUMN_USERNAME = ?", arrayOf(username))
        db.close()
    }

    fun getAvatar(username: String): String? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_AVATAR), "$COLUMN_USERNAME = ?", arrayOf(username), null, null, null)
        var uri: String? = null
        if (cursor.moveToFirst()) {
            uri = cursor.getString(0)
        }
        cursor.close()
        return uri
    }

    fun updatePassword(username: String, newPassword: String): Int {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_PASSWORD, newPassword)
        val result = db.update(TABLE_USERS, values, "$COLUMN_USERNAME = ?", arrayOf(username))
        db.close()
        return result
    }

    fun checkUser(username: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun checkUserLogin(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // --- ALARM METHODS ---
    private val gson = Gson()

    fun addAlarm(username: String, alarm: AlarmItem): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_ALARM_OWNER, username)
        values.put(COLUMN_HOUR, alarm.hour)
        values.put(COLUMN_MINUTE, alarm.minute)
        values.put(COLUMN_DAYS, gson.toJson(alarm.daysOfWeek))
        values.put(COLUMN_SOUND, alarm.soundUriString)
        values.put(COLUMN_ENABLED, if (alarm.isEnabled) 1 else 0)
        
        val id = db.insert(TABLE_ALARMS, null, values)
        db.close()
        return id
    }

    fun getAlarmsByUser(username: String): MutableList<AlarmItem> {
        val list = mutableListOf<AlarmItem>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_ALARMS WHERE $COLUMN_ALARM_OWNER = ?"
        val cursor = db.rawQuery(query, arrayOf(username))

        if (cursor.moveToFirst()) {
            do {
                val alarm = AlarmItem(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ALARM_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HOUR)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINUTE)),
                    gson.fromJson(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS)), BooleanArray::class.java),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SOUND)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1
                )
                list.add(alarm)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateAlarm(alarm: AlarmItem) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_HOUR, alarm.hour)
        values.put(COLUMN_MINUTE, alarm.minute)
        values.put(COLUMN_DAYS, gson.toJson(alarm.daysOfWeek))
        values.put(COLUMN_SOUND, alarm.soundUriString)
        values.put(COLUMN_ENABLED, if (alarm.isEnabled) 1 else 0)

        db.update(TABLE_ALARMS, values, "$COLUMN_ALARM_ID = ?", arrayOf(alarm.id.toString()))
        db.close()
    }

    fun deleteAlarm(alarmId: Int) {
        val db = this.writableDatabase
        db.delete(TABLE_ALARMS, "$COLUMN_ALARM_ID = ?", arrayOf(alarmId.toString()))
        db.close()
    }

    // --- WORLD CLOCK METHODS ---
    fun addWorldClock(username: String, timezoneId: String): Long {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_WC_OWNER, username)
        values.put(COLUMN_TIMEZONE_ID, timezoneId)
        val id = db.insert(TABLE_WORLD_CLOCK, null, values)
        db.close()
        return id
    }

    fun getWorldClocksByUser(username: String): MutableList<String> {
        val list = mutableListOf<String>()
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_TIMEZONE_ID FROM $TABLE_WORLD_CLOCK WHERE $COLUMN_WC_OWNER = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        if (cursor.moveToFirst()) {
            do {
                list.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteWorldClock(username: String, timezoneId: String) {
        val db = this.writableDatabase
        db.delete(TABLE_WORLD_CLOCK, "$COLUMN_WC_OWNER = ? AND $COLUMN_TIMEZONE_ID = ?", arrayOf(username, timezoneId))
        db.close()
    }
}
