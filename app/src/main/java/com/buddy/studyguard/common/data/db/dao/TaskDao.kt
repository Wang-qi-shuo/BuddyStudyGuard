package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY completed ASC, dueAt IS NULL, dueAt ASC, createdAt DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE source = :source ORDER BY createdAt DESC")
    fun observeBySource(source: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY dueAt IS NULL, dueAt ASC")
    fun observePending(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAt: Long?)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 按云端 ID 查找任务。 */
    @Query("SELECT * FROM tasks WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): TaskEntity?

    /** 更新本地任务的云端 ID。 */
    @Query("UPDATE tasks SET cloudId = :cloudId WHERE id = :id")
    suspend fun setCloudId(id: Long, cloudId: String)

    /** 按云端 ID 删除任务（用于云端删除后的本地同步）。 */
    @Query("DELETE FROM tasks WHERE cloudId = :cloudId")
    suspend fun deleteByCloudId(cloudId: String)
}
