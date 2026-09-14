package com.juliablack.blocker5g

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RadioButton
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

internal data class LanguageOption(val tag: String?, @StringRes val name: Int)

@Composable
internal fun BlockerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = darkColors(
            primary = colorResource(R.color.colorPrimary),
            secondary = colorResource(R.color.colorAccent),
            background = colorResource(R.color.colorBackground),
            surface = colorResource(R.color.colorBackground),
            onPrimary = colorResource(R.color.colorText),
            onSecondary = colorResource(R.color.colorBackground),
            onBackground = colorResource(R.color.colorText),
            onSurface = colorResource(R.color.colorText)
        ),
        content = content
    )
}

@Composable
internal fun BlockerScreen(
    strings: Context,
    valueDanger: Int,
    isAnalysing: Boolean,
    analyseStep: Int,
    isLaunchProtection: Boolean,
    snackbarHostState: SnackbarHostState,
    onSettings: () -> Unit,
    onScan: () -> Unit,
    onProtection: () -> Unit
) {
    val background = colorResource(R.color.colorBackground)
    val gradientColor = when {
        isAnalysing -> colorResource(R.color.colorAnalyse)
        isLaunchProtection -> colorResource(R.color.colorProtected)
        valueDanger == 1 -> colorResource(R.color.colorDanger1)
        valueDanger == 2 -> colorResource(R.color.colorDanger2)
        valueDanger == 3 -> colorResource(R.color.colorDanger3)
        valueDanger == 4 -> colorResource(R.color.colorDanger4)
        valueDanger == 5 -> colorResource(R.color.colorDanger5)
        else -> background
    }
    val title = when {
        isAnalysing -> strings.getString(
            when (analyseStep) {
                2 -> R.string.scan_2
                3 -> R.string.scan_3
                else -> R.string.scan_1
            }
        )
        isLaunchProtection -> strings.getString(R.string.you_safe)
        valueDanger == -1 -> strings.getString(R.string.not_connection)
        else -> strings.getString(R.string.level_danger, valueDanger.toString())
    }
    val subtitle = when {
        isAnalysing -> null
        isLaunchProtection -> strings.getString(R.string.launched_protection)
        valueDanger == -1 -> strings.getString(R.string.need_connection)
        else -> strings.getString(R.string.need_protection)
    }
    val showProtection = !isAnalysing && (isLaunchProtection || valueDanger > 0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(124.dp)
                .background(Brush.verticalGradient(listOf(gradientColor, background)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(88.dp)
                .background(Brush.verticalGradient(listOf(background, gradientColor)))
        )

        IconButton(
            onClick = onSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = strings.getString(R.string.settings),
                tint = colorResource(R.color.colorText)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 144.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    color = colorResource(R.color.colorText),
                    textAlign = TextAlign.Center,
                    fontSize = 22.sp,
                    style = MaterialTheme.typography.h6
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = subtitle,
                        color = colorResource(R.color.colorText),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isAnalysing) {
                    AndroidView(
                        factory = { RadarAnimationView(it) },
                        modifier = Modifier.size(124.dp)
                    )
                } else {
                    val image = when {
                        valueDanger == -1 -> R.drawable.ic_no_signal
                        isLaunchProtection -> R.drawable.ic_active_shield
                        valueDanger > 0 -> R.drawable.ic_unactive_shield
                        else -> R.drawable.ic_active_shield
                    }
                    androidx.compose.foundation.Image(
                        painter = painterResource(image),
                        contentDescription = null,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }

            if (!isAnalysing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        onClick = onScan,
                        border = BorderStroke(2.dp, colorResource(R.color.colorText)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colorResource(R.color.colorText)
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = strings.getString(R.string.start_scanner),
                            color = colorResource(R.color.colorText),
                            fontSize = 16.sp
                        )
                    }
                    if (showProtection) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onProtection,
                            border = BorderStroke(2.dp, colorResource(R.color.colorText)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colorResource(R.color.colorText)
                            ),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp)
                        ) {
                            Text(
                                text = strings.getString(
                                    if (isLaunchProtection) R.string.unlaunch else R.string.launch
                                ),
                                color = colorResource(R.color.colorText),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
internal fun LanguageDialog(
    strings: Context,
    options: List<LanguageOption>,
    selectedLanguage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var pendingLanguage by remember(selectedLanguage) { mutableStateOf(selectedLanguage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.getString(R.string.language_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pendingLanguage = option.tag }
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = pendingLanguage == option.tag,
                            onClick = { pendingLanguage = option.tag }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.getString(option.name))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pendingLanguage) }) {
                Text(strings.getString(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.getString(R.string.cancel))
            }
        }
    )
}

@Preview(
    name = "Protected screen",
    showBackground = true,
    backgroundColor = 0xFF000000,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun BlockerScreenPreview() {
    val context = LocalContext.current

    BlockerTheme {
        BlockerScreen(
            strings = context,
            valueDanger = 0,
            isAnalysing = false,
            analyseStep = 0,
            isLaunchProtection = true,
            snackbarHostState = remember { SnackbarHostState() },
            onSettings = {},
            onScan = {},
            onProtection = {}
        )
    }
}
