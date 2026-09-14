package com.juliablack.blocker5g

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val languageOptions = listOf(
        LanguageOption(null, R.string.language_system_default),
        LanguageOption("en", R.string.language_english),
        LanguageOption("es", R.string.language_spanish),
        LanguageOption("pt", R.string.language_portuguese),
        LanguageOption("de", R.string.language_german),
        LanguageOption("pl", R.string.language_polish),
        LanguageOption("uk", R.string.language_ukrainian),
        LanguageOption("ru", R.string.language_russian)
    )

    private var valueDanger by mutableStateOf(0)
    private var isLaunchProtection by mutableStateOf(false)
    private var analyseStep by mutableStateOf(0)
    private var isAnalysing by mutableStateOf(false)
    private var isLanguageDialogVisible by mutableStateOf(false)
    private var languageContext by mutableStateOf<Context?>(null)

    private var systemBaseContext: Context? = null
    private var appUpdateManager: AppUpdateManager? = null
    private var newVersionCode: Int? = null
    private var snackbarHostState: SnackbarHostState? = null
    private var snackbarScope: CoroutineScope? = null
    private var snackbarJob: Job? = null

    override fun attachBaseContext(newBase: Context) {
        systemBaseContext = newBase
        super.attachBaseContext(Preference.applyLanguage(newBase))
    }

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> showSnackbarDownloading()
            InstallStatus.DOWNLOADED -> showSnackbarForCompleteUpdate()
            InstallStatus.FAILED, InstallStatus.CANCELED -> showSnackbarFailedUpdate()
            else -> Unit
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()

        languageContext = Preference.applyLanguage(systemBaseContext ?: this)

        setContent {
            val strings = languageContext ?: this@MainActivity
            val hostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()

            DisposableEffect(hostState, scope) {
                snackbarHostState = hostState
                snackbarScope = scope
                onDispose {
                    snackbarHostState = null
                    snackbarScope = null
                }
            }

            BlockerTheme {
                BlockerScreen(
                    strings = strings,
                    valueDanger = valueDanger,
                    isAnalysing = isAnalysing,
                    analyseStep = analyseStep,
                    isLaunchProtection = isLaunchProtection,
                    snackbarHostState = hostState,
                    onSettings = { isLanguageDialogVisible = true },
                    onScan = ::startAnalyse,
                    onProtection = {
                        if (isLaunchProtection) unlaunchProtection() else launchProtection()
                    },
                    onPlantItClick = ::openPlantIt
                )

                if (isLanguageDialogVisible) {
                    LanguageDialog(
                        strings = strings,
                        options = languageOptions,
                        selectedLanguage = Preference.getLanguageTag(this@MainActivity),
                        onDismiss = { isLanguageDialogVisible = false },
                        onConfirm = { language ->
                            Preference.saveLanguageTag(this@MainActivity, language)
                            languageContext = Preference.applyLanguage(systemBaseContext ?: this@MainActivity)
                            isLanguageDialogVisible = false
                        }
                    )
                }
            }
        }
        startAnalyse()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) configureSystemBars()
    }

    override fun onResume() {
        super.onResume()
        checkUpdates()
    }

    override fun onDestroy() {
        snackbarJob?.cancel()
        appUpdateManager?.unregisterListener(listener)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE && resultCode == RESULT_CANCELED) {
            Preference.saveCanceledUpdate(this, newVersionCode)
        }
    }

    private fun configureSystemBars() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun startAnalyse() {
        isLaunchProtection = false
        isAnalysing = true
        analyseStep = 1

        Handler(Looper.getMainLooper()).postDelayed({
            analyseStep = 2
            Handler(Looper.getMainLooper()).postDelayed({
                analyseStep = 3
                Handler(Looper.getMainLooper()).postDelayed({
                    valueDanger = NetworkUtil.checkConnection(this)
                    analyseStep = 0
                    isAnalysing = false
                }, TIMEOUT_WORK)
            }, TIMEOUT_WORK)
        }, TIMEOUT_WORK)
    }

    private fun launchProtection() {
        isLaunchProtection = true
    }

    private fun unlaunchProtection() {
        isLaunchProtection = false
    }

    private fun openPlantIt() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLANTIT_URL)))
    }

    private fun checkUpdates() {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager?.registerListener(listener)
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (isDownloaded(appUpdateInfo)) {
                showSnackbarForCompleteUpdate()
            } else if (!isDownloading(appUpdateInfo)
                && appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                && appUpdateInfo.availableVersionCode() != Preference.getCanceledVersionCode(this)
            ) {
                newVersionCode = appUpdateInfo.availableVersionCode()
                appUpdateManager?.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    private fun isDownloaded(appUpdateInfo: AppUpdateInfo?) =
        appUpdateInfo?.installStatus() == InstallStatus.DOWNLOADED

    private fun isDownloading(appUpdateInfo: AppUpdateInfo?) =
        appUpdateInfo?.installStatus() == InstallStatus.DOWNLOADING

    private fun showSnackbarDownloading() {
        launchSnackbar(localizedString(R.string.loading))
    }

    private fun showSnackbarForCompleteUpdate() {
        launchSnackbar(
            message = localizedString(R.string.downloaded),
            actionLabel = localizedString(R.string.install),
            onAction = { appUpdateManager?.completeUpdate() }
        )
    }

    private fun showSnackbarFailedUpdate() {
        launchSnackbar(
            message = localizedString(R.string.failed_download),
            actionLabel = localizedString(R.string.retry),
            onAction = {
                appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
                    appUpdateManager?.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.FLEXIBLE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                }
            }
        )
    }

    private fun launchSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: () -> Unit = {}
    ) {
        val hostState = snackbarHostState ?: return
        val scope = snackbarScope ?: return
        snackbarJob?.cancel()
        snackbarJob = scope.launch {
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    private fun localizedString(@StringRes resourceId: Int, vararg formatArgs: Any): String =
        (languageContext ?: this).getString(resourceId, *formatArgs)

    companion object {
        const val TIMEOUT_WORK = 1_500L
        const val UPDATE_REQUEST_CODE = 12
        const val PLANTIT_URL = "https://singlelab.cat/download.html"
    }
}
