package com.gorilla.gallery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MediaEntity::class,
        FavoriteEntity::class,
        TrashEntity::class,
        SecureItemEntity::class,
        ImageEmbeddingEntry::class,
    ],
    version = 7,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun trashDao(): TrashDao
    abstract fun secureDao(): SecureDao
    abstract fun imageEmbeddingDao(): ImageEmbeddingDao
    
    fun imageLabelDao(): ImageLabelDao = ImageLabelDao(this)
    fun faceIndexDao(): FaceIndexDao = FaceIndexDao(this)
    fun objectIndexDao(): ObjectIndexDao = ObjectIndexDao(this)
    fun textIndexDao(): TextIndexDao = TextIndexDao(this)
    fun faceEmbeddingDao(): FaceEmbeddingDao = FaceEmbeddingDao(this)

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        private val IMAGE_LABEL_CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                runCatching { db.execSQL(ImageLabelDao.CREATE_FTS_SQL) }
                runCatching { db.execSQL(FaceIndexDao.CREATE_TABLE_SQL) }
                runCatching { db.execSQL(FaceEmbeddingDao.CREATE_TABLE_SQL) }
                runCatching { db.execSQL(ObjectIndexDao.CREATE_TABLE_SQL) }
                runCatching { db.execSQL(TextIndexDao.CREATE_TABLE_SQL) }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                runCatching { db.execSQL(ImageLabelDao.CREATE_FTS_SQL) }
                runCatching { db.execSQL(FaceIndexDao.CREATE_TABLE_SQL) }
                runCatching { db.execSQL(FaceEmbeddingDao.CREATE_TABLE_SQL) }
                runCatching { db.execSQL(ObjectIndexDao.CREATE_TABLE_SQL) }
                runCatching { db.execSQL(TextIndexDao.CREATE_TABLE_SQL) }
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media ADD COLUMN absolutePath TEXT NOT NULL DEFAULT ''")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gorilla_gallery.db",
                )
                    .addCallback(IMAGE_LABEL_CALLBACK)
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
