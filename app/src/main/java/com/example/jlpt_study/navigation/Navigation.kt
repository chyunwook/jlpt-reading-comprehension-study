package com.example.jlpt_study.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Training : Screen("training")
    data object Review : Screen("review")
    data object My : Screen("my")
}
