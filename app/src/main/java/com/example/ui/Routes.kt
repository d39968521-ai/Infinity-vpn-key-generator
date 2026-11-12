package com.example.ui

object Routes {
    const val HOME = "home"
    const val DETAIL = "detail/{keyId}"
    
    fun detailRoute(keyId: String) = "detail/$keyId"
}
