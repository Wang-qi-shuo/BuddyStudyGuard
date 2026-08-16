package com.buddy.studyguard.parent.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.buddy.studyguard.R
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.parent.ui.pin.PinManageScreen

/**
 * 家长模式入口：口令验证。验证成功调用 [onSuccess]。
 */
@Composable
fun ParentEntryScreen(
    onSuccess: () -> Unit,
    viewModel: ParentEntryViewModel = hiltViewModel()
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showDefaultWarning by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }

    if (showChangePin) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { showChangePin = false }) {
                Text(stringResource(R.string.back))
            }
            PinManageScreen()
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.parent_entry_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(8); error = false },
            label = { Text(stringResource(R.string.parent_entry_hint)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        if (error) {
            Text(
                text = stringResource(R.string.parent_entry_wrong),
                color = NeonMagenta,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (showDefaultWarning) {
            Text(
                text = "检测到默认口令",
                color = NeonMagenta,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "当前使用默认口令 123456，存在安全风险，建议立即修改为仅自己知道的口令。",
                color = NeonMagenta,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { showChangePin = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("去修改口令")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSuccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("仍要进入")
            }
        } else {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.verify(pin) { ok, isDefault ->
                        if (ok) {
                            if (isDefault) showDefaultWarning = true else onSuccess()
                        } else {
                            error = true
                        }
                    }
                },
                enabled = pin.length >= 4,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.confirm))
            }
        }
    }
}
