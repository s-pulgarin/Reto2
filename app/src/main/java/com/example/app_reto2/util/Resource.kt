package com.example.app_reto2.util

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : Resource<Nothing>()
}