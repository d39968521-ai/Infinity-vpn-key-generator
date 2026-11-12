package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProxyKeyDao {
    @Query("SELECT * FROM proxy_keys ORDER BY expiresAt DESC")
    fun getAllKeys(): Flow<List<ProxyKeyEntity>>

    @Query("SELECT * FROM proxy_keys WHERE id = :keyId")
    suspend fun getKeyById(keyId: String): ProxyKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(proxyKey: ProxyKeyEntity)

    @Query("DELETE FROM proxy_keys WHERE id = :keyId")
    suspend fun deleteKey(keyId: String)
    
    @Query("DELETE FROM proxy_keys")
    suspend fun clearAll()
}
