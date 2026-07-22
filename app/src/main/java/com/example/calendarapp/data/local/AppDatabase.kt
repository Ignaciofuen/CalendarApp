package com.example.calendarapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calendarapp.data.model.CalendarEvent

// Definimos la base de datos, las tablas (entities) y la versión
@Database(entities = [CalendarEvent::class], version = 4, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración 1 → 2: añade colorHex para colores por evento
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN colorHex TEXT NOT NULL DEFAULT ''")
            }
        }

        // Migración 2 → 3: añade endTime (hora de fin) y recurrence (tipo de recurrencia)
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN endTime TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE events ADD COLUMN recurrence TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        // Migración 3 → 4: isAllDay, location, url, photoUri, notificationSoundUri
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE events ADD COLUMN isAllDay INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE events ADD COLUMN location TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE events ADD COLUMN url TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE events ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE events ADD COLUMN notificationSoundUri TEXT NOT NULL DEFAULT ''")
            }
        }

        /** Cierra y libera la instancia singleton (necesario antes de copiar el archivo DB). */
        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "calendar_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
