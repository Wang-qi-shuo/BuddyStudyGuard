package com.buddy.studyguard.study.ui.mode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.ParentPinDao
import com.buddy.studyguard.common.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 模式选择页 ViewModel。
 *
 * 管理 PIN 验证逻辑，复用已有的 [ParentPinDao] + [PinHasher] 基础设施。
 * 默认 PIN 由数据库 SEED_CALLBACK 在首次创建时写入（任务要求默认 "123456"）。
 */
@HiltViewModel
class ModeSelectionViewModel @Inject constructor(
    private val parentPinDao: ParentPinDao
) : ViewModel() {

    /** PIN 输入对话框是否可见。 */
    private val _showPinDialog = MutableStateFlow(false)
    val showPinDialog: StateFlow<Boolean> = _showPinDialog.asStateFlow()

    /** 验证错误提示是否可见。 */
    private val _pinError = MutableStateFlow(false)
    val pinError: StateFlow<Boolean> = _pinError.asStateFlow()

    /** 是否正在验证（防止重复提交）。 */
    private val _verifying = MutableStateFlow(false)
    val verifying: StateFlow<Boolean> = _verifying.asStateFlow()

    fun openPinDialog() {
        _showPinDialog.value = true
        _pinError.value = false
    }

    fun dismissPinDialog() {
        _showPinDialog.value = false
        _pinError.value = false
    }

    /**
     * 验证 PIN，通过 [onResult] 回调结果。
     * 与 [com.buddy.studyguard.parent.ui.entry.ParentEntryViewModel.verify] 逻辑一致。
     */
    fun verify(pin: String, onResult: (Boolean) -> Unit) {
        if (_verifying.value) return
        _verifying.value = true
        _pinError.value = false
        viewModelScope.launch {
            try {
                val entity = parentPinDao.get()
                val ok = entity != null && PinHasher.verify(pin, entity.pinHash, entity.salt)
                _pinError.value = !ok
                onResult(ok)
            } finally {
                _verifying.value = false
            }
        }
    }
}
