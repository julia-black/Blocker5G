package com.juliablack.blocker5g

import android.content.Context
import android.graphics.Canvas
import android.graphics.Movie
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

@Suppress("DEPRECATION")
class RadarAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val movie = resources.openRawResource(R.mipmap.radar).use {
        Movie.decodeStream(it)
    }
    private val startTime = SystemClock.uptimeMillis()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val duration = movie.duration().takeIf { it > 0 } ?: DEFAULT_DURATION
        val elapsed = ((SystemClock.uptimeMillis() - startTime) % duration).toInt()
        movie.setTime(elapsed)

        val scale = min(
            width.toFloat() / movie.width().coerceAtLeast(1),
            height.toFloat() / movie.height().coerceAtLeast(1)
        )
        val scaledWidth = movie.width() * scale
        val scaledHeight = movie.height() * scale
        val left = (width - scaledWidth) / 2f
        val top = (height - scaledHeight) / 2f

        canvas.save()
        canvas.translate(left, top)
        canvas.scale(scale, scale)
        movie.draw(canvas, 0f, 0f)
        canvas.restore()

        postInvalidateOnAnimation()
    }

    companion object {
        private const val DEFAULT_DURATION = 1_000
    }
}
