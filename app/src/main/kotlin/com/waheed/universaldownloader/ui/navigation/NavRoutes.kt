package com.waheed.universaldownloader.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PREVIEW = "preview/{link}"
    const val PROGRESS = "progress/{downloadId}"
    const val LIBRARY = "library"
    const val PLAYER = "player/{fileId}"
    const val SETTINGS = "settings"

    fun previewRoute(link: String) = "preview/${java.net.URLEncoder.encode(link, "UTF-8")}"
    fun progressRoute(downloadId: String) = "progress/$downloadId"
    fun playerRoute(fileId: String) = "player/$fileId"
}
