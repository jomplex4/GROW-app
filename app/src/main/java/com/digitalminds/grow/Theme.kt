package com.digitalminds.grow

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

object C {
    const val BG = 0xFF000000.toInt()
    const val CARD = 0xFF151517.toInt()
    const val CARD2 = 0xFF1F2023.toInt()
    const val RED = 0xFFDC0000.toInt()
    const val REDB = 0xFFFF2A1F.toInt()
    const val REDD = 0xFFDC0000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val GRAY = 0xFF9A9CA4.toInt()
    const val DIM = 0xFF6B6D75.toInt()
    const val BLACK = 0xFF000000.toInt()
}

fun Context.dp(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics).toInt()
fun Context.dp(v: Int): Int = dp(v.toFloat())

lateinit var HEAD: Typeface
lateinit var BODY: Typeface
lateinit var MED: Typeface

/** Space Grotesk (variable font) from assets; falls back to the system sans if anything fails. */
fun initFonts(ctx: Context) {
    fun make(weight: Int, fallback: Typeface): Typeface = try {
        android.graphics.Typeface.Builder(ctx.assets, "fonts/spacegrotesk.ttf")
            .setFontVariationSettings("'wght' $weight").build() ?: fallback
    } catch (e: Exception) { fallback }
    HEAD = make(700, Typeface.create("sans-serif-condensed", Typeface.BOLD))
    MED = make(500, Typeface.create("sans-serif-medium", Typeface.NORMAL))
    BODY = make(400, Typeface.create("sans-serif", Typeface.NORMAL))
}

fun Context.tv(text: String, sp: Float, color: Int = C.WHITE, tf: Typeface = MED, gravity: Int = Gravity.START): TextView =
    TextView(this).apply {
        this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(color); typeface = tf
        this.gravity = gravity; includeFontPadding = false
    }

fun Context.round(color: Int, radiusDp: Float): GradientDrawable =
    GradientDrawable().apply { setColor(color); cornerRadius = dp(radiusDp).toFloat() }

fun Context.ripple(bg: GradientDrawable): RippleDrawable =
    RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), bg, null)

/** Pill-shaped button. */
fun Context.button(text: String, bg: Int, fg: Int, sp: Float = 16f, h: Int = 56, radius: Float = 28f): TextView =
    tv(text, sp, fg, HEAD, Gravity.CENTER).apply {
        letterSpacing = 0.08f
        background = ripple(round(bg, radius))
        isClickable = true
        minHeight = dp(h)
    }

fun lp(w: Int, h: Int, weight: Float = 0f): LinearLayout.LayoutParams = LinearLayout.LayoutParams(w, h, weight)
fun View.margins(l: Int = 0, t: Int = 0, r: Int = 0, b: Int = 0) {
    val apply = {
        val p = layoutParams
        if (p is ViewGroup.MarginLayoutParams) { p.setMargins(l, t, r, b); layoutParams = p }
    }
    if (layoutParams != null && parent != null) apply()
    else addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { apply(); v.removeOnAttachStateChangeListener(this) }
        override fun onViewDetachedFromWindow(v: View) {}
    })
}
fun fl(w: Int, h: Int, g: Int = Gravity.NO_GRAVITY): FrameLayout.LayoutParams = FrameLayout.LayoutParams(w, h, g)
const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun fmtTime(sec: Int): String { val s = if (sec < 0) 0 else sec; return "%02d:%02d".format(s / 60, s % 60) }
