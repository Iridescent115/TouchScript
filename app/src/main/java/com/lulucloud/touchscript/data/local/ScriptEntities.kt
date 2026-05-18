package com.lulucloud.touchscript.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Database
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val source: String,
    val updatedAt: Long
)

@Serializable
@Entity(tableName = "script_templates")
data class ScriptTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val description: String,
    val source: String
)

@Serializable
@Entity(tableName = "run_records")
data class RunRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scriptName: String,
    val status: String,
    val summary: String,
    val startedAt: Long,
    val endedAt: Long
)

@Dao
interface ScriptDao {
    @Query("SELECT * FROM scripts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ScriptEntity>>

    @Query("SELECT COUNT(*) FROM scripts")
    suspend fun count(): Int

    @Query("SELECT * FROM scripts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScriptEntity): Long

    @Update
    suspend fun update(entity: ScriptEntity)
}

@Dao
interface ScriptTemplateDao {
    @Query("SELECT * FROM script_templates ORDER BY id ASC")
    fun observeAll(): Flow<List<ScriptTemplateEntity>>

    @Query("SELECT COUNT(*) FROM script_templates")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<ScriptTemplateEntity>)
}

@Dao
interface RunRecordDao {
    @Query("SELECT * FROM run_records ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<RunRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RunRecordEntity): Long
}

@Database(
    entities = [ScriptEntity::class, ScriptTemplateEntity::class, RunRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TouchWorkshopDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao
    abstract fun scriptTemplateDao(): ScriptTemplateDao
    abstract fun runRecordDao(): RunRecordDao
}
