//package com.terrobytes.cybermanaver2.ui.composable.wizard
//
//import com.terrobytes.cybermanaver2.ui.composable.colors.BgDeep
//import com.terrobytes.cybermanaver2.ui.composable.colors.Ok
//import com.terrobytes.cybermanaver2.ui.composable.colors.White
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.CheckCircle
//import androidx.compose.material3.Icon
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun DoneContent() {
//    Scaffold(containerColor = BgDeep) { innerPadding ->
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//        ) {
//            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Ok, modifier = Modifier)
//            Text("Configuration terminée", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
//        }
//    }
//}
//
///**
// * Enchaîne les 5 écrans avec un index local - purement pour prévisualiser
// * le parcours. Le vrai enchaînement viendra du childStack Decompose.
// */
//@Composable
//fun WizardHost() {
//    var step by remember { mutableIntStateOf(0) }
//
//    when (step) {
//        0 -> ConnectionContent(onContinue = { step = 1 })
//        1 -> TemplateContent(onBack = { step = 0 }, onContinue = { step = 2 })
//        2 -> BackupContent(onBack = { step = 1 }, onContinue = { step = 3 })
//        3 -> InjectionContent(onBack = { step = 2 }, onContinue = { step = 4 })
//        4 -> VerifyContent(onBack = { step = 3 }, onContinue = { step = 5 })
//        else -> DoneContent()
//    }
//}