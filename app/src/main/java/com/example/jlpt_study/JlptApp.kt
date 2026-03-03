package com.example.jlpt_study

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jlpt_study.data.local.AppDatabase
import com.example.jlpt_study.data.repository.JlptRepository
import com.example.jlpt_study.navigation.Screen
import com.example.jlpt_study.network.GptService
import com.example.jlpt_study.ui.screens.*
import com.example.jlpt_study.ui.viewmodel.*

@Composable
fun JlptApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // API 키: BuildConfig에서 가져옴 (local.properties에서 설정)
    val apiKey = BuildConfig.OPENAI_API_KEY

    // Repository 및 GptService 생성
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember {
        JlptRepository(
            sentenceDao = database.sentenceDao(),
            attemptDao = database.attemptDao(),
            wordBankDao = database.wordBankDao()
        )
    }

    val gptService = remember(apiKey) {
        if (apiKey.isNotBlank()) GptService(apiKey) else null
    }

    // ViewModels
    val trainingViewModel: TrainingViewModel = viewModel(
        factory = TrainingViewModelFactory(repository, gptService)
    )
    val statisticsViewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModelFactory(repository)
    )

    // 통계 로드
    LaunchedEffect(Unit) {
        statisticsViewModel.loadStatistics()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                val statisticsState by statisticsViewModel.uiState.collectAsState()

                HomeScreen(
                    statisticsState = statisticsState,
                    onStartTraining = {
                        trainingViewModel.resetSession()
                        trainingViewModel.startSession(isReview = false)
                        navController.navigate(Screen.Training.route)
                    },
                    onStartReview = {
                        trainingViewModel.resetSession()
                        trainingViewModel.startSession(isReview = true)
                        navController.navigate(Screen.Review.route)
                    },
                    onNavigateToMy = {
                        statisticsViewModel.loadStatistics()
                        navController.navigate(Screen.My.route)
                    }
                )
            }

            composable(Screen.Training.route) {
                val uiState by trainingViewModel.uiState.collectAsState()

                when {
                    uiState.isSessionComplete -> {
                        SessionCompleteScreen(
                            totalQuestions = uiState.sentences.size,
                            correctAnswers = uiState.correctCount,
                            onGoHome = {
                                trainingViewModel.resetSession()
                                statisticsViewModel.loadStatistics()
                                navController.popBackStack(Screen.Home.route, inclusive = false)
                            },
                            onStartReview = {
                                trainingViewModel.resetSession()
                                trainingViewModel.startSession(isReview = true)
                                navController.navigate(Screen.Review.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            }
                        )
                    }
                    uiState.showResult && uiState.lastResult != null -> {
                        ResultScreen(
                            resultData = uiState.lastResult!!,
                            onNext = { trainingViewModel.nextSentence() }
                        )
                    }
                    else -> {
                        TrainingScreen(
                            uiState = uiState,
                            onUserInputChange = { trainingViewModel.updateUserInput(it) },
                            onToggleUnknownWord = { trainingViewModel.toggleUnknownWord(it) },
                            onSubmit = { trainingViewModel.submitAnswer() },
                            onBack = {
                                trainingViewModel.resetSession()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }

            composable(Screen.Review.route) {
                val uiState by trainingViewModel.uiState.collectAsState()

                when {
                    uiState.isSessionComplete -> {
                        SessionCompleteScreen(
                            totalQuestions = uiState.sentences.size,
                            correctAnswers = uiState.correctCount,
                            onGoHome = {
                                trainingViewModel.resetSession()
                                statisticsViewModel.loadStatistics()
                                navController.popBackStack(Screen.Home.route, inclusive = false)
                            },
                            onStartReview = {
                                trainingViewModel.resetSession()
                                trainingViewModel.startSession(isReview = true)
                            }
                        )
                    }
                    uiState.showResult && uiState.lastResult != null -> {
                        ResultScreen(
                            resultData = uiState.lastResult!!,
                            onNext = { trainingViewModel.nextSentence() }
                        )
                    }
                    else -> {
                        TrainingScreen(
                            uiState = uiState,
                            onUserInputChange = { trainingViewModel.updateUserInput(it) },
                            onToggleUnknownWord = { trainingViewModel.toggleUnknownWord(it) },
                            onSubmit = { trainingViewModel.submitAnswer() },
                            onBack = {
                                trainingViewModel.resetSession()
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }

            composable(Screen.My.route) {
                val uiState by statisticsViewModel.uiState.collectAsState()

                MyScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onNavigateToWordBank = {
                        statisticsViewModel.loadWordBank()
                        navController.navigate(Screen.WordBank.route)
                    }
                )
            }

            composable(Screen.WordBank.route) {
                val wordBankState by statisticsViewModel.wordBankState.collectAsState()

                WordBankScreen(
                    words = wordBankState.words,
                    isLoading = wordBankState.isLoading,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
