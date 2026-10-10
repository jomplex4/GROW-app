package com.digitalminds.grow

import android.view.Gravity
import android.widget.*

/** Info screen: About this app + Reset progress (from the ⓘ icon). */
class InfoScreen(val act: MainActivity) : Screen {
    override val view = FrameLayout(act)

    init {
        view.setBackgroundColor(C.BG)
        val sv = ScrollView(act).apply { isVerticalScrollBarEnabled = false }
        val col = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(act.dp(24), act.dp(56), act.dp(24), act.dp(40))
        }
        sv.addView(col)
        view.addView(sv)

        // Back button
        val back = IconView(act, Ic.BACK).apply {
            setOnClickListener { act.showHome() }
        }
        view.addView(back, FrameLayout.LayoutParams(act.dp(48), act.dp(48)).also { it.topMargin = act.dp(8) })

        // About section
        col.addView(act.tv("ABOUT GROWTH", 11f, C.GRAY, HEAD).apply { letterSpacing = 0.18f }, lp(MATCH, WRAP))
        col.addView(act.tv("GROWTH", 28f, C.WHITE, HEAD).apply { letterSpacing = 0.04f }, lp(MATCH, WRAP).also { it.topMargin = act.dp(12) })
        col.addView(act.tv("Daily posture, impact and strength program.\nDesigned for long-term consistency.", 14f, C.GRAY, BODY).apply { setLineSpacing(act.dp(4).toFloat(), 1f) }, lp(MATCH, WRAP).also { it.topMargin = act.dp(8) })
        col.addView(act.tv("Version 2.4  ·  ${Data.TOTAL} days  ·  ${Data.exercises.size} exercises", 12f, C.DIM, BODY), lp(MATCH, WRAP).also { it.topMargin = act.dp(6) })

        // DIGITALMINDS branding
        val dm = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val dmLogo = android.widget.ImageView(act).apply {
            setImageResource(R.drawable.dm_logo)
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        dm.addView(act.tv("Powered by ", 12f, C.DIM, BODY))
        dm.addView(dmLogo, LinearLayout.LayoutParams(act.dp(110), act.dp(22)))
        col.addView(dm, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })

        // Attribution required by the licence of the exercise illustrations
        val credit = act.tv("Exercise illustrations by RepDB (repdb.co)", 12f, C.GRAY, BODY).apply {
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
            setOnClickListener {
                try { act.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://repdb.co"))) } catch (_: Exception) {}
            }
        }
        col.addView(credit, lp(MATCH, WRAP).also { it.topMargin = act.dp(14) })

        // Divider
        val div = android.view.View(act).apply { setBackgroundColor(C.CARD2) }
        col.addView(div, lp(MATCH, 1).also { it.setMargins(0, act.dp(32), 0, act.dp(28)) })

        // Reset progress
        col.addView(act.tv("RESET PROGRESS", 11f, C.GRAY, HEAD).apply { letterSpacing = 0.18f })
        col.addView(act.tv("Erases completed days, streaks and measurements.\nSettings are kept. Cannot be undone.", 13f, C.GRAY, BODY).apply { setLineSpacing(act.dp(3).toFloat(), 1f) }, lp(MATCH, WRAP).also { it.topMargin = act.dp(8) })
        col.addView(act.button("RESET ALL PROGRESS", C.CARD2, C.GRAY, 13f, 48, 24f).apply {
            setOnClickListener { confirmReset() }
        }, lp(MATCH, act.dp(48)).also { it.topMargin = act.dp(16) })
    }

    private fun confirmReset() {
        val d = android.app.Dialog(act)
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val box = LinearLayout(act).apply {
            orientation = LinearLayout.VERTICAL
            background = act.round(C.CARD, 24f)
            setPadding(act.dp(24), act.dp(24), act.dp(24), act.dp(20))
        }
        box.addView(act.tv("Reset progress?", 20f, C.WHITE, HEAD))
        box.addView(act.tv("This erases completed days, streaks and measurements. Cannot be undone.", 13f, C.GRAY, BODY).apply {
            setLineSpacing(act.dp(3).toFloat(), 1f); margins(t = act.dp(8))
        })
        val row = LinearLayout(act).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(act.button("CANCEL", C.RED, C.WHITE, 14f, 48, 24f).apply { setOnClickListener { d.dismiss() } },
            lp(0, act.dp(48), 1f).also { it.rightMargin = act.dp(8) })
        row.addView(act.button("RESET", C.CARD2, C.WHITE, 14f, 48, 24f).apply {
            setOnClickListener { act.store.resetProgress(); d.dismiss(); act.showHome() }
        }, lp(0, act.dp(48), 1f))
        box.addView(row, lp(MATCH, WRAP).also { it.topMargin = act.dp(20) })
        d.setContentView(box)
        d.window?.apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout((act.resources.displayMetrics.widthPixels * 0.88f).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        d.show()
    }

    override fun onBack(): Boolean { act.showHome(); return true }
}
