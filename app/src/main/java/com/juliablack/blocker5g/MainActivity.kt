package com.juliablack.blocker5g

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.juliablack.blocker5g.NetworkUtil.checkConnection
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var valueDanger = 0
    private var isLaunchProtection = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
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

    private fun startAnalyse() {
        showView(isAnalyse = true)
        Handler().postDelayed({
            stateText.text = getString(R.string.scan_2)

            Handler().postDelayed({
                stateText.text = getString(R.string.scan_3)
                Handler().postDelayed({
                    valueDanger = checkConnection(this)
                    showView(result = valueDanger)
                }, 1_500)
            }, 1_500)
        }, 1_500)
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
                when (valueDanger) {
                    1 -> R.color.colorDanger1
                    2 -> R.color.colorDanger2
                    3 -> R.color.colorDanger3
                    4 -> R.color.colorDanger4
                    5 -> R.color.colorDanger5
                    else -> R.color.colorBackground
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
            searchImage.visibility = View.VISIBLE
            stateText.visibility = View.VISIBLE
            unactiveImage.visibility = View.INVISIBLE
            activeImage.visibility = View.INVISIBLE
            stateSubText.visibility = View.INVISIBLE
            notConnectionImage.visibility = View.INVISIBLE
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
                unactiveImage.visibility = View.INVISIBLE
                activeImage.visibility = View.INVISIBLE
                notConnectionImage.visibility = View.VISIBLE
                buttonProtection.visibility = View.VISIBLE
                buttonProtection.text = getString(R.string.launch)
                buttonProtection.visibility = View.INVISIBLE
            } else {
                stateText.text = getString(R.string.level_danger, result.toString())
                stateSubText.text = getString(R.string.need_protection)

                stateSubText.visibility = if (result > 0) View.VISIBLE else View.INVISIBLE
                unactiveImage.visibility = if (result > 0) View.VISIBLE else View.INVISIBLE
                activeImage.visibility = if (result > 0) View.INVISIBLE else View.VISIBLE
                buttonProtection.visibility = if (result > 0) View.VISIBLE else View.INVISIBLE

                searchImage.visibility = View.INVISIBLE
                buttonScan.visibility = View.VISIBLE
                notConnectionImage.visibility = View.INVISIBLE

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

            unactiveImage.visibility = if (isLaunchProtection) View.INVISIBLE else View.VISIBLE
            notConnectionImage.visibility = View.INVISIBLE
            activeImage.visibility = if (isLaunchProtection) View.VISIBLE else View.INVISIBLE
            buttonProtection.visibility = View.VISIBLE

            buttonProtection.text =
                if (isLaunchProtection) getString(R.string.unlaunch)
                else getString(R.string.launch)

            if (isLaunchProtection) {
                showGradient(ContextCompat.getColor(this, R.color.colorProtected))
            } else {
                showGradient()
            }
        }
    }
}
