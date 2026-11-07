package com.v2rayops.client.core.error

sealed class AppError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class Network(
        override val message: String = "Network connection failed. Please check your internet connectivity.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Server(
        val statusCode: Int,
        override val message: String = "Server returned error code $statusCode",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Authentication(
        override val message: String = "Authentication failed. Invalid customer code or expired session.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class SubscriptionExpired(
        override val message: String = "Your subscription or trial has expired. Please top up your wallet or renew your plan.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Vpn(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Storage(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Validation(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
