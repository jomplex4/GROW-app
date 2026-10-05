package com.digitalminds.grow

import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView

/** Brief launch screen with the GROW logo. */
class SplashScreen(val act: MainActivity) : Screen {
    override val view = FrameLayout(act)

    init {
        view.setBackgroundColor(C.BG)
        val logo = ImageView(act).apply {
            setImageResource(R.drawable.grow_logo)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            alpha = 0f; scaleX = 0.92f; scaleY = 0.92f
        }
        view.addView(logo, FrameLayout.LayoutParams(act.dp(150), act.dp(150), Gravity.CENTER))
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(480).start()
    }

    override fun onBack(): Boolean = false
}
