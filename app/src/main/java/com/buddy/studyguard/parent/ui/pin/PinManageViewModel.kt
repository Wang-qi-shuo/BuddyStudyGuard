package com.buddy.studyguard.parent.ui.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.ParentPinDao
import com.buddy.studyguard.common.data.db.entity.ParentPinEntity
import com.buddy.studyguard.common.util.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinManageViewModel @Inject constructor(
    private val parentPinDao: ParentPinDao
) : ViewModel() {

    /** 修改口令：验证旧口令后写入新 salt+hash。[onResult] true 成功。 */
    fun changePin(oldPin: String, newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val entity = parentPinDao.get()
            if (entity == null || !PinHasher.verify(oldPin, entity.pinHash, entity.salt)) {
                onResult(false)
                return@launch
            }
            val salt = PinHasher.newSalt()
            val hash = PinHasher.hash(newPin, salt)
            parentPinDao.upsert(
                ParentPinEntity(pinHash = hash, salt = salt)
            )
            onResult(true)
        }
    }
}
