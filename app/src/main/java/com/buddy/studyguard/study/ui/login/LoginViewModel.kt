package com.buddy.studyguard.study.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录页 ViewModel。
 *
 * 管理账号密码输入、登录验证及登录后身份检查。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** null=未完成, true=有身份→直接导航, false=无身份→身份绑定 */
    private val _loginResult = MutableStateFlow<Boolean?>(null)
    val loginResult: StateFlow<Boolean?> = _loginResult.asStateFlow()

    fun onUsernameChange(value: String) {
        _username.value = value
        _errorMessage.value = null
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        _errorMessage.value = null
    }

    /**
     * 账号 + 密码登录。
     * 登录成功后检查 users 集合中是否有该用户的身份记录。
     */
    fun login() {
        val username = _username.value.trim()
        val password = _password.value
        if (username.isEmpty()) {
            _errorMessage.value = "请输入账号"
            return
        }
        if (password.isEmpty()) {
            _errorMessage.value = "请输入密码"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val api = CloudBaseManager.api
                // 1. 账号密码登录
                val deviceId = CloudBaseManager.getDeviceId()
                val signInResp = api.signIn(deviceId, SignInRequest(username, password))
                // 2. 保存认证信息
                CloudBaseManager.setAuth(
                    signInResp.access_token,
                    signInResp.sub,
                    username,
                    signInResp.refresh_token,
                    signInResp.expires_in
                )
                // 3. 查询用户身份
                val rows = api.query(
                    CloudBaseManager.COLL_USERS,
                    filters = mapOf("uid" to "eq.${signInResp.sub}")
                )
                val hasIdentity = rows.isNotEmpty()
                // 4. 登录成功后立即同步一次限制快照，避免新限制/删除限制要等 5 分钟轮询
                RestrictionSyncWorker.runNow(context)
                _loginResult.value = hasIdentity
            } catch (e: Exception) {
                Log.w("LoginViewModel", "登录失败", e)
                _errorMessage.value = "登录失败，请检查账号或密码"
            } finally {
                _loading.value = false
            }
        }
    }

    /** 重置登录结果状态，用于重新导航。 */
    fun resetLoginResult() {
        _loginResult.value = null
    }
}
