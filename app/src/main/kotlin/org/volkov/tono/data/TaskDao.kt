package org.volkov.tono.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY dayKey ASC, position ASC")
    fun observeAll(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE dayKey = :dayKey AND id = :id")
    suspend fun deleteById(dayKey: String, id: String)

    @Query("SELECT MAX(position) FROM tasks WHERE dayKey = :dayKey")
    suspend fun maxPosition(dayKey: String): Int?
}
