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
    }
}
