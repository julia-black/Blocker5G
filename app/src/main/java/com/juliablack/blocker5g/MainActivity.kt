package com.juliablack.blocker5g

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.juliablack.blocker5g.NetworkUtil.checkConnection
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private var valueDanger = 0
    private var isLaunchProtection = false

    private var appUpdateManager: AppUpdateManager? = null
    private var newVersionCode: Int? = null

    private val snackbarDownloading by lazy {
        Snackbar.make(
            findViewById(R.id.container),
            getString(R.string.loading),
            Snackbar.LENGTH_INDEFINITE
        )
    }

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                showSnackbarDownloading()
            }
            InstallStatus.DOWNLOADED -> {
                showSnackbarForCompleteUpdate()
            }
            else -> {
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    override fun onResume() {
        super.onResume()
        // adView.resume()
        if (valueDanger > 0) {
            showGradient()
        }

        checkUpdates()
        checkDownloadedUpdates()
    }

    override fun onPause() {
        //  adView.pause()
        super.onPause()
    }

    override fun onDestroy() {
        //   adView.destroy()
        appUpdateManager?.unregisterListener(listener)
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Preference.saveCanceledUpdate(this, newVersionCode)
            }
        }
    }

    private fun init() {
        initAds()
        startAnalyse()
        buttonScan.setOnClickListener {
            startAnalyse()
        }
        buttonProtection.setOnClickListener {
            if (isLaunchProtection) {
                unlaunchProtection()
            } else {
                launchProtection()
            }
        }
    }

    private fun initAds() {
//        MobileAds.initialize(this) {}
//
//        val adRequest = AdRequest.Builder().build()
//        adView.loadAd(adRequest)
    }

    private fun startAnalyse() {
        showView(isAnalyse = true)
        Handler().postDelayed({
            stateText.text = getString(R.string.scan_2)

            Handler().postDelayed({
                stateText.text = getString(R.string.scan_3)
                Handler().postDelayed({
                    valueDanger = checkConnection(this)
                    showView(result = valueDanger)
                }, TIMEOUT_WORK)
            }, TIMEOUT_WORK)
        }, TIMEOUT_WORK)
    }

    private fun checkUpdates() {
        appUpdateManager = AppUpdateManagerFactory.create(this)

        appUpdateManager?.registerListener(listener)

        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (!isDownloading(appUpdateInfo) && appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
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

    private fun isDownloading(appUpdateInfo: AppUpdateInfo?) =
        appUpdateInfo?.installStatus() == InstallStatus.DOWNLOADING || appUpdateInfo?.installStatus() == InstallStatus.DOWNLOADED

    private fun checkDownloadedUpdates() {
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                showSnackbarForCompleteUpdate()
            }
        }
    }

    private fun showSnackbarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.container),
            getString(R.string.downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(getString(R.string.install)) { appUpdateManager?.completeUpdate() }
            setActionTextColor(resources.getColor(R.color.colorAccent))
            show()
        }
    }

    private fun showSnackbarDownloading() {
        snackbarDownloading.show()
    }

    private fun launchProtection() {
        this.isLaunchProtection = true
        showView(isLaunchProtection = true)
    }

    private fun unlaunchProtection() {
        this.isLaunchProtection = false
        showView(result = valueDanger, isLaunchProtection = false)
    }

    private fun showGradient() {
        showGradient(
            ContextCompat.getColor(
                this,
                if (isLaunchProtection) {
                    R.color.colorProtected
                } else {
                    when (valueDanger) {
                        1 -> R.color.colorDanger1
                        2 -> R.color.colorDanger2
                        3 -> R.color.colorDanger3
                        4 -> R.color.colorDanger4
                        5 -> R.color.colorDanger5
                        else -> R.color.colorBackground
                    }
                }
            )
        )
    }

    private fun showGradient(@ColorInt color: Int) {
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(
                color,
                ContextCompat.getColor(this@MainActivity, R.color.colorBackground)
            )
        )
        gradientDrawable.cornerRadius = 0f

        topGradient.background = gradientDrawable
        bottomGradient.background = gradientDrawable
    }

    private fun showView(
        isAnalyse: Boolean = false,
        result: Int? = null,
        isLaunchProtection: Boolean = false
    ) {
        if (isAnalyse) {
            buttonScan.visibility = View.INVISIBLE
            stateText.visibility = View.VISIBLE
            searchImage.visibility = View.VISIBLE
            activeImage.visibility = View.INVISIBLE
            stateSubText.visibility = View.INVISIBLE
            buttonProtection.visibility = View.INVISIBLE
            showGradient(ContextCompat.getColor(this, R.color.colorAnalyse))
            stateText.text = getString(R.string.scan_1)
        } else if (result != null) {
            if (result == -1) {
                stateText.text = getString(R.string.not_connection)
                stateSubText.visibility = View.VISIBLE
                stateSubText.text = getString(R.string.need_connection)
                searchImage.visibility = View.INVISIBLE
                buttonScan.visibility = View.VISIBLE
                activeImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_no_signal
                    )
                )
                activeImage.visibility = View.VISIBLE
                buttonProtection.visibility = View.VISIBLE
                buttonProtection.text = getString(R.string.launch)
                buttonProtection.visibility = View.INVISIBLE
            } else {
                stateText.text = getString(R.string.level_danger, result.toString())
                stateSubText.text = getString(R.string.need_protection)

                stateSubText.visibility = if (result > 0) View.VISIBLE else View.INVISIBLE
                activeImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        if (result > 0)
                            R.drawable.ic_unactive_shield
                        else
                            R.drawable.ic_active_shield
                    )
                )
                activeImage.visibility = View.VISIBLE
                buttonProtection.visibility = if (result > 0) View.VISIBLE else View.INVISIBLE
                searchImage.visibility = View.INVISIBLE
                buttonScan.visibility = View.VISIBLE
                buttonProtection.text = getString(R.string.launch)
                showGradient()
            }
        } else {
            stateText.text =
                if (isLaunchProtection) getString(R.string.you_safe)
                else getString(R.string.level_danger, result.toString())

            searchImage.visibility = View.INVISIBLE
            buttonScan.visibility = View.VISIBLE
            stateSubText.visibility = View.VISIBLE

            stateSubText.text =
                if (isLaunchProtection) getString(R.string.launched_protection)
                else getString(R.string.need_protection)

            activeImage.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (isLaunchProtection)
                        R.drawable.ic_active_shield
                    else R.drawable.ic_unactive_shield
                )
            )
            activeImage.visibility = View.VISIBLE
            buttonProtection.visibility = View.VISIBLE

            buttonProtection.text =
                if (isLaunchProtection) getString(R.string.unlaunch)
                else getString(R.string.launch)

            showGradient()
        }
    }

    companion object {
        const val TIMEOUT_WORK = 1_500L
        const val UPDATE_REQUEST_CODE = 12
    }
}
