package com.buddy.studyguard.parent.ui.pin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.buddy.studyguard.R
import com.buddy.studyguard.common.ui.theme.NeonGreen
import com.buddy.studyguard.common.ui.theme.NeonMagenta

@Composable
fun PinManageScreen(viewModel: PinManageViewModel = hiltViewModel()) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "修改家长口令",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )

        fun digit(s: String) = s.filter { it.isDigit() }.take(8)

        OutlinedTextField(
            value = oldPin,
            onValueChange = { oldPin = digit(it); message = null },
            label = { Text("旧口令") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = newPin,
            onValueChange = { newPin = digit(it); message = null },
            label = { Text("新口令") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = digit(it); message = null },
            label = { Text("确认新口令") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) NeonMagenta else NeonGreen
            )
        }

        Button(
            onClick = {
                when {
                    oldPin.length < 4 -> { isError = true; message = "旧口令不正确" }
                    newPin.length < 4 -> { isError = true; message = "新口令至少 4 位" }
                    newPin != confirmPin -> { isError = true; message = "两次新口令不一致" }
                    else -> viewModel.changePin(oldPin, newPin) { ok ->
                        if (ok) {
                            isError = false
                            message = "口令已更新"
                            oldPin = ""; newPin = ""; confirmPin = ""
                        } else {
                            isError = true
                            message = "旧口令不正确"
                        }
                    }
                }
            },
            enabled = oldPin.length >= 4 && newPin.length >= 4 && confirmPin.length >= 4,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save))
        }
    }
}
