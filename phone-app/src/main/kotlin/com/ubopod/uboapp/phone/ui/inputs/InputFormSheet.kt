package com.ubopod.uboapp.phone.ui.inputs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.ubopod.uboapp.phone.ui.common.LinkifiedText
import com.ubopod.uboapp.phone.viewmodel.DeviceViewModel
import com.ubopod.ubokotlin.models.InputFieldDescription
import com.ubopod.ubokotlin.models.InputFieldType
import com.ubopod.ubokotlin.models.WebUIInputDescription
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

/**
 * Native rendering of a [WebUIInputDescription] in a Material 3
 * [ModalBottomSheet].
 *
 * Mirrors `ubo-swift-app/ubo-swift-app/Views/Inputs/InputFormView.swift`.
 * Each [InputFieldType] maps to a native control; on **Submit** the form
 * dispatches `client.provideInput`, on **Cancel** it dispatches
 * `client.cancelInput`. [onDismiss] is called immediately on close so the
 * parent can stop presenting before the device's state update propagates
 * back over gRPC.
 *
 * `prompt` is the primary heading and `title` an optional elaboration
 * shown below it — matching the Web UI's `Inputs` component (`prompt` ->
 * `DialogTitle`, `title` -> linkified subtitle), not the field names'
 * intuitive-looking-but-wrong opposite reading. Getting this backwards
 * previously showed a several-hundred-character OAuth URL as plain,
 * unclickable body text instead of the linkified heading it should be.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun InputFormSheet(
    description: WebUIInputDescription,
    viewModel: DeviceViewModel,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // values map keyed by field.name
    val values = remember(description.id) {
        mutableStateMapOf<String, String>().apply {
            description.fields.forEach { put(it.name, seedValue(it)) }
        }
    }
    val errors = remember(description.id) { mutableStateMapOf<String, String>() }
    var submitting by remember(description.id) { mutableStateOf(false) }
    // FILE fields picked but not yet uploaded, keyed by field name. Sent in
    // the background after submit — see `submit()`.
    val pendingUploads = remember(description.id) { mutableStateMapOf<String, PendingUpload>() }

    LaunchedEffect(description.id) {
        // Re-seed defaults if the description changes (rarely; the device
        // generally creates a fresh demand id per request).
        description.fields.forEach { values[it.name] = seedValue(it) }
    }

    fun validate(): Boolean {
        errors.clear()
        for (field in description.fields) {
            val v = values[field.name].orEmpty()
            if (field.required && v.isEmpty()) {
                errors[field.name] = "Required"
                continue
            }
            val pattern = field.pattern
            if (!pattern.isNullOrEmpty() && v.isNotEmpty()) {
                val ok = runCatching { Regex(pattern).containsMatchIn(v) }.getOrDefault(true)
                if (!ok) errors[field.name] = "Invalid format"
            }
        }
        return errors.isEmpty()
    }

    fun submit() {
        if (!validate()) return
        submitting = true
        // `value` is the scalar shown to single-field callers; `data` (every
        // field's name -> value) is what server-side handlers for
        // multi-field forms actually read (mirrors the Web UI's inputs.tsx).
        val data = values.toMutableMap()
        // FILE fields: the server never sees the bytes through `data` — it
        // reads `{field}_upload_id`/`{field}_name` and waits for a matching
        // chunked upload (started below) to complete. Mirrors the Web UI's
        // `inputs.tsx`.
        for ((fieldName, pending) in pendingUploads) {
            data["${fieldName}_upload_id"] = pending.uploadId
            data["${fieldName}_name"] = pending.filename
        }
        val scalar = description.fields.firstOrNull()?.let { data[it.name] }.orEmpty()
        onDismiss()
        // viewModel.viewModelScope, not the composable-scoped `scope`:
        // onDismiss() above removes this sheet from composition, which
        // cancels `scope` — a multi-chunk upload mid-flight at that moment
        // would die with the server-side session stuck waiting forever for
        // chunks/completion that will never arrive ("stuck uploading").
        // viewModelScope survives the sheet closing.
        viewModel.viewModelScope.launch {
            val result = runCatching { viewModel.client.provideInput(description.id, scalar, data) }
            submitting = false
            if (result.isFailure) return@launch
            // Uploads run after the form has been accepted, same as the Web
            // UI — the server's await_completed_upload has its own timeout
            // to cover this arriving after the InputProvideAction that
            // references it.
            for (pending in pendingUploads.values) {
                launch {
                    runCatching { viewModel.client.uploadFile(pending.uploadId, pending.filename, pending.data) }
                }
            }
        }
    }

    fun cancel() {
        onDismiss()
        viewModel.viewModelScope.launch {
            runCatching { viewModel.client.cancelInput(description.id) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { cancel() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            description.prompt?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            description.title?.takeIf { it.isNotEmpty() }?.let {
                LinkifiedText(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            description.fields.forEach { field ->
                FieldEditor(
                    field = field,
                    value = values[field.name].orEmpty(),
                    onChange = { values[field.name] = it },
                    error = errors[field.name],
                    onFilePicked = { pendingUploads[field.name] = it },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { cancel() },
                    modifier = Modifier.weight(1f),
                ) { Text("Cancel") }
                Button(
                    onClick = { submit() },
                    enabled = !submitting,
                    modifier = Modifier.weight(1f),
                ) { Text("Submit") }
            }
        }
    }
}

/**
 * Initial/re-seeded value for a field. [InputFieldType.RANGE] is stored as
 * an integer 0-100 string (parsed from [InputFieldDescription.defaultValue],
 * defaulting to 50) since the field has no natural string default; every
 * other type just uses `defaultValue` verbatim. Mirrors the special case in
 * Swift's `InputFormView.seedDefaults()`.
 */
