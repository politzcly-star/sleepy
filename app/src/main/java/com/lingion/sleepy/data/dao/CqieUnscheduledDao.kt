package com.lingion.sleepy.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lingion.sleepy.data.entity.CqieUnscheduledEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CqieUnscheduledDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CqieUnscheduledEntity>): List<Long>

    @Query("DELETE FROM cqie_unscheduled_courses WHERE tableId = :tableId")
    suspend fun deleteByTableId(tableId: Long)

    @Query("SELECT * FROM cqie_unscheduled_courses WHERE tableId = :tableId ORDER BY courseName, id")
    fun observeByTable(tableId: Long): Flow<List<CqieUnscheduledEntity>>

    @Query("SELECT * FROM cqie_unscheduled_courses WHERE tableId = :tableId ORDER BY courseName, id")
    suspend fun getByTable(tableId: Long): List<CqieUnscheduledEntity>
}
