package com.ubopod.uboapp.wear.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Labelled text input with a visible container.
 *
 * A bare [BasicTextField] on Wear draws nothing of its own, so on the pure
 * black watch background an *empty* field was invisible — there was no way to
 * tell a tappable input from a plain label. This wraps it in a filled,
 * outlined box with a minimum height so the field is obvious whether or not it
 * holds text, and brightens the outline on focus so the active field is
 * unambiguous while the keyboard is up.
 *
 * Used by every text input in the watch app; prefer it over a raw
 * `BasicTextField` so the three entry screens stay consistent.
 */
@Composable
internal fun WatchTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var isFocused by remember { mutableStateOf(false) }

    // Fill stays subtle; the outline does the work of announcing "this is an
    // input". Both are derived from onSurface so they track the theme.
    val fill = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
    val outline = if (isFocused) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = 0.35f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.caption2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .onFocusChanged { isFocused = it.isFocused },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Without a minimum height an empty field collapses to
                        // nothing — the original bug.
                        .defaultMinSize(minHeight = 34.dp)
                        .background(fill, RoundedCornerShape(10.dp))
                        .border(1.dp, outline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.45f),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