private fun seedValue(field: InputFieldDescription): String =
    if (field.type == InputFieldType.RANGE) {
        (field.defaultValue?.toDoubleOrNull() ?: 50.0).toInt().toString()
    } else {
        field.defaultValue.orEmpty()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldEditor(
    field: InputFieldDescription,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    onFilePicked: (PendingUpload) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (field.type) {
            InputFieldType.TEXT -> OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(field.label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.PASSWORD -> OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(field.label) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.NUMBER -> OutlinedTextField(
                value = value,
                onValueChange = { input ->
                    onChange(input.filter { it.isDigit() || it == '.' || it == '-' })
                },
                label = { Text(field.label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.LONG -> OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(field.label) },
                minLines = 4,
                maxLines = 8,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.CHECKBOX -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = value == "true",
                    onCheckedChange = { onChange(if (it) "true" else "false") },
                )
                Text(field.label, style = MaterialTheme.typography.bodyMedium)
            }
            InputFieldType.SELECT -> SelectField(
                field = field,
                value = value,
                onChange = onChange,
                error = error,
            )
            InputFieldType.COLOR -> OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text("${field.label} (hex)") },
                placeholder = { Text("#rrggbb") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.DATE -> OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(field.label) },
                placeholder = { Text("YYYY-MM-DD") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.TIME -> OutlinedTextField(
                value = value,
                onValueChange = onChange,
                label = { Text(field.label) },
                placeholder = { Text("HH:MM") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            InputFieldType.FILE -> FilePickerField(
                field = field,
                value = value,
                onChange = onChange,
                onFilePicked = onFilePicked,
            )
            InputFieldType.RANGE -> {
                val sliderValue = value.toFloatOrNull() ?: 50f
                Text(field.label)
                Slider(
                    value = sliderValue,
                    onValueChange = { onChange(it.toInt().toString()) },
                    valueRange = 0f..100f,
                    steps = 99,
                )
                Text(
                    "${sliderValue.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        field.description?.takeIf { it.isNotEmpty() }?.let {
            LinkifiedText(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectField(
    field: InputFieldDescription,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(field.label) },
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(
                type = androidx.compose.material3.MenuAnchorType.PrimaryNotEditable,
                enabled = true,
            ).fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            field.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * A FILE field picked and read into memory, ready to hand to
 * `UboClient.uploadFile`. [uploadId] is generated on pick (not on submit)
 * so it's stable if the user re-opens the picker before submitting.
 */
public data class PendingUpload(val uploadId: String, val filename: String, val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PendingUpload
        return uploadId == other.uploadId && filename == other.filename && data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = uploadId.hashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }

@Composable
private fun FilePickerField(
    field: InputFieldDescription,
    value: String,
    onChange: (String) -> Unit,
    onFilePicked: (PendingUpload) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var readError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        readError = null
        scope.launch(Dispatchers.IO) {
            val filename = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "file"
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                if (bytes == null) {
                    readError = "Couldn't read that file."
                    return@withContext
                }
                onChange(filename)
                onFilePicked(PendingUpload(uploadId = UUID.randomUUID().toString(), filename = filename, data = bytes))
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(field.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(value.ifEmpty { "Choose file" })
        }
        if (value.isNotEmpty()) {
            Text(
                "Selected: $value",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        readError?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
