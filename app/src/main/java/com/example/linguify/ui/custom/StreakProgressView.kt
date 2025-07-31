package com.example.linguify.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.linguify.R
import com.example.linguify.databinding.StreakProgressViewBinding

class StreakProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: StreakProgressViewBinding
    private val dayIndicators = mutableListOf<ImageView>()

    private val dayLabels = listOf("P", "S", "C", "P", "C", "C", "P")

    init {
        binding = StreakProgressViewBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        setupDayIndicators()
    }

    private fun setupDayIndicators() {
        dayIndicators.clear()
        binding.llDaysContainer.removeAllViews()

        for (i in 0 until 7) {
            val dayContainer = createDayContainer(i)
            binding.llDaysContainer.addView(dayContainer)

            if (i < 6) {
                val connector = createConnectorLine()
                binding.llDaysContainer.addView(connector)
            }
        }
    }

    private fun createDayContainer(dayIndex: Int): LinearLayout {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val dayLabel = TextView(context).apply {
            text = dayLabels[dayIndex]
            textSize = 10f
            setTextColor(ContextCompat.getColor(context, R.color.secondary_text))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4.dpToPx()
            }
        }

        val dayIndicator = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                32.dpToPx(),
                32.dpToPx()
            )
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.day_indicator_inactive)
        }

        dayIndicators.add(dayIndicator)

        container.addView(dayLabel)
        container.addView(dayIndicator)

        return container
    }

    private fun createConnectorLine(): View {
        return View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                16.dpToPx(),
                2.dpToPx()
            ).apply {
                topMargin = 16.dpToPx()
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.border_light))
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}