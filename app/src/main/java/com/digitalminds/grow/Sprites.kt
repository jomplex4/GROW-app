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
    val step: Boolean,     // key poses with a short cross-fade instead of continuous morphing
    val bust: Boolean,     // head-and-shoulders close-up, anchored to the bottom edge
    val ground: Boolean    // draw a contact shadow
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
    fun smooth(x: Float): Float { val t = x.coerceIn(0f, 1f); return t * t * (3f - 2f * t) }
}

object Sprites {
    private val meta = HashMap<String, SpriteMeta>()
    private val cache = LinkedHashMap<String, Array<Bitmap>>(8, 0.75f, true)
    private val waiting = HashMap<String, MutableList<() -> Unit>>()
    private val bad = HashSet<String>()
    private val thumbs = HashMap<String, Bitmap>()
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private const val MAX = 4

    fun init(ctx: Context) {
        if (meta.isNotEmpty()) return
        try {
            val o = JSONObject(ctx.assets.open("anim/meta.json").bufferedReader().use { it.readText() })
            val keys = o.keys()
            while (keys.hasNext()) {
                val id = keys.next(); val m = o.getJSONObject(id)
                meta[id] = SpriteMeta(m.getInt("n"), m.getInt("w"), m.getInt("h"), m.optInt("alt") == 1,
                    m.optInt("step") == 1, m.optInt("bust") == 1, m.optInt("ground", 1) == 1)
            }
        } catch (e: Exception) { /* no sprites: the app falls back to the code-drawn figure */ }
    }

    fun meta(id: String): SpriteMeta? = meta[id]
    fun failed(id: String): Boolean = synchronized(cache) { bad.contains(id) }
    fun frames(id: String): Array<Bitmap>? = synchronized(cache) { cache[id] }

    fun load(ctx: Context, id: String, done: () -> Unit) {
        val m = meta[id] ?: return
        synchronized(cache) {
            if (cache.containsKey(id)) { main.post(done); return }
            val l = waiting[id]
            if (l != null) { l.add(done); return }
            waiting[id] = mutableListOf(done)
        }
        val app = ctx.applicationContext
        io.execute {
            val arr = ArrayList<Bitmap>()
            var ok = true
            val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
            for (k in 0 until m.n) {
                try {
                    val b = app.assets.open("anim/$id/%02d.webp".format(k)).use { BitmapFactory.decodeStream(it, null, opts) }
                    if (b == null) { ok = false; break }
                    arr.add(b)
                } catch (e: Exception) { ok = false; break }
            }
            val cbs: List<() -> Unit>
            synchronized(cache) {
                if (ok) {
                    cache[id] = arr.toTypedArray()
                    while (cache.size > MAX) { val first = cache.keys.iterator().next(); cache.remove(first) }
                } else bad.add(id)
                cbs = waiting.remove(id) ?: emptyList()
            }
            main.post { for (c in cbs) c() }
        }
    }

    fun thumb(ctx: Context, id: String): Bitmap? {
        synchronized(thumbs) { thumbs[id]?.let { return it } }
        return try {
            val b = ctx.assets.open("anim/$id/t.webp").use { BitmapFactory.decodeStream(it) }
            if (b != null) synchronized(thumbs) { thumbs[id] = b }
            b
        } catch (e: Exception) { null }
    }
}
