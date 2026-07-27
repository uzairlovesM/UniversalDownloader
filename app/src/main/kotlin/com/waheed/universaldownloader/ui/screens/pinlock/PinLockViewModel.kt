package com.waheed.universaldownloader.ui.screens.pinlock

import androidx.lifecycle.ViewModel
import com.waheed.universaldownloader.data.settings.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

enum class PinLockMode { SETUP, VERIFY }

data class PinLockUiState(
    val mode: PinLockMode = PinLockMode.VERIFY,
    val enteredDigits: String = "",
    val firstEntryForSetup: String? = null, // holds the first PIN entry while confirming
    val errorMessage: String? = null,
    val isUnlocked: Boolean = false
)

@HiltViewModel
class PinLockViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PinLockUiState(mode = if (pinManager.isPinSet()) PinLockMode.VERIFY else PinLockMode.SETUP)
    )
    val uiState: StateFlow<PinLockUiState> = _uiState.asStateFlow()

    fun onDigitPressed(digit: Char) {
        val current = _uiState.value
        if (current.enteredDigits.length >= 4) return

        val updatedDigits = current.enteredDigits + digit
        _uiState.value = current.copy(enteredDigits = updatedDigits, errorMessage = null)

        if (updatedDigits.length == 4) {
            handleFullEntry(updatedDigits)
        }
    }

    fun onBackspace() {
        val current = _uiState.value
        if (current.enteredDigits.isEmpty()) return
        _uiState.value = current.copy(enteredDigits = current.enteredDigits.dropLast(1))
    }

    private fun handleFullEntry(pin: String) {
        val current = _uiState.value

        when (current.mode) {
            PinLockMode.VERIFY -> {
                if (pinManager.verifyPin(pin)) {
                    _uiState.value = current.copy(isUnlocked = true)
                } else {
                    _uiState.value = current.copy(
                        enteredDigits = "",
                        errorMessage = "Incorrect PIN, try again"
                    )
                }
            }
            PinLockMode.SETUP -> {
                if (current.firstEntryForSetup == null) {
                    // First entry — ask for confirmation
                    _uiState.value = current.copy(
                        enteredDigits = "",
                        firstEntryForSetup = pin
                    )
                } else {
                    // Second entry — must match the first
                    if (pin == current.firstEntryForSetup) {
                        pinManager.setPin(pin)
                        _uiState.value = current.copy(isUnlocked = true)
                    } else {
                        _uiState.value = current.copy(
                            enteredDigits = "",
                            firstEntryForSetup = null,
                            errorMessage = "PINs didn't match, start over"
                        )
                    }
                }
            }
        }
    }
}
