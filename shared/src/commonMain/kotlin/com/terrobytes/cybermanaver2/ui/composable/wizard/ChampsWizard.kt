package com.terrobytes.cybermanaver2.ui.composable.wizard

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terrobytes.cybermanaver2.ui.composable.colors.*
import java.nio.charset.StandardCharsets

@Composable
fun ChampWizard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxlength: Int = 0,
    isSsid: Boolean = false,
    isPassword: Boolean = false,
    isNumeric: Boolean = false,
    minValue: Int = Int.MIN_VALUE,
    maxValue: Int = Int.MAX_VALUE,
    errorMessage: String? = null,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val numericRegex = Regex("^[0-9]*$")

    Column(modifier = modifier.padding(bottom = 4.dp)) {
        Text(
            text = label,
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                if (isSsid) {
                    if (maxlength <= 0) {
                        onValueChange(newValue)
                    } else {
                        val octets = newValue.toByteArray(StandardCharsets.UTF_8)
                        if (octets.size <= maxlength) {
                            onValueChange(newValue)
                        }
                    }
                } else if (isPassword) {
                    if (newValue.length <= 63) {
                        onValueChange(newValue)
                    }
                } else if (isNumeric) {
                    if (newValue.matches(numericRegex)) {
                        if (newValue.isEmpty()) {
                            onValueChange(minValue.toString())
                        } else {
                            val intValue = newValue.toIntOrNull()
                            if (intValue != null && intValue in minValue..maxValue) {
                                onValueChange(newValue)
                            }
                        }
                    }
                } else {
                    onValueChange(newValue)
                }
            },
            singleLine = true,
            isError = errorMessage != null,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = if (isNumeric) KeyboardType.Number else if (isPassword) KeyboardType.Password else KeyboardType.Text,
            ),
            trailingIcon = when {
                isPassword -> {
                    {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Masquer" else "Afficher"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description, tint = Muted)
                        }
                    }
                }
                isNumeric -> {
                    {
                        Row(modifier = Modifier.padding(end = 4.dp)) {
                            // Bouton Moins
                            IconButton(
                                onClick = {
                                    val current = value.toIntOrNull() ?: minValue
                                    if (current > minValue) {
                                        onValueChange((current - 1).toString())
                                    }
                                },
                                enabled = (value.toIntOrNull() ?: minValue) > minValue
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Diminuer",
                                    tint = if ((value.toIntOrNull() ?: minValue) > minValue) Accent else Muted
                                )
                            }
                            // Bouton Plus
                            IconButton(
                                onClick = {
                                    val current = value.toIntOrNull() ?: minValue
                                    if (current < maxValue) {
                                        onValueChange((current + 1).toString())
                                    }
                                },
                                enabled = (value.toIntOrNull() ?: maxValue) < maxValue
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropUp,
                                    contentDescription = "Augmenter",
                                    tint = if ((value.toIntOrNull() ?: maxValue) < maxValue) Accent else Muted
                                )
                            }
                        }
                    }
                }
                else -> null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedBorderColor = Accent,
                unfocusedBorderColor = BorderColor,
                errorBorderColor = Danger,
                focusedContainerColor = BgDeep,
                unfocusedContainerColor = BgDeep,
                cursorColor = Accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (errorMessage != null) {
            Text(errorMessage, color = Danger, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
        }
    }
}