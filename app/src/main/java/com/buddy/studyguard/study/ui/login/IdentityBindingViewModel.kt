package com.buddy.studyguard.study.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.CloudBaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 身份绑定页 ViewModel。
 *
 * 支持学生生成家庭码和家长通过家庭码绑定。
 */
@HiltViewModel
class IdentityBindingViewModel @Inject constructor() : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 学生端生成的家庭码（6 位数字）。 */
    private val _generatedFamilyCode = MutableStateFlow<String?>(null)
    val generatedFamilyCode: StateFlow<String?> = _generatedFamilyCode.asStateFlow()

    /** 家长端输入的家庭码。 */
    private val _parentFamilyCode = MutableStateFlow("")
    val parentFamilyCode: StateFlow<String> = _parentFamilyCode.asStateFlow()

    /** 家长端输入的称呼（昵称）。 */
    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    /** 绑定完成 → true，失败 → false。 */
    private val _bindingComplete = MutableStateFlow(false)
    val bindingComplete: StateFlow<Boolean> = _bindingComplete.asStateFlow()

    /** 当前登录账号（用户名），用于页面提示。 */
    val currentUsername: String? = CloudBaseManager.currentUsername()

    fun onParentCodeChange(value: String) {
        _parentFamilyCode.value = value.filter { it.isDigit() }.take(6)
        _errorMessage.value = null
    }

    fun onNicknameChange(value: String) {
        _nickname.value = value
        _errorMessage.value = null
    }

    /**
     * 学生端：生成随机 6 位家庭码，写入 users 集合。
     */
    fun bindAsStudent() {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val uid = CloudBaseManager.currentUserId()
                if (uid == null) {
                    _errorMessage.value = "用户未登录"
                    return@launch
                }

                val familyCode = generateFamilyCode()

                // 幂等写入 users：已存在则更新，不存在才插入
                val existing = CloudBaseManager.api.query(
                    CloudBaseManager.COLL_USERS,
                    filters = mapOf("uid" to "eq.$uid")
                )
                if (existing.isNotEmpty()) {
                    CloudBaseManager.api.update(
                        CloudBaseManager.COLL_USERS,
                        filters = mapOf("uid" to "eq.$uid"),
                        body = mapOf(
                            "identity" to "student",
                            "family_code" to familyCode
                        )
                    )
                } else {
                    CloudBaseManager.api.insert(
                        CloudBaseManager.COLL_USERS,
                        listOf(mapOf(
                            "uid" to uid,
                            "phone" to "",
                            "identity" to "student",
                            "family_code" to familyCode,
                            "created_at" to System.currentTimeMillis()
                        ))
                    )
                }

                _generatedFamilyCode.value = familyCode
            } catch (e: Exception) {
                _errorMessage.value = "绑定失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 家长端：通过家庭码查找学生，建立 family_groups 绑定。
     */
    fun bindAsParent() {
        val familyCode = _parentFamilyCode.value
        if (familyCode.length != 6) {
            _errorMessage.value = "请输入 6 位家庭绑定码"
            return
        }

        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            try {
                val uid = CloudBaseManager.currentUserId()
                if (uid == null) {
                    _errorMessage.value = "用户未登录"
                    return@launch
                }

                // 通过家庭码查找学生
                val students = CloudBaseManager.api.query(
                    CloudBaseManager.COLL_USERS,
                    filters = mapOf(
                        "family_code" to "eq.$familyCode",
                        "identity" to "eq.student"
                    )
                )

                if (students.isEmpty()) {
                    _errorMessage.value = "未找到该家庭码对应的学生，请检查"
                    return@launch
                }

                val studentDoc = students.first()
                val studentUid = studentDoc["uid"] as? String ?: ""

                // 幂等写入 users：家长身份已存在则更新，不存在才插入
                val existingUser = CloudBaseManager.api.query(
                    CloudBaseManager.COLL_USERS,
                    filters = mapOf("uid" to "eq.$uid")
                )
                if (existingUser.isNotEmpty()) {
                    CloudBaseManager.api.update(
                        CloudBaseManager.COLL_USERS,
                        filters = mapOf("uid" to "eq.$uid"),
                        body = mapOf(
                            "identity" to "parent",
                            "family_code" to familyCode,
                            "nickname" to _nickname.value
                        )
                    )
                } else {
                    CloudBaseManager.api.insert(
                        CloudBaseManager.COLL_USERS,
                        listOf(mapOf(
                            "uid" to uid,
                            "phone" to "",
                            "identity" to "parent",
                            "family_code" to familyCode,
                            "nickname" to _nickname.value,
                            "created_at" to System.currentTimeMillis()
                        ))
                    )
                }

                // 幂等写入 family_groups：已存在相同 parent_uid+family_code 则跳过
                val existingGroup = CloudBaseManager.api.query(
                    CloudBaseManager.COLL_FAMILY_GROUPS,
                    filters = mapOf(
                        "parent_uid" to "eq.$uid",
                        "family_code" to "eq.$familyCode"
                    )
                )
                if (existingGroup.isEmpty()) {
                    CloudBaseManager.api.insert(
                        CloudBaseManager.COLL_FAMILY_GROUPS,
                        listOf(mapOf(
                            "family_code" to familyCode,
                            "student_uid" to studentUid,
                            "parent_uid" to uid,
                            "created_at" to System.currentTimeMillis()
                        ))
                    )
                }

                _bindingComplete.value = true
            } catch (e: Exception) {
                _errorMessage.value = "绑定失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /** 生成 6 位随机数字家庭码。 */
    private fun generateFamilyCode(): String {
        return (100000..999999).random().toString()
    }
}
