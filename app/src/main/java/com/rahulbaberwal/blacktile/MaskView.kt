package com.rahulbaberwal.blacktile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * Custom view representing an individual privacy tile.
 * Supports multiple 100% opaque overlay styles and textures:
 * - Pure Pitch Black (OLED)
 * - Matte Obsidian
 * - Midnight Navy
 * - Dark Slate
 * - Carbon Fiber Weave Texture
 * - Security Dot Mesh Texture
 * When locked: ALWAYS 100% FULLY DARK & OPAQUE (alpha 255), completely concealing underlying content.
 * When unlocked (editing): draws with configured editing opacity, dashed border, and resize handles.
 */
class MaskView(
    context: Context,
    val tile: Tile,
    var opacity: Int = 100,
    var isLocked: Boolean = false,
    var overlayStyle: String = MaskPrefs.STYLE_PURE_BLACK,
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

    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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

    private var carbonShader: BitmapShader? = null
    private var dotMatrixShader: BitmapShader? = null

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

    private fun getCarbonShader(): BitmapShader {
        carbonShader?.let { return it }
        val size = (16 * density).toInt().coerceAtLeast(8)
        val half = size / 2
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint()

        // Checker weave blocks
        p.color = Color.rgb(28, 29, 36)
        c.drawRect(0f, 0f, half.toFloat(), half.toFloat(), p)
        c.drawRect(half.toFloat(), half.toFloat(), size.toFloat(), size.toFloat(), p)

        p.color = Color.rgb(16, 17, 22)
        c.drawRect(half.toFloat(), 0f, size.toFloat(), half.toFloat(), p)
        c.drawRect(0f, half.toFloat(), half.toFloat(), size.toFloat(), p)

        // Subtle diagonal weave highlight lines
        p.color = Color.rgb(38, 40, 50)
        p.strokeWidth = 1f * density
        c.drawLine(0f, 0f, half.toFloat(), half.toFloat(), p)
        c.drawLine(half.toFloat(), half.toFloat(), size.toFloat(), size.toFloat(), p)

        val shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        carbonShader = shader
        return shader
    }

    private fun getDotMatrixShader(): BitmapShader {
        dotMatrixShader?.let { return it }
        val size = (12 * density).toInt().coerceAtLeast(8)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.rgb(8, 8, 10)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)

        p.color = Color.rgb(34, 36, 48)
        c.drawCircle(size / 2f, size / 2f, 1.5f * density, p)

        val shader = BitmapShader(bmp, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        dotMatrixShader = shader
        return shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        // 1. Determine effective alpha:
        // When locked: ALWAYS 100% FULLY DARK (255) as explicitly requested!
        // When unlocked (editing): use configured editing opacity
        val effectiveAlpha = if (isLocked) 255 else ((opacity.coerceIn(20, 100) / 100f) * 255).toInt()

        // 2. Draw 100% solid base and selected texture (completely opaque when locked)
        drawOverlayStyle(canvas, w, h, effectiveAlpha)

        // 3. In locked mode, hide borders, handles, and close icons
        if (isLocked) {
            return
        }

        // 4. Draw dashed white border
        val inset = borderPaint.strokeWidth / 2f
        canvas.drawRect(inset, inset, w - inset, h - inset, borderPaint)

        // 5. Draw Close button (✕) in top-right corner
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

        // 6. Draw triangular resize handle in bottom-right corner
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

    private fun drawOverlayStyle(canvas: Canvas, w: Float, h: Float, alpha: Int) {
        val isFullDark = isLocked || alpha >= 255

        when (overlayStyle) {
            MaskPrefs.STYLE_PURE_BLACK -> {
                fillPaint.color = if (isFullDark) Color.BLACK else Color.argb(alpha, 0, 0, 0)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
            }
            MaskPrefs.STYLE_MATTE_OBSIDIAN -> {
                val c = Color.rgb(18, 18, 20)
                fillPaint.color = if (isFullDark) c else Color.argb(alpha, 18, 18, 20)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
            }
            MaskPrefs.STYLE_DARK_NAVY -> {
                val c = Color.rgb(11, 16, 38)
                fillPaint.color = if (isFullDark) c else Color.argb(alpha, 11, 16, 38)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
            }
            MaskPrefs.STYLE_DARK_SLATE -> {
                val c = Color.rgb(22, 27, 34)
                fillPaint.color = if (isFullDark) c else Color.argb(alpha, 22, 27, 34)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
            }
            MaskPrefs.STYLE_CARBON_WEAVE -> {
                // 1. Draw solid dark base (guarantees 100% opacity, zero transparency underneath)
                fillPaint.color = if (isFullDark) Color.rgb(14, 14, 16) else Color.argb(alpha, 14, 14, 16)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
                // 2. Draw repeating carbon weave pattern
                texturePaint.shader = getCarbonShader()
                texturePaint.alpha = if (isFullDark) 255 else alpha
                canvas.drawRect(0f, 0f, w, h, texturePaint)
            }
            MaskPrefs.STYLE_DOT_MATRIX -> {
                // 1. Draw solid dark base
                fillPaint.color = if (isFullDark) Color.BLACK else Color.argb(alpha, 0, 0, 0)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
                // 2. Draw repeating dot mesh pattern
                texturePaint.shader = getDotMatrixShader()
                texturePaint.alpha = if (isFullDark) 255 else alpha
                canvas.drawRect(0f, 0f, w, h, texturePaint)
            }
            else -> {
                fillPaint.color = if (isFullDark) Color.BLACK else Color.argb(alpha, 0, 0, 0)
                canvas.drawRect(0f, 0f, w, h, fillPaint)
            }
        }
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
