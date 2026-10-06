package com.digitalminds.grow

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import java.time.LocalDate

class MainActivity : Activity() {
    lateinit var store: Store
    lateinit var speaker: Speaker
    private lateinit var root: FrameLayout
    private var screen: Screen? = null
    var jawMode = false

    val today: Int get() = Data.dayOf(LocalDate.now())

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        initFonts(this)
        Data.load(this)
        store = Store(this)
        speaker = Speaker(this).also { it.enabled = store.voice }
        root = FrameLayout(this).apply { setBackgroundColor(C.BG) }
        setContentView(root)
        window.statusBarColor = C.BG; window.navigationBarColor = C.BG
        if (store.reminder >= 0) Reminder.schedule(this, store.reminder)
        swap(SplashScreen(this))
        root.postDelayed({ if (!isFinishing && screen is SplashScreen) showHome() }, 1050L)
    }

    private fun swap(s: Screen) {
        screen?.onDestroy()
        screen = s
        root.removeAllViews()
        s.view.alpha = 0f
        root.addView(s.view, FrameLayout.LayoutParams(-1, -1))
        s.view.animate().alpha(1f).setDuration(180).start()
    }

    fun keepAwake(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    fun showHome() { keepAwake(false); swap(HomeScreen(this)) }
    fun showSettings() { swap(SettingsScreen(this)) }
    fun showProgress() { swap(ProgressScreen(this)) }
    fun startWorkout(day: Int, steps: List<Step>) { keepAwake(true); swap(WorkoutScreen(this, day, steps, jawMode)) }
    fun showComplete(day: Int, steps: List<Step>, elapsedSec: Int) { keepAwake(false); swap(CompleteScreen(this, day, steps, elapsedSec, jawMode)) }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (screen is SplashScreen) { super.onBackPressed(); return }
        if (screen?.onBack() == true) return
        if (screen !is HomeScreen) showHome() else super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        // if the app stayed open past midnight (or the phone date changed), show the right day
        (screen as? HomeScreen)?.let { if (it.builtDay != today) showHome() }
    }

    override fun onPause() { screen?.onPause(); super.onPause() }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) Sprites.trim()
    }
    override fun onDestroy() { screen?.onDestroy(); speaker.shutdown(); super.onDestroy() }
}
