package com.example.data

import kotlinx.serialization.Serializable

@Serializable data class Error(val msg: String)

@Serializable data class Success(val transactionId: String)
