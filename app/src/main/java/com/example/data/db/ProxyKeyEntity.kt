package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_keys")
data class ProxyKeyEntity(
    @PrimaryKey val id: String, // Treat the key itself as the primary key
    val cabinetNo: String?,
    val country: String?,
    val subUrl: String?,
    val expiresAt: String?,
    val panelUser: String?
)
