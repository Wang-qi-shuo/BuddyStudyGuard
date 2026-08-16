package com.buddy.studyguard.parent.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.ParentPinDao
import com.buddy.studyguard.common.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentEntryViewModel @Inject constructor(
    private val parentPinDao: ParentPinDao
) : ViewModel() {

    /**
     * 验证口令，结果通过 [onResult] 回调。
     * 第一个参数为验证是否通过，第二个参数为当前口令是否为默认口令。
     */
    fun verify(pin: String, onResult: (ok: Boolean, isDefault: Boolean) -> Unit) {
        viewModelScope.launch {
            val entity = parentPinDao.get()
            val ok = entity != null && PinHasher.verify(pin, entity.pinHash, entity.salt)
            val isDefault = entity != null &&
                PinHasher.verify(PinHasher.DEFAULT_PIN, entity.pinHash, entity.salt)
            onResult(ok, isDefault)
        }
    }
}
