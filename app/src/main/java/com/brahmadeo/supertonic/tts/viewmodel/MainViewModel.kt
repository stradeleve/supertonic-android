package com.brahmadeo.supertonic.tts.viewmodel

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    // UI State
    var inputText = mutableStateOf("")
    var isInitializing = mutableStateOf(true)
    var isSynthesizing = mutableStateOf(false)

    // Settings State
    var currentLang = mutableStateOf("en")
    var selectedVoiceFile = mutableStateOf("M1.json")
    var selectedVoiceFile2 = mutableStateOf("M2.json")
    var isMixingEnabled = mutableStateOf(false)
    var mixAlpha = mutableFloatStateOf(0.5f)
    var currentSpeed = mutableFloatStateOf(1.1f)
    var currentSteps = mutableIntStateOf(5)

    // Mini Player State
    var showMiniPlayer = mutableStateOf(false)
    var miniPlayerTitle = mutableStateOf("Now Playing")
    var miniPlayerIsPlaying = mutableStateOf(false)

    // Asset Download State
    var isDownloading = mutableStateOf(false)
    var downloadProgress = mutableFloatStateOf(0f)
    var downloadStatus = mutableStateOf("Checking assets...")

    // Dialog State
    var showQueueDialog = mutableStateOf(false)
    var queueDialogText = ""

    // Data
    val voiceFiles = mutableStateMapOf<String, String>()
}