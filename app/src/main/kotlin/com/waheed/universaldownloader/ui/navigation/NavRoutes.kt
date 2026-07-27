package com.waheed.universaldownloader.ui.navigation

import java.net.URLEncoder

object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PREVIEW = "preview/{link}"
    const val PROGRESS = "progress/{url}/{title}/{thumbnail}/{site}/{isAudio}/{format}"
    const val LIBRARY = "library"
    const val PLAYER = "player/{fileId}"
    const val SETTINGS = "settings"
    const val PIN_LOCK_SETUP = "pinlock_setup"
    const val PIN_LOCK_VERIFY = "pinlock_verify"

    fun previewRoute(link: String) = "preview/${URLEncoder.encode(link, "UTF-8")}"

    fun progressRoute(
        url: String,
        title: String,
        thumbnail: String?,
        site: String,
        isAudio: Boolean,
        format: String
    ): String {
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        val encodedThumb = URLEncoder.encode(thumbnail ?: "none", "UTF-8")
        val encodedSite = URLEncoder.encode(site, "UTF-8")
        val encodedFormat = URLEncoder.encode(format, "UTF-8")
        return "progress/$encodedUrl/$encodedTitle/$encodedThumb/$encodedSite/$isAudio/$encodedFormat"
    }

    fun playerRoute(fileId: Long) = "player/$fileId"
}
