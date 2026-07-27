package com.waheed.universaldownloader.domain.usecase

import javax.inject.Inject

sealed class UrlValidationResult {
    object Valid : UrlValidationResult()
    data class Invalid(val reason: String) : UrlValidationResult()
}

/** Basic sanity checks on a pasted URL before we ever hit the network with it. */
class ValidateUrlUseCase @Inject constructor() {
    operator fun invoke(rawUrl: String): UrlValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return UrlValidationResult.Invalid("Link is empty")
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return UrlValidationResult.Invalid("Link must start with http:// or https://")
        }
        if (!trimmed.contains(".")) {
            return UrlValidationResult.Invalid("Link doesn't look valid")
        }
        return UrlValidationResult.Valid
    }
}
