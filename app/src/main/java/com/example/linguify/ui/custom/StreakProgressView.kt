package com.example.linguify.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.linguify.R
import com.example.linguify.databinding.StreakProgressViewBinding
import java.nio.file.Files.readAttributes

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

    init {
        circleRadius = 16.dpToPx().toFloat()
        activeCircleColor = ContextCompat.getColor(context, R.color.primary_color)
        inactiveCircleColor = ContextCompat.getColor(context, R.color.border_light)
        connectorLineColor = ContextCompat.getColor(context, R.color.border_light)
        connectorLineWidth = 2.dpToPx().toFloat()
        dayLabelTextSize = 10.sp
        dayLabelTextColor = ContextCompat.getColor(context, R.color.secondary_text)
        streakTextSize = 14.sp
        streakTextColor = ContextCompat.getColor(context, R.color.primary_text)
        streakBackgroundColor = ContextCompat.getColor(context, R.color.white)
        streakBorderColor = ContextCompat.getColor(context, R.color.border_light)
        streakCornerRadius = 12.dpToPx().toFloat()
        animationDuration = 800L
        animationScale = 0.3f
        logoColor = Color.WHITE
        logoSize = 8.dpToPx().toFloat()

        attrs?.let { readAttributes(it, defStyleAttr) }

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

    override fun onDraw(canvas: Canvas?) {
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
        return height - 45.dpToPx().toFloat()
    }

    private fun getCirclePositions(): List<Float> {
        val padding = 24.dpToPx().toFloat()
        val availableWidth = width - 2 * padding
        val spacing = availableWidth / 6f

        return (0 until 7).map { i ->
            padding + i * spacing
        }
    }

    private fun drawBackground(canvas: Canvas) { }
    private fun drawStreakText(canvas: Canvas) { }
    private fun drawConnectorLines(canvas: Canvas) { }
    private fun drawCircles(canvas: Canvas) { }
    private fun drawLogos(canvas: Canvas) { }
    private fun drawDayLabels(canvas: Canvas) { }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
    fun updateStreakData(weeklyStreak: List<Boolean>, currentStreak: Int) {
        updateStreakCount(currentStreak)
        updateDayIndicators(weeklyStreak)
    }

    private fun updateStreakCount(streak: Int) {
        binding.tvStreakCount.text = when {
            streak == 0 -> "Streak başlat!"
            streak == 1 -> "1 gün streak"
            else -> "$streak gün streak"
        }
    }

    private fun updateDayIndicators(weeklyStreak: List<Boolean>) {
        if (weeklyStreak.size != dayIndicators.size) return

        weeklyStreak.forEachIndexed { index, isActive ->
            val indicator = dayIndicators[index]
            val wasActive = if (index < previousWeeklyStreak.size) previousWeeklyStreak[index] else false

            val shouldAnimate = isActive && !wasActive

            if (shouldAnimate) {
                updateIndicatorState(indicator, isActive)
            } else {
                updateIndicatorStateWithoutAnimation(indicator, isActive)
            }
        }

        previousWeeklyStreak = weeklyStreak.toList()
    }

}