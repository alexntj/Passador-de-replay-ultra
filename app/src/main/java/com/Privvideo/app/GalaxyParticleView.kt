package com.Privvideo.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/** Fundo decorativo monocromático: partículas leves caem sobre o fundo preto. */
class GalaxyParticleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class Particle(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speed: Float,
        val drift: Float,
        val baseAlpha: Int,
        val phase: Float,
        val twinkleSpeed: Float
    )

    private val density = resources.displayMetrics.density
    private val random = Random(System.nanoTime())
    private val particles = ArrayList<Particle>(96)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var animationRunning = false
    private var lastFrameTimeNs = 0L

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return

        particles.clear()
        val count = ((width / density) * 0.20f).toInt().coerceIn(48, 110)
        repeat(count) {
            particles += newParticle(width.toFloat(), random.nextFloat() * height)
        }
        lastFrameTimeNs = 0L
    }

    private fun newParticle(width: Float, y: Float): Particle = Particle(
        x = random.nextFloat() * width,
        y = y,
        radius = (0.55f + random.nextFloat() * 1.65f) * density,
        speed = (16f + random.nextFloat() * 44f) * density,
        drift = (random.nextFloat() - 0.5f) * 6f * density,
        baseAlpha = 24 + random.nextInt(88),
        phase = random.nextFloat() * 6.28318f,
        twinkleSpeed = 1.4f + random.nextFloat() * 3.4f
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.isEmpty()) return

        val now = System.nanoTime()
        val deltaSeconds = if (lastFrameTimeNs == 0L) {
            0f
        } else {
            ((now - lastFrameTimeNs) / 1_000_000_000.0).coerceIn(0.0, 0.04).toFloat()
        }
        lastFrameTimeNs = now
        val timeSeconds = now / 1_000_000_000.0
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        for (particle in particles) {
            particle.y += particle.speed * deltaSeconds
            particle.x += particle.drift * deltaSeconds

            if (particle.y - particle.radius > viewHeight) {
                particle.y = -particle.radius
                particle.x = random.nextFloat() * viewWidth
            }
            if (particle.x < -particle.radius) particle.x = viewWidth + particle.radius
            if (particle.x > viewWidth + particle.radius) particle.x = -particle.radius

            val shimmer = ((sin(timeSeconds * particle.twinkleSpeed + particle.phase) + 1.0) * 0.5).toFloat()
            paint.alpha = (particle.baseAlpha * (0.58f + 0.42f * shimmer)).toInt().coerceIn(12, 140)
            canvas.drawCircle(particle.x, particle.y, particle.radius, paint)
        }

        if (animationRunning && isAttachedToWindow && visibility == VISIBLE) {
            postInvalidateOnAnimation()
        }
    }

    fun resumeAnimation() {
        if (animationRunning) return
        animationRunning = true
        lastFrameTimeNs = 0L
        postInvalidateOnAnimation()
    }

    fun pauseAnimation() {
        animationRunning = false
        lastFrameTimeNs = 0L
    }

    override fun onDetachedFromWindow() {
        pauseAnimation()
        super.onDetachedFromWindow()
    }
}
