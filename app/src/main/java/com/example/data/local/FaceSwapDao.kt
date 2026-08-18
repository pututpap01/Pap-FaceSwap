package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FaceSwapDao {

    @Query("SELECT * FROM face_swap_projects WHERE isEncryptedVault = 0 ORDER BY timestamp DESC")
    fun getAllPublicProjects(): Flow<List<FaceSwapEntity>>

    @Query("SELECT * FROM face_swap_projects WHERE isEncryptedVault = 1 ORDER BY timestamp DESC")
    fun getAllVaultProjects(): Flow<List<FaceSwapEntity>>

    @Query("SELECT * FROM face_swap_projects WHERE isFavorite = 1 AND isEncryptedVault = 0 ORDER BY timestamp DESC")
    fun getFavoriteProjects(): Flow<List<FaceSwapEntity>>

    @Query("SELECT * FROM face_swap_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): FaceSwapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: FaceSwapEntity): Long

    @Update
    suspend fun updateProject(project: FaceSwapEntity)

    @Query("DELETE FROM face_swap_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    @Query("UPDATE face_swap_projects SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE face_swap_projects SET isEncryptedVault = :isVault WHERE id = :id")
    suspend fun updateVaultStatus(id: Long, isVault: Boolean)

    @Query("DELETE FROM face_swap_projects WHERE isEncryptedVault = 1")
    suspend fun clearVault()
}
