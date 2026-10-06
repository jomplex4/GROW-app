package com.digitalminds.grow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.util.concurrent.Executors

/** Per-exercise sprite description (see assets/anim/meta.json). */
class SpriteMeta(
    val n: Int, val w: Int, val h: Int,
    val alt: Boolean,      // second half of the loop uses mirrored frames (front-view alternating moves)
    val step: Boolean,     // key poses with a short cross-fade instead of continuous morphing (kept as a fallback)
    val cyc: Boolean,      // fast cyclic move: no pause at the ends of the loop
    val bust: Boolean,     // head-and-shoulders close-up, anchored to the bottom edge
    val ground: Boolean,   // draw a contact shadow
    val loop: Float = 0f,  // seconds for one side (0 = use the exercise default)
    val cycle: Boolean = false,      // frames form a forward loop (gait) instead of start -> end -> start
    val ri: Float = 0.5f, val ho: Float = 0f, val fa: Float = 0.5f,   // rise / hold / fall fractions of the loop
    val dip: Boolean = false,        // quick fade when switching to the mirrored side
    val breath: Boolean = false      // subtle breathing on static holds
)

/** Pure sequencing helpers (no Android types) so they can be unit tested on the JVM. */
object Seq {
    /** frame index of every beat in one loop: ping-pong over the frames, doubled when alternating */
    fun beats(n: Int, alt: Boolean): IntArray {
        val one = ArrayList<Int>()
        for (i in 0 until n) one.add(i)
        for (i in n - 2 downTo 1) one.add(i)
        if (alt) { val l = one.size; for (i in 0 until l) one.add(one[i]) }
        return one.toIntArray()
    }
    fun flips(n: Int, alt: Boolean): BooleanArray {
        val l = beats(n, alt).size
        return BooleanArray(l) { alt && it >= l / 2 }
    }
    /** eased 0..1..0 travel across the frames of a smooth exercise, with a short hold at both ends */
    fun smoothPos(phase: Float): Float {
        val p0 = 0.5f - 0.5f * kotlin.math.cos(2.0 * Math.PI * phase).toFloat()
        val d = 0.10f
        val x = ((p0 - d) / (1f - 2f * d)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }
    /** plain 0..1..0 ping-pong travel, no pause at the ends */
    fun cosPos(phase: Float): Float = (0.5f - 0.5f * kotlin.math.cos(2.0 * Math.PI * phase).toFloat()).coerceIn(0f, 1f)
    /** travel 0..1..0 for one side: rise, hold at the end pose, fall, then rest at the start pose */
    fun profile(phase: Float, ri: Float, ho: Float, fa: Float): Float {
        val p = ((phase % 1f) + 1f) % 1f
        return when {
            p < ri -> if (ri > 0f) smooth(p / ri) else 1f
            p < ri + ho -> 1f
            p < ri + ho + fa -> if (fa > 0f) 1f - smooth((p - ri - ho) / fa) else 0f
            else -> 0f
        }
    }
    fun smooth(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }
}

object Sprites {
    private val meta = HashMap<String, SpriteMeta>()
    private val bad = HashSet<String>()
    private val stills = HashMap<String, Bitmap>()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** LRU cache of decoded frame sets; sample = 1 for full size, 2 for the list thumbnails */
    private class FrameCache(val max: Int, val sample: Int) {
        val cache = LinkedHashMap<String, Array<Bitmap>>(12, 0.75f, true)
        val waiting = HashMap<String, MutableList<() -> Unit>>()
    }
    private val full = FrameCache(4, 1)
    private val half = FrameCache(10, 2)

    fun init(ctx: Context) {
        if (meta.isNotEmpty()) return
        try {
            val o = JSONObject(ctx.assets.open("anim/meta.json").bufferedReader().use { it.readText() })
            val keys = o.keys()
            while (keys.hasNext()) {
                val id = keys.next(); val m = o.getJSONObject(id)
                meta[id] = SpriteMeta(m.getInt("n"), m.getInt("w"), m.getInt("h"), m.optInt("alt") >= 1,
                    m.optInt("step") == 1, m.optInt("cyc") == 1, m.optInt("bust") == 1, m.optInt("ground", 1) == 1,
                    m.optDouble("loop", 0.0).toFloat(), m.optString("mode", "pp") == "cy",
                    m.optDouble("ri", 0.5).toFloat(), m.optDouble("ho", 0.0).toFloat(), m.optDouble("fa", 0.5).toFloat(),
                    m.optInt("alt") == 2, m.optInt("br") == 1)
            }
        } catch (e: Exception) { /* no sprites: the app falls back to the code-drawn figure */ }
    }

    fun meta(id: String): SpriteMeta? = meta[id]
    fun failed(id: String): Boolean = synchronized(bad) { bad.contains(id) }
    fun frames(id: String): Array<Bitmap>? = synchronized(full) { full.cache[id] }
    fun halfFrames(id: String): Array<Bitmap>? = synchronized(half) { half.cache[id] }

    fun load(ctx: Context, id: String, done: () -> Unit) = loadInto(full, ctx, id, done)
    fun loadHalf(ctx: Context, id: String, done: () -> Unit) = loadInto(half, ctx, id, done)

    private fun loadInto(fc: FrameCache, ctx: Context, id: String, done: () -> Unit) {
        val m = meta[id] ?: return
        synchronized(fc) {
            if (fc.cache.containsKey(id)) { main.post(done); return }
            val l = fc.waiting[id]
            if (l != null) { l.add(done); return }
            fc.waiting[id] = mutableListOf(done)
        }
        val app = ctx.applicationContext
        io.execute {
            val arr = ArrayList<Bitmap>()
            var ok = true
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inSampleSize = fc.sample }
            for (k in 0 until m.n) {
                try {
                    val b = app.assets.open("anim/$id/%02d.webp".format(k)).use { BitmapFactory.decodeStream(it, null, opts) }
                    if (b == null) { ok = false; break }
                    arr.add(b)
                } catch (e: Exception) { ok = false; break }
            }
            val cbs: List<() -> Unit>
            synchronized(fc) {
                if (ok) {
                    fc.cache[id] = arr.toTypedArray()
                    while (fc.cache.size > fc.max) { val first = fc.cache.keys.iterator().next(); fc.cache.remove(first) }
                } else synchronized(bad) { bad.add(id) }
                cbs = fc.waiting.remove(id) ?: emptyList()
            }
            main.post { for (c in cbs) c() }
        }
    }

    /** free decoded frames when the system is short of memory (they reload on demand) */
    fun trim() {
        synchronized(full) { full.cache.clear() }
        synchronized(half) { half.cache.clear() }
        synchronized(stills) { stills.clear() }
    }

    /** first frame at half size, used as the instant placeholder of list rows (decoded once) */
    fun still(ctx: Context, id: String): Bitmap? {
        synchronized(stills) { stills[id]?.let { return it } }
        return try {
            val o = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inSampleSize = 2 }
            val b = ctx.assets.open("anim/$id/00.webp").use { BitmapFactory.decodeStream(it, null, o) }
            if (b != null) synchronized(stills) { stills[id] = b }
            b
        } catch (e: Exception) { null }
    }
}
