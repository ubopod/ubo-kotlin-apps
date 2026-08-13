package com.ubopod.uboapp.wear.ui.inputs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material.Switch
import com.ubopod.uboapp.wear.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.InputFieldDescription
import com.ubopod.ubokotlin.models.InputFieldType
import com.ubopod.ubokotlin.models.WebUIInputDescription
import kotlinx.coroutines.launch

/**
 * Compact watch-screen input form. Renders one field at a time inside a
 * scrolling column. Supported on the watch: TEXT, PASSWORD, NUMBER,
 * CHECKBOX, LONG (textarea is one field); SELECT / FILE / COLOR / DATE
 * / TIME fall back to plain TEXT entry — the screen real estate isn't
 * enough for proper pickers, but the user can still respond.
 *
 * Mirrors `ubo Watch App/Views/WatchInputFormView.swift`.
 */
@Composable
public fun WatchInputFormSheet(
    description: WebUIInputDescription,
    viewModel: DeviceViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val initial = description.fields.firstOrNull()
    var value by remember(description.id) { mutableStateOf(initial?.defaultValue.orEmpty()) }
    var bool by remember(description.id) { mutableStateOf(initial?.defaultValue?.lowercase() == "true") }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            description.title?.let {
                Text(it, style = MaterialTheme.typography.title3)
            }
        }
        item {
            description.prompt?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }
        }
        item {
            FieldEditor(
                field = initial,
                text = value,
                onTextChange = { value = it },
                checked = bool,
                onCheckedChange = { bool = it },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                Button(
                    onClick = {
                        scope.launch { runCatching { viewModel.client.cancelInput(description.id) } }
                        onDismiss()
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        val payload = if (initial?.type == InputFieldType.CHECKBOX) bool.toString() else value
                        // Server-side handlers read result.data, not value
                        // (mirrors the Web UI's inputs.tsx) — even this
                        // single-field form needs its field name -> value
                        // in the data map or submit silently no-ops.
                        val data = initial?.let { mapOf(it.name to payload) }.orEmpty()
                        scope.launch { runCatching { viewModel.client.provideInput(description.id, payload, data) } }
                        onDismiss()
                    },
                    colors = ButtonDefaults.primaryButtonColors(),
                ) { Text("Submit") }
            }
        }
    }
}

@Composable
private fun FieldEditor(
    field: InputFieldDescription?,
    text: String,
    onTextChange: (String) -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    if (field == null) return
    val label = field.label.ifEmpty { field.name }
    when (field.type) {
        InputFieldType.CHECKBOX -> ToggleChip(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = { Text(label) },
            toggleControl = {
                Switch(
                    checked = checked,
                    enabled = true,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        InputFieldType.PASSWORD,
        InputFieldType.TEXT,
        InputFieldType.LONG,
        InputFieldType.NUMBER,
        InputFieldType.RANGE,
        InputFieldType.SELECT,
        InputFieldType.FILE,
        InputFieldType.COLOR,
        InputFieldType.DATE,
        InputFieldType.TIME -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = MaterialTheme.typography.caption1)
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = field.type != InputFieldType.LONG,
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = when (field.type) {
                        InputFieldType.NUMBER, InputFieldType.RANGE -> KeyboardType.Number
                        InputFieldType.PASSWORD -> KeyboardType.Password
                        else -> KeyboardType.Text
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colors.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(6.dp),
            )
        }
    }
}
