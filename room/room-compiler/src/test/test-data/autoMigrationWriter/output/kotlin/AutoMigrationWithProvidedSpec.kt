package foo.bar

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import javax.`annotation`.processing.Generated
import kotlin.Suppress
import kotlin.Unit

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION"])
internal class MyDatabase_AutoMigration_1_2_Impl : Migration {
    private val callback: AutoMigrationSpec

    public constructor(callback: AutoMigrationSpec) : super(1, 2) {
        this.callback = callback
    }

    public override fun migrate(database: SupportSQLiteDatabase): Unit {
        database.execSQL("ALTER TABLE `Song` ADD COLUMN `artistId` INTEGER DEFAULT NULL")
        callback.onPostMigrate(database)
    }
}