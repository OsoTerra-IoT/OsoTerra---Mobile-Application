package com.osoterra.mobile.core.util

sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    /** [httpCode] es el código HTTP cuando el Backend respondió con error (409: regla de negocio). */
    data class Error(val message: String, val cause: Throwable? = null, val httpCode: Int? = null) : DataResult<Nothing>
}
