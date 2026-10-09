package com.docscanner.sdk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt

/**
 * Draws the white crop rectangle and dims everything outside it.
 * This is the native overlay that Flutter hosts via a PlatformView —
 * Flutter must not redraw this frame in Dart.
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99000000")
        style = Paint.Style.FILL
    }

    private val clearPath = Path()
    private val overlayRect = RectF()

    @ColorInt
    var borderColor: Int = Color.WHITE
        set(value) {
            field = value
            borderPaint.color = value
            invalidate()
        }

    var borderWidth: Float = 8f
        set(value) {
            field = value
            borderPaint.strokeWidth = value
            invalidate()
        }

    var cornerRadius: Float = 24f
        set(value) {
            field = value
            invalidate()
        }

    init {
        setWillNotDraw(false)
        context.theme.obtainStyledAttributes(attrs, R.styleable.OverlayView, 0, 0).apply {
            try {
                borderColor = getColor(R.styleable.OverlayView_overlayBorderColor, Color.WHITE)
                borderWidth = getDimension(R.styleable.OverlayView_overlayBorderWidth, 8f)
                cornerRadius = getDimension(R.styleable.OverlayView_overlayCornerRadius, 24f)
            } finally {
                recycle()
            }
        }
        borderPaint.color = borderColor
        borderPaint.strokeWidth = borderWidth
    }

    /** Crop rectangle in this view's coordinates (after layout). */
    fun getCropRect(): RectF = RectF(overlayRect)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val inset = borderWidth / 2f
        overlayRect.set(inset, inset, w - inset, h - inset)

        // Dim outside: fill full view then clear the hole
        val save = canvas.saveLayer(0f, 0f, w, h, null)
        canvas.drawRect(0f, 0f, w, h, dimPaint)
        clearPath.reset()
        clearPath.addRoundRect(overlayRect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(clearPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR)
        })
        canvas.restoreToCount(save)

        canvas.drawRoundRect(overlayRect, cornerRadius, cornerRadius, borderPaint)
        drawCornerIndicators(canvas)
    }

    private fun drawCornerIndicators(canvas: Canvas) {
        val cornerLength = 40f
        val cornerPaint = Paint(borderPaint).apply {
            strokeWidth = borderWidth * 1.5f
        }
        val left = overlayRect.left
        val top = overlayRect.top
        val right = overlayRect.right
        val bottom = overlayRect.bottom
        val r = cornerRadius

        canvas.drawLine(left, top + r, left, top + r + cornerLength, cornerPaint)
        canvas.drawLine(left + r, top, left + r + cornerLength, top, cornerPaint)
        canvas.drawLine(right, top + r, right, top + r + cornerLength, cornerPaint)
        canvas.drawLine(right - r, top, right - r - cornerLength, top, cornerPaint)
        canvas.drawLine(left, bottom - r, left, bottom - r - cornerLength, cornerPaint)
        canvas.drawLine(left + r, bottom, left + r + cornerLength, bottom, cornerPaint)
        canvas.drawLine(right, bottom - r, right, bottom - r - cornerLength, cornerPaint)
        canvas.drawLine(right - r, bottom, right - r - cornerLength, bottom, cornerPaint)
    }
}
