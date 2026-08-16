package com.buddy.studyguard.study.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.StudySessionDao
import com.buddy.studyguard.common.data.db.entity.FocusMode
import com.buddy.studyguard.common.data.db.entity.StudySessionEntity
import com.buddy.studyguard.common.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusTimerViewModel @Inject constructor(
    private val studySessionDao: StudySessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var sessionStartAt: Long = 0L

    fun setMode(mode: String) {
        stop()
        _uiState.update {
            it.copy(
                mode = mode,
                remainingMs = if (mode == FocusMode.POMODORO) WORK_MS else 0L,
                elapsedMs = 0L
            )
        }
    }

    fun setSubject(subject: String) = _uiState.update { it.copy(currentSubject = subject) }

    fun start() {
        if (_uiState.value.running) return
        sessionStartAt = System.currentTimeMillis()
        _uiState.update { it.copy(running = true) }
        tickJob = viewModelScope.launch {
            while (_uiState.value.running) {
                delay(1000)
                val s = _uiState.value
                if (s.mode == FocusMode.POMODORO) {
                    val newRem = s.remainingMs - 1000
                    if (newRem <= 0) {
                        _uiState.update { it.copy(running = false, remainingMs = 0L) }
                        saveSession()
                        break
                    }
                    _uiState.update { it.copy(remainingMs = newRem) }
                } else {
                    _uiState.update { it.copy(elapsedMs = s.elapsedMs + 1000) }
                }
            }
        }
    }

    fun pause() {
        _uiState.update { it.copy(running = false) }
        tickJob?.cancel()
    }

    fun reset() {
        stop()
        _uiState.update {
            it.copy(
                remainingMs = if (it.mode == FocusMode.POMODORO) WORK_MS else 0L,
                elapsedMs = 0L
            )
        }
    }

    fun finishAndSave() {
        stop()
        saveSession()
        reset()
    }

    private fun stop() {
        _uiState.update { it.copy(running = false) }
        tickJob?.cancel()
    }

    private fun saveSession() {
        val s = _uiState.value
        val duration = if (s.mode == FocusMode.POMODORO) WORK_MS - s.remainingMs else s.elapsedMs
        if (duration < 1000) return
        viewModelScope.launch {
            studySessionDao.insert(
                StudySessionEntity(
                    subject = s.currentSubject.ifBlank { "学习" },
                    mode = s.mode,
                    startAt = sessionStartAt,
                    durationMs = duration
                )
            )
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
    }

    companion object {
        val WORK_MS: Long = Constants.POMODORO_WORK_MINUTES * 60_000L
    }
}

data class FocusUiState(
    val mode: String = FocusMode.POMODORO,
    val running: Boolean = false,
    val remainingMs: Long = WORK_MS,
    val elapsedMs: Long = 0L,
    val currentSubject: String = ""
) {
    companion object {
        val WORK_MS: Long = Constants.POMODORO_WORK_MINUTES * 60_000L
    }
}
