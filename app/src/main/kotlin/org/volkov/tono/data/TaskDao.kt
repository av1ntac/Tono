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

    @Query("UPDATE tasks SET text = :text WHERE id = :id")
    suspend fun updateText(id: String, text: String)

    @Query("SELECT MAX(position) FROM tasks WHERE dayKey = :dayKey")
    suspend fun maxPosition(dayKey: String): Int?

    @Query("SELECT * FROM tasks WHERE bucket = '$BUCKET_WEEK' AND dayKey < :windowStart")
    suspend fun getTasksBefore(windowStart: String): List<Task>

    /**
     * Month rows that have fallen behind the current month. `later` never matches: it sorts
     * after every `yyyy-MM` key, and is by definition never stale.
     */
    @Query("SELECT * FROM tasks WHERE bucket = '$BUCKET_MONTH' AND dayKey < :currentMonthKey")
    suspend fun getMonthTasksBefore(currentMonthKey: String): List<Task>

    @Query("SELECT * FROM tasks WHERE dayKey = :dayKey ORDER BY position ASC")
    suspend fun getTasksForDay(dayKey: String): List<Task>
}
