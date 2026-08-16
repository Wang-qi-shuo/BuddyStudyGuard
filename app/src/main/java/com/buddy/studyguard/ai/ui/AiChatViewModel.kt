package com.buddy.studyguard.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.ai.data.repository.AiChatRepository
import com.buddy.studyguard.ai.data.repository.AiResult
import com.buddy.studyguard.common.data.db.entity.AiMessageEntity
import com.buddy.studyguard.common.data.db.entity.AiRole
import com.buddy.studyguard.common.data.prefs.ApiKeyPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI 对话页 ViewModel。
 *
 * 在 init 时生成 sessionId 并订阅该会话的消息流；UI 通过 [uiState] 获取渲染所需全部状态。
 */
@HiltViewModel
class AiChatViewModel @Inject constructor(
    private val repo: AiChatRepository,
    private val apiKeyPrefs: ApiKeyPrefs
) : ViewModel() {

    /** AI 对话页 UI 状态。 */
    data class AiChatUiState(
        val messages: List<AiMessageEntity> = emptyList(),
        val loading: Boolean = false,
        val offline: Boolean = false,
        val error: String? = null,
        val sessionId: String = "",
        val apiKeySaved: Boolean = false
    )

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(apiKeySaved = apiKeyPrefs.hasApiKey()) }
        viewModelScope.launch {
            val sid = repo.newSessionId()
            _uiState.update { it.copy(sessionId = sid) }
            repo.observeMessages(sid).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    /**
     * 保存用户输入的 API Key 到 SharedPreferences，并更新状态。
     * 保存后下次进入不再弹窗。
     */
    fun saveApiKey(key: String) {
        apiKeyPrefs.saveApiKey(key.trim())
        _uiState.update { it.copy(apiKeySaved = true) }
    }

    /**
     * 发送一条用户消息。空文本且无图片时忽略；sessionId 尚未就绪时忽略。
     */
    fun send(text: String, imageUri: String? = null) {
        if (text.isBlank() && imageUri == null) return
        val sid = _uiState.value.sessionId
        if (sid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            applyResult(repo.sendMessage(sid, text, imageUri))
        }
    }

    /**
     * 重试上一条 USER 消息（用于 [AiResult.Error] 之后）。
     */
    fun retryLast() {
        val sid = _uiState.value.sessionId
        if (sid.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            applyResult(repo.retryLast(sid))
        }
    }

    /**
     * 清空当前会话的全部消息。
     */
    fun clearChat() {
        val sid = _uiState.value.sessionId
        if (sid.isBlank()) return
        viewModelScope.launch { repo.clearSession(sid) }
    }

    /** 将 [AiResult] 映射为 UI 状态更新。 */
    private fun applyResult(r: AiResult) {
        when (r) {
            is AiResult.Online -> _uiState.update {
                it.copy(loading = false, offline = false, error = null)
            }
            is AiResult.Offline -> _uiState.update {
                it.copy(loading = false, offline = true, error = null)
            }
            is AiResult.Error -> _uiState.update {
                it.copy(loading = false, error = r.message)
            }
        }
    }
}
