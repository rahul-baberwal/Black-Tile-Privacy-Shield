package com.example

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view representing an individual privacy tile.
 * Renders the black fill, dashed border, close button, and resize handle.
 * Handles touch gestures for moving and resizing the tile when unlocked.
 */
class MaskView(
    context: Context,
    val tile: Tile,
    var opacity: Int = 100,
    var isLocked: Boolean = false,
    private val listener: TileInteractionListener
) : View(context) {

    interface TileInteractionListener {
        fun onMoved(tile: Tile, newX: Int, newY: Int)
        fun onResized(tile: Tile, newW: Int, newH: Int)
        fun onDelete(tile: Tile)
        fun onEditFinished(tile: Tile)
    }

    private val density = resources.displayMetrics.density
    private val minSizePx = (80 * density).toInt()
    private val handleSizePx = (44 * density).toInt()

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f * density
        pathEffect = DashPathEffect(floatArrayOf(12f * density, 8f * density), 0f)
    }

    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 40, 40, 40)
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val trianglePath = Path()

    private enum class TouchMode { NONE, MOVING, RESIZING, CLOSE_TAP }
    private var touchMode = TouchMode.NONE

    private var startRawX = 0f
    private var startRawY = 0f
    private var initialX = 0
    private var initialY = 0
    private var initialW = 0
    private var initialH = 0

    init {
        isClickable = true
        isFocusable = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Draw black mask fill with configured opacity
        val alpha = ((opacity.coerceIn(20, 100) / 100f) * 255).toInt()
        fillPaint.color = Color.argb(alpha, 0, 0, 0)
        canvas.drawRect(0f, 0f, w, h, fillPaint)

        // 2. In locked mode, hide borders, handles, and icons
        if (isLocked) {
            return
        }

        // 3. Draw dashed white border
        val inset = borderPaint.strokeWidth / 2f
        canvas.drawRect(inset, inset, w - inset, h - inset, borderPaint)

        // 4. Draw Close button (✕) in top-right corner
        val closeBtnRadius = 14f * density
        val closeCenterX = w - (20f * density)
        val closeCenterY = 20f * density
        canvas.drawCircle(closeCenterX, closeCenterY, closeBtnRadius, handlePaint)

        val crossHalf = 6f * density
        canvas.drawLine(
            closeCenterX - crossHalf, closeCenterY - crossHalf,
            closeCenterX + crossHalf, closeCenterY + crossHalf,
            iconPaint
        )
        canvas.drawLine(
            closeCenterX + crossHalf, closeCenterY - crossHalf,
            closeCenterX - crossHalf, closeCenterY + crossHalf,
            iconPaint
        )

        // 5. Draw triangular resize handle in bottom-right corner
        val handleSide = 28f * density
        trianglePath.reset()
        trianglePath.moveTo(w, h - handleSide)
        trianglePath.lineTo(w, h)
        trianglePath.lineTo(w - handleSide, h)
        trianglePath.close()
        canvas.drawPath(trianglePath, handlePaint)

        // Diagonal grip lines inside resize handle
        val gripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 1.5f * density
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(w - (8f * density), h - (2f * density), w - (2f * density), h - (8f * density), gripPaint)
        canvas.drawLine(w - (14f * density), h - (2f * density), w - (2f * density), h - (14f * density), gripPaint)
        canvas.drawLine(w - (20f * density), h - (2f * density), w - (2f * density), h - (20f * density), gripPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            return false
        }

        val displayMetrics = resources.displayMetrics
        val screenW = displayMetrics.widthPixels
        val screenH = displayMetrics.heightPixels
        val minVisibleMargin = (40 * density).toInt()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startRawX = event.rawX
                startRawY = event.rawY
                initialX = tile.x
                initialY = tile.y
                initialW = tile.w
                initialH = tile.h

                val x = event.x
                val y = event.y

                // Check if touch is on top-right close button
                if (x >= width - handleSizePx && y <= handleSizePx) {
                    touchMode = TouchMode.CLOSE_TAP
                }
                // Check if touch is on bottom-right resize handle
                else if (x >= width - handleSizePx && y >= height - handleSizePx) {
                    touchMode = TouchMode.RESIZING
                }
                // Otherwise dragging moves the tile
                else {
                    touchMode = TouchMode.MOVING
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - startRawX
                val deltaY = event.rawY - startRawY

                when (touchMode) {
                    TouchMode.MOVING -> {
                        // Clamp position so the tile cannot leave the screen entirely
                        val newX = (initialX + deltaX).toInt().coerceIn(
                            -tile.w + minVisibleMargin,
                            screenW - minVisibleMargin
                        )
                        val newY = (initialY + deltaY).toInt().coerceIn(
                            0,
                            screenH - minVisibleMargin
                        )
                        tile.x = newX
                        tile.y = newY
                        listener.onMoved(tile, newX, newY)
                    }

                    TouchMode.RESIZING -> {
                        // Clamp resize so min size is 80dp and cannot exceed screen bounds
                        val newW = (initialW + deltaX).toInt()
                            .coerceAtLeast(minSizePx)
                            .coerceAtMost(max(minSizePx, screenW - tile.x))
                        val newH = (initialH + deltaY).toInt()
                            .coerceAtLeast(minSizePx)
                            .coerceAtMost(max(minSizePx, screenH - tile.y))
                        tile.w = newW
                        tile.h = newH
                        listener.onResized(tile, newW, newH)
                    }

                    TouchMode.CLOSE_TAP -> {
                        // If moved significantly while pressing close, cancel close tap
                        if (abs(deltaX) > 10 * density || abs(deltaY) > 10 * density) {
                            touchMode = TouchMode.NONE
                        }
                    }

                    TouchMode.NONE -> {}
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (touchMode == TouchMode.CLOSE_TAP) {
                    listener.onDelete(tile)
                } else if (touchMode == TouchMode.MOVING || touchMode == TouchMode.RESIZING) {
                    listener.onEditFinished(tile)
                }
                touchMode = TouchMode.NONE
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                touchMode = TouchMode.NONE
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
