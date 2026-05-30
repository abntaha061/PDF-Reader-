package com.example.pdf

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_files")
data class SavedFileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val timestamp: Long,
    val isFavorite: Boolean
)

@Dao
interface SavedFileDao {
    @Query("SELECT * FROM saved_files ORDER BY timestamp DESC")
    fun getAllSavedFiles(): Flow<List<SavedFileEntity>>

    @Query("SELECT * FROM saved_files WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteFiles(): Flow<List<SavedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(file: SavedFileEntity)

    @Query("UPDATE saved_files SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: String, isFav: Boolean)

    @Query("DELETE FROM saved_files WHERE id = :id")
    suspend fun deleteFile(id: String)
}

@Database(entities = [SavedFileEntity::class], version = 1, exportSchema = false)
abstract class PdfDatabase : RoomDatabase() {
    abstract fun savedFileDao(): SavedFileDao

    companion object {
        @Volatile
        private var INSTANCE: PdfDatabase? = null

        fun getDatabase(context: Context): PdfDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfDatabase::class.java,
                    "pdf_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
