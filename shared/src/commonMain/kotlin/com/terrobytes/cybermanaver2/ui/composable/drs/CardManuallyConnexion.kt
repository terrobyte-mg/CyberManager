package com.terrobytes.cybermanaver2.ui.composable.drs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Carte de connexion manuelle à un routeur MikroTik, affichée uniquement lorsque l'utilisateur veux une connexion manuelle
 *  à un routeur.
 */
@Preview
@Composable
fun CardManuallyConnexion(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (addressIp: String, username: String, password: String) -> Unit = { _, _, _ -> },
    onCancel: () -> Unit = {}
) {
    var adressIp by remember { mutableStateOf("") }
    val ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex()

    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val accent = Color(0xFF1A8CFF)
    val fieldBg = Color(0xFF141C24)
    val borderColor = Color(0xFF232D38)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0B1319), shape = RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Connexion admin", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Connexion manuelle au routeur MikroTik.",
            color = Color(0xFF8E9297),
            fontSize = 13.sp
        )

        OutlinedTextField(
            value = adressIp,
            onValueChange = {
                if (isValidPartialIp(it)) {
                    adressIp = it
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            label = { Text("Adresse IP") },
            leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg,
                disabledContainerColor = fieldBg,
                focusedBorderColor = accent,
                unfocusedBorderColor = borderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = accent,
                focusedLabelColor = accent,
                unfocusedLabelColor = Color(0xFF8E9297),
                focusedLeadingIconColor = accent,
                unfocusedLeadingIconColor = Color(0xFF5F6368)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            label = { Text("Identifiant") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg,
                disabledContainerColor = fieldBg,
                focusedBorderColor = accent,
                unfocusedBorderColor = borderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = accent,
                focusedLabelColor = accent,
                unfocusedLabelColor = Color(0xFF8E9297),
                focusedLeadingIconColor = accent,
                unfocusedLeadingIconColor = Color(0xFF5F6368)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            label = { Text("Mot de passe") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF8E9297)
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg,
                disabledContainerColor = fieldBg,
                focusedBorderColor = accent,
                unfocusedBorderColor = borderColor,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = accent,
                focusedLabelColor = accent,
                unfocusedLabelColor = Color(0xFF8E9297),
                focusedLeadingIconColor = accent,
                unfocusedLeadingIconColor = Color(0xFF5F6368)
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(errorMessage, color = Color(0xFFFF5C5C), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .alpha(if (isLoading) 0.6f else 1f)
                .background(accent, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (!isLoading) Modifier.clickable { onSubmit(adressIp, username, password) } else Modifier
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connexion…", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text("Se connecter", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Annuler",
            color = Color(0xFF8E9297),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(enabled = !isLoading) { onCancel() }
        )
    }
}

private fun isValidPartialIp(input: String): Boolean {

    if (input.isEmpty()) return true

    if (!input.matches(Regex("[0-9.]*")))
        return false

    val parts = input.split(".")

    // Maximum 4 blocs
    if (parts.size > 4)
        return false

    for (part in parts) {

        // Pendant la saisie on accepte un bloc vide
        if (part.isEmpty())
            continue

        // Maximum 3 chiffres
        if (part.length > 3)
            return false

        val value = part.toInt()

        if (value !in 0..255)
            return false
    }

    return true
}