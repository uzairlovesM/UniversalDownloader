package com.waheed.universaldownloader.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.waheed.universaldownloader.ui.screens.splash.SplashScreen
import com.waheed.universaldownloader.ui.screens.onboarding.OnboardingScreen
import com.waheed.universaldownloader.ui.screens.home.HomeScreen
import com.waheed.universaldownloader.ui.screens.preview.PreviewScreen
import com.waheed.universaldownloader.ui.screens.progress.ProgressScreen
import com.waheed.universaldownloader.ui.screens.player.PlayerScreen
import java.net.URLDecoder

@Composable
fun UDNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH,
        modifier = Modifier,
        enterTransition = {
            androidx.compose.animation.slideInHorizontally(
                animationSpec = tween(350),
                initialOffsetX = { it / 3 }
            ) + androidx.compose.animation.fadeIn(tween(350))
        },
        exitTransition = {
            androidx.compose.animation.fadeOut(tween(200))
        },
        popEnterTransition = {
            androidx.compose.animation.fadeIn(tween(300))
        },
        popExitTransition = {
            androidx.compose.animation.slideOutHorizontally(
                animationSpec = tween(300),
                targetOffsetX = { it / 3 }
            ) + androidx.compose.animation.fadeOut(tween(300))
        }
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(onFinished = {
                navController.navigate(NavRoutes.ONBOARDING) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                navController.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(NavRoutes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(
            route = NavRoutes.PREVIEW,
            arguments = listOf(navArgument("link") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedLink = backStackEntry.arguments?.getString("link").orEmpty()
            val link = URLDecoder.decode(encodedLink, "UTF-8")
            PreviewScreen(url = link, navController = navController)
        }
        composable(
            route = NavRoutes.PROGRESS,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("thumbnail") { type = NavType.StringType },
                navArgument("site") { type = NavType.StringType },
                navArgument("isAudio") { type = NavType.BoolType },
                navArgument("format") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val url = URLDecoder.decode(args.getString("url").orEmpty(), "UTF-8")
            val title = URLDecoder.decode(args.getString("title").orEmpty(), "UTF-8")
            val thumbRaw = URLDecoder.decode(args.getString("thumbnail").orEmpty(), "UTF-8")
            val thumbnail = if (thumbRaw == "none") null else thumbRaw
            val site = URLDecoder.decode(args.getString("site").orEmpty(), "UTF-8")
            val isAudio = args.getBoolean("isAudio")
            val format = URLDecoder.decode(args.getString("format").orEmpty(), "UTF-8")

            ProgressScreen(
                url = url,
                title = title,
                thumbnailUrl = thumbnail,
                siteName = site,
                isAudioOnly = isAudio,
                formatSelector = format,
                navController = navController
            )
        }
        composable(
            route = NavRoutes.PLAYER,
            arguments = listOf(navArgument("fileId") { type = NavType.LongType })
        ) {
            PlayerScreen(navController = navController)
        }
    }
}
