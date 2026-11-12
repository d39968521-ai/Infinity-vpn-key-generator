package com.example.data

import com.example.data.api.StrausAccount
import com.example.data.db.ProxyKeyDao
import com.example.data.db.ProxyKeyEntity
import kotlinx.coroutines.flow.Flow

class ProxyRepository(private val dao: ProxyKeyDao) {

    val allKeys: Flow<List<ProxyKeyEntity>> = dao.getAllKeys()

    suspend fun generateNewKey(baseUrl: String? = null): Result<ProxyKeyEntity> {
        val account = if (baseUrl != null) StrausAccount(baseUrl = baseUrl) else StrausAccount()
        val success = account.fullSetup()

        return if (success && account.key != null) {
            val entity = ProxyKeyEntity(
                id = account.key!!,
                cabinetNo = account.cabinetNo,
                country = account.country,
                subUrl = account.subUrl,
                expiresAt = account.expiresAt,
                panelUser = account.panelUser
            )
            dao.insertKey(entity)
            Result.success(entity)
        } else {
            Result.failure(Exception(account.error ?: "Generation failed"))
        }
    }

    suspend fun addManualKey(key: String, baseUrl: String? = null): Result<ProxyKeyEntity> {
        val account = if (baseUrl != null) StrausAccount(baseUrl = baseUrl) else StrausAccount()
        val loginSuccess = account.login(key)
        
        if (loginSuccess) {
           val trialSuccess = account.startTrial(key)
           if(trialSuccess) {
                val entity = ProxyKeyEntity(
                    id = key,
                    cabinetNo = account.cabinetNo,
                    country = account.country,
                    subUrl = account.subUrl,
                    expiresAt = account.expiresAt,
                    panelUser = account.panelUser
                )
                dao.insertKey(entity)
                return Result.success(entity)
           } else {
              return Result.failure(Exception(account.error ?: "Failed to start trial for key"))
           }
        }
        return Result.failure(Exception(account.error ?: "Invalid key or login failed"))
    }
    
    suspend fun fetchConfig(url: String, format: String): String? {
       val account = StrausAccount()
       return account.fetchConfig(url, format)
    }

    suspend fun refreshKey(keyId: String, baseUrl: String? = null): Result<ProxyKeyEntity> {
        val account = if (baseUrl != null) StrausAccount(baseUrl = baseUrl) else StrausAccount()
        val loginSuccess = account.login(keyId)
        if (loginSuccess) {
            val trialSuccess = account.startTrial(keyId)
            if (trialSuccess) {
                val entity = ProxyKeyEntity(
                    id = keyId,
                    cabinetNo = account.cabinetNo,
                    country = account.country,
                    subUrl = account.subUrl,
                    expiresAt = account.expiresAt,
                    panelUser = account.panelUser
                )
                dao.insertKey(entity)
                return Result.success(entity)
            } else {
                return Result.failure(Exception(account.error ?: "Подписка не активна или истекла"))
            }
        }
        return Result.failure(Exception(account.error ?: "Ошибка подключения к серверу"))
    }

    suspend fun deleteKey(keyId: String) {
        dao.deleteKey(keyId)
    }
}
