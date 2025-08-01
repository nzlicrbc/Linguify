package com.example.linguify.ui.custom

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.linguify.R
import android.graphics.Path
import android.view.animation.BounceInterpolator

class StreakProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var circleRadius: Float
    private var activeCircleColor: Int
    private var inactiveCircleColor: Int
    private var connectorLineColor: Int
    private var connectorLineWidth: Float
    private var dayLabelTextSize: Float
    private var dayLabelTextColor: Int
    private var streakTextSize: Float
    private var streakTextColor: Int
    private var streakBackgroundColor: Int
    private var streakBorderColor: Int
    private var streakCornerRadius: Float
    private var animationDuration: Long
    private var animationScale: Float
    private var logoColor: Int
    private var logoSize: Float

    private val activePaint = Paint()
    private val inactivePaint = Paint()
    private val textPaint = Paint()
    private val connectorPaint = Paint()
    private val backgroundPaint = Paint()
    private val borderPaint = Paint()

    private var currentStreak = 0
    private var weeklyStreak = List(7) { false }
    private val dayLabels = listOf("P", "S", "C", "P", "C", "C", "P")

    private var animatingIndex = -1
    private var animationProgress = 0f

    private var previousWeeklyStreak = List(7) { false }

    init {
        circleRadius = 16.dpToPx().toFloat()
        activeCircleColor = ContextCompat.getColor(context, R.color.primary_color)
        inactiveCircleColor = ContextCompat.getColor(context, R.color.border_light)
        connectorLineColor = ContextCompat.getColor(context, R.color.border_light)
        connectorLineWidth = 2.dpToPx().toFloat()
        dayLabelTextSize = 10.spToPx()
        dayLabelTextColor = ContextCompat.getColor(context, R.color.secondary_text)
        streakTextSize = 14.spToPx()
        streakTextColor = ContextCompat.getColor(context, R.color.primary_text)
        streakBackgroundColor = ContextCompat.getColor(context, R.color.white)
        streakBorderColor = ContextCompat.getColor(context, R.color.border_light)
        streakCornerRadius = 12.dpToPx().toFloat()
        animationDuration = 800L
        animationScale = 0.3f
        logoColor = Color.WHITE
        logoSize = 8.dpToPx().toFloat()

        attrs?.let { readAttributes(it, defStyleAttr) }

        initializePaints()
    }

    private fun initializePaints() {
        activePaint.apply {
            color = activeCircleColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        inactivePaint.apply {
            color = inactiveCircleColor
            style = Paint.Style.STROKE
            strokeWidth = 2.dpToPx().toFloat()
            isAntiAlias = true
        }

        textPaint.apply {
            color = dayLabelTextColor
            textSize = dayLabelTextSize
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        connectorPaint.apply {
            color = connectorLineColor
            strokeWidth = connectorLineWidth
            isAntiAlias = true
        }

        backgroundPaint.apply {
            color = streakBackgroundColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        borderPaint.apply {
            color = streakBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 1.dpToPx().toFloat()
            isAntiAlias = true
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 320.dpToPx()
        val desiredHeight = 120.dpToPx()

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> minOf(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> minOf(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    private fun readAttributes(attrs: AttributeSet, defStyleAttr: Int) {
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.StreakProgressView,
            defStyleAttr,
            0
        )

        try {
            circleRadius = typedArray.getDimension(
                R.styleable.StreakProgressView_circleRadius,
                circleRadius
            )

            activeCircleColor = typedArray.getColor(
                R.styleable.StreakProgressView_activeCircleColor,
                activeCircleColor
            )

            inactiveCircleColor = typedArray.getColor(
                R.styleable.StreakProgressView_inactiveCircleColor,
                inactiveCircleColor
            )

            connectorLineColor = typedArray.getColor(
                R.styleable.StreakProgressView_connectorLineColor,
                connectorLineColor
            )

            connectorLineWidth = typedArray.getDimension(
                R.styleable.StreakProgressView_connectorLineWidth,
                connectorLineWidth
            )

            dayLabelTextSize = typedArray.getDimension(
                R.styleable.StreakProgressView_dayLabelTextSize,
                dayLabelTextSize
            )

            dayLabelTextColor = typedArray.getColor(
                R.styleable.StreakProgressView_dayLabelTextColor,
                dayLabelTextColor
            )

            streakTextSize = typedArray.getDimension(
                R.styleable.StreakProgressView_streakTextSize,
                streakTextSize
            )

            streakTextColor = typedArray.getColor(
                R.styleable.StreakProgressView_streakTextColor,
                streakTextColor
            )

            streakBackgroundColor = typedArray.getColor(
                R.styleable.StreakProgressView_streakBackgroundColor,
                streakBackgroundColor
            )

            streakBorderColor = typedArray.getColor(
                R.styleable.StreakProgressView_streakBorderColor,
                streakBorderColor
            )

            streakCornerRadius = typedArray.getDimension(
                R.styleable.StreakProgressView_streakCornerRadius,
                streakCornerRadius
            )

            animationDuration = typedArray.getInteger(
                R.styleable.StreakProgressView_animationDuration,
                animationDuration.toInt()
            ).toLong()

            animationScale = typedArray.getFloat(
                R.styleable.StreakProgressView_animationScale,
                animationScale
            )

            logoColor = typedArray.getColor(
                R.styleable.StreakProgressView_logoColor,
                logoColor
            )

            logoSize = typedArray.getDimension(
                R.styleable.StreakProgressView_logoSize,
                logoSize
            )

        } finally {
            typedArray.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas ?: return

        drawBackground(canvas)
        drawStreakText(canvas)
        drawConnectorLines(canvas)
        drawCircles(canvas)
        drawLogos(canvas)
        drawDayLabels(canvas)
    }

    private fun getCircleY(): Float {
        return height - 30.dpToPx().toFloat()
    }

    private fun getCirclePositions(): List<Float> {
        val padding = 24.dpToPx().toFloat()
        val availableWidth = width - 2 * padding
        val spacing = availableWidth / 6f

        return (0 until 7).map { i ->
            padding + i * spacing
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

        canvas.drawRoundRect(rect, streakCornerRadius, streakCornerRadius, backgroundPaint)

        canvas.drawRoundRect(rect, streakCornerRadius, streakCornerRadius, borderPaint)
    }
    private fun drawStreakText(canvas: Canvas) {
        val fireIconX = 40.dpToPx().toFloat()
        val textY = 30.dpToPx().toFloat()

        drawFireIcon(canvas, fireIconX, textY)

        val streakText = when {
            currentStreak == 0 -> "Streak başlat!"
            currentStreak == 1 -> "1 gün streak"
            else -> "$currentStreak gün streak"
        }

        val streakTextPaint = Paint().apply {
            color = streakTextColor
            textSize = streakTextSize
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            isFakeBoldText = true
        }

        canvas.drawText(streakText, fireIconX + 32.dpToPx(), textY + 5.dpToPx(), streakTextPaint)
    }

    private fun drawFireIcon(canvas: Canvas, x: Float, y: Float) {
        val fireDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fire)
        fireDrawable?.let { drawable ->
            val size = 16.dpToPx()
            drawable.setBounds(
                (x - size/2).toInt(),
                (y - size/2).toInt(),
                (x + size/2).toInt(),
                (y + size/2).toInt()
            )
            drawable.draw(canvas)
        }
    }
    private fun drawConnectorLines(canvas: Canvas) {
        val circleY = getCircleY()
        val positions = getCirclePositions()

        for (i in 0 until positions.size - 1) {
            val startX = positions[i] + circleRadius
            val endX = positions[i + 1] - circleRadius

            canvas.drawLine(startX, circleY, endX, circleY, connectorPaint)
        }
    }
    private fun drawCircles(canvas: Canvas) {
        val circleY = getCircleY()
        val positions = getCirclePositions()

        positions.forEachIndexed { index, x ->
            val isActive = if (index < weeklyStreak.size) weeklyStreak[index] else false

            val currentRadius = if (index == animatingIndex) {
                val scale = 1f + animationProgress
                circleRadius * scale
            } else {
                circleRadius
            }

            if (isActive) {
                val innerPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                canvas.drawCircle(x, circleY, currentRadius, innerPaint)

                val activeStrokePaint = Paint().apply {
                    color = activeCircleColor
                    style = Paint.Style.STROKE
                    strokeWidth = 3.dpToPx().toFloat()
                    isAntiAlias = true
                }
                canvas.drawCircle(x, circleY, currentRadius, activeStrokePaint)

            } else {
                val inactiveFillPaint = Paint().apply {
                    color = ContextCompat.getColor(context, R.color.background_light)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(x, circleY, currentRadius, inactiveFillPaint)
                canvas.drawCircle(x, circleY, currentRadius, inactivePaint)
            }
        }
    }
    private fun drawLogos(canvas: Canvas) {
        val circleY = getCircleY()
        val positions = getCirclePositions()

        positions.forEachIndexed { index, x ->
            val isActive = if (index < weeklyStreak.size) weeklyStreak[index] else false

            if (isActive) {
                drawHeartIcon(canvas, x, circleY)
            }
        }
    }

    private fun drawHeartIcon(canvas: Canvas, centerX: Float, centerY: Float) {
        val heartDrawable = ContextCompat.getDrawable(context, R.drawable.ic_favorite)
        heartDrawable?.let { drawable ->
            val size = logoSize.toInt()
            drawable.setBounds(
                (centerX - size/2).toInt(),
                (centerY - size/2).toInt(),
                (centerX + size/2).toInt(),
                (centerY + size/2).toInt()
            )
            drawable.draw(canvas)
        }
    }

    private fun drawDayLabels(canvas: Canvas) {
        val circleY = getCircleY()
        val positions = getCirclePositions()
        val labelY = circleY - circleRadius - 12.dpToPx().toFloat()

        positions.forEachIndexed { index, x ->
            val label = dayLabels[index]
            canvas.drawText(label, x, labelY, textPaint)
        }
    }

    private fun animateNewStreak(dayIndex: Int) {
        if (dayIndex == -1) return

        animatingIndex = dayIndex

        ValueAnimator.ofFloat(1f, 0.7f).apply {
            duration = 500

            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float - 1f
                invalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    startStage2Animation()
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetAnimation()
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })

            start()
        }
    }

    private fun startStage2Animation() {
        ValueAnimator.ofFloat(0.7f, 1.2f).apply {
            duration = 500

            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float - 1f
                invalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    startStage3Animation()
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetAnimation()
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })

            start()
        }
    }

    private fun startStage3Animation() {
        ValueAnimator.ofFloat(1.2f, 1f).apply {
            duration = 100

            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float - 1f
                invalidate()
            }

            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    resetAnimation()
                }

                override fun onAnimationCancel(animation: Animator) {
                    resetAnimation()
                }

                override fun onAnimationRepeat(animation: Animator) {}
            })

            start()
        }
    }

    private fun resetAnimation() {
        animatingIndex = -1
        animationProgress = 0f
        invalidate()
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun Int.spToPx(): Float {
        return this * context.resources.displayMetrics.scaledDensity
    }
    fun updateStreakData(weeklyStreak: List<Boolean>, currentStreak: Int) {
        val oldStreak = this.weeklyStreak
        this.weeklyStreak = weeklyStreak
        this.currentStreak = currentStreak

        val newActiveIndex = findNewActiveIndex(oldStreak, weeklyStreak)
        if (newActiveIndex != -1) {
            animateNewStreak(newActiveIndex)
        } else {
            invalidate()
        }

        previousWeeklyStreak = weeklyStreak.toList()
    }

    private fun findNewActiveIndex(previous: List<Boolean>, current: List<Boolean>): Int {
        for (i in current.indices) {
            val wasActive = previous.getOrNull(i) ?: false
            val isActive = current[i]
            if (isActive && !wasActive) {
                return i
            }
        }
        return -1
    }



}