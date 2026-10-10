package com.digitalminds.grow

import android.graphics.Typeface
import android.view.Gravity
import android.widget.*

/** Launch screen: GROWTH logo centred on black, "POWERED BY DIGITALMINDS" at the bottom - same style as COMET. */
class SplashScreen(val act: MainActivity) : Screen {
    override val view = FrameLayout(act)

    init {
        view.setBackgroundColor(0xFF000000.toInt())

        // Logo centred
        val logo = android.widget.ImageView(act).apply {
            setImageResource(R.drawable.grow_logo)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            alpha = 0f
        }
        val lp = FrameLayout.LayoutParams(act.dp(200), act.dp(222), Gravity.CENTER)
        lp.bottomMargin = act.dp(60)
        view.addView(logo, lp)

        // "POWERED BY" + DM logo at the bottom
        val bottom = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        bottom.addView(act.tv("POWERED BY", 10f, 0xFF555555.toInt(), HEAD, Gravity.CENTER).apply {
            letterSpacing = 0.18f
        })
        val dmLogo = android.widget.ImageView(act).apply {
            setImageResource(R.drawable.dm_logo)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            alpha = 0.85f
        }
        bottom.addView(dmLogo, LinearLayout.LayoutParams(act.dp(160), act.dp(36)).also { it.topMargin = act.dp(8) })

        val blp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
        blp.bottomMargin = act.dp(48)
        view.addView(bottom, blp)

        // Fade in
        logo.animate().alpha(1f).setDuration(520).setStartDelay(80).start()
    }

    override fun onBack(): Boolean = false
}
