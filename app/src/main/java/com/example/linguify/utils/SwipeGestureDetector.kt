package com.example.linguify.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.absoluteValue

class SwipeGestureDetector(
    private val view: View,
    private val onSwipe: (SwipeDirection) -> Unit
) : View.OnTouchListener {

    enum class SwipeDirection {
        LEFT, RIGHT
    }

    private var initialX = 0f
    private var initialY = 0f
    private var dX = 0f
    private var velocityTracker: VelocityTracker? = null
    private val SWIPE_THRESHOLD = 100f
    private val SWIPE_VELOCITY_THRESHOLD = 800f
    private val SWIPE_ESCAPE_VELOCITY = 1500f
    private val CLICK_MOVEMENT_THRESHOLD = 20f
    private val CLICK_DURATION_THRESHOLD = 200L
    private var isSwiping = false
    private var startTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val parentWidth = (v.parent as ViewGroup).width.toFloat()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = v.x
                initialY = v.y
                dX = v.x - event.rawX
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                isSwiping = false
                startTime = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)

                val newX = event.rawX + dX
                val distance = abs(newX - initialX)

                if (distance > CLICK_MOVEMENT_THRESHOLD) {
                    isSwiping = true

                    v.x = newX

                    val rotation = (newX - initialX) / parentWidth * 15
                    v.rotation = rotation

                    val alpha = 1.0f - (abs(newX - initialX) / parentWidth * 0.5f)
                    v.alpha = alpha.coerceIn(0.5f, 1.0f)
                }

                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.computeCurrentVelocity(1000)
                val velocityX = velocityTracker?.xVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                val distance = v.x - initialX
                val elapsedTime = System.currentTimeMillis() - startTime

                if (!isSwiping && distance.absoluteValue < CLICK_MOVEMENT_THRESHOLD && elapsedTime < CLICK_DURATION_THRESHOLD) {
                    resetCardPosition(v)
                    v.performClick()
                    return false
                }

                if (isSwiping) {
                    if (abs(distance) > SWIPE_THRESHOLD || abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        val swipedRight = distance > 0

                        val targetX = if (swipedRight) parentWidth * 1.5f else -parentWidth * 1.5f
                        val duration = if (abs(velocityX) > SWIPE_ESCAPE_VELOCITY) 150L else 300L

                        val swipeAnimation = ObjectAnimator.ofFloat(v, "x", v.x, targetX)
                        swipeAnimation.duration = duration
                        swipeAnimation.interpolator = AccelerateInterpolator()

                        swipeAnimation.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                val direction = if (swipedRight) SwipeDirection.RIGHT else SwipeDirection.LEFT
                                onSwipe(direction)

                                v.x = initialX
                                v.rotation = 0f
                                v.alpha = 1f
                            }
                        })

                        swipeAnimation.start()
                    } else {
                        resetCardPosition(v)
                    }
                    return true
                } else {
                    resetCardPosition(v)
                    return false
                }
            }

            else -> return false
        }
    }

    private fun resetCardPosition(v: View) {
        val positionAnimation = ObjectAnimator.ofFloat(v, "x", v.x, initialX)
        val rotationAnimation = ObjectAnimator.ofFloat(v, "rotation", v.rotation, 0f)
        val alphaAnimation = ObjectAnimator.ofFloat(v, "alpha", v.alpha, 1.0f)

        positionAnimation.duration = 200
        rotationAnimation.duration = 200
        alphaAnimation.duration = 200

        positionAnimation.interpolator = DecelerateInterpolator()
        rotationAnimation.interpolator = DecelerateInterpolator()

        positionAnimation.start()
        rotationAnimation.start()
        alphaAnimation.start()
    }

    fun animateSwipe(direction: SwipeDirection) {
        val parentWidth = (view.parent as ViewGroup).width.toFloat()
        val targetX = if (direction == SwipeDirection.RIGHT) parentWidth * 1.5f else -parentWidth * 1.5f

        val swipeAnimation = ObjectAnimator.ofFloat(view, "x", view.x, targetX)
        val rotationAnimation = ObjectAnimator.ofFloat(view, "rotation", 0f, if (direction == SwipeDirection.RIGHT) 15f else -15f)
        val fadeAnimation = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f)

        swipeAnimation.duration = 300
        rotationAnimation.duration = 300
        fadeAnimation.duration = 150

        swipeAnimation.interpolator = AccelerateInterpolator()
        rotationAnimation.interpolator = AccelerateInterpolator()

        swipeAnimation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onSwipe(direction)

                view.x = initialX
                view.rotation = 0f
                view.alpha = 1f
            }
        })

        swipeAnimation.start()
        rotationAnimation.start()
        fadeAnimation.start()
    }

    fun isCardSwiping(): Boolean {
        return isSwiping
    }
}