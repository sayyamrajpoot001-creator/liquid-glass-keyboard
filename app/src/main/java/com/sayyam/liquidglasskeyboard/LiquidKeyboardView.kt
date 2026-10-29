package com.sayyam.liquidglasskeyboard

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class LiquidKeyboardView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val rows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("123",",","SPACE",".","↵")
    )
    private var pressedRow = -1
    private var pressedCol = -1
    private var popupText = ""
    private var popupX = 0f
    private var popupY = 0f
    private var popupScale = 1f
    private var shift = false

    init {
        isFocusable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (272 * density).toInt())
    }

    private fun keyRect(row: Int, col: Int): RectF {
        val pad = 5f * density
        val top = 10f * density + row * 64f * density
        val rowH = 51f * density
        val gap = 4f * density
        val count = rows[row].size
        val keyW = when {
            row == 3 && col == 0 -> width * 0.14f
            row == 3 && col == 2 -> width * 0.39f
            row == 3 -> width * 0.14f
            row == 2 && (col == 0 || col == count - 1) -> width * 0.13f
            row == 1 -> (width - 42f * density) / count
            else -> (width - 20f * density) / count
        }
        var x = pad
        for (i in 0 until col) {
            x += when {
                row == 3 && i == 0 -> width * 0.14f + gap
                row == 3 && i == 2 -> width * 0.39f + gap
                row == 3 -> width * 0.14f + gap
                row == 2 && (i == 0 || i == count - 1) -> width * 0.13f + gap
                row == 1 -> keyW + gap
                else -> keyW + gap
            }
        }
        var actualW = keyW
        if (row == 3 && col == 2) actualW = width * 0.39f
        if (row == 2 && (col == 0 || col == count - 1)) actualW = width * 0.13f
        return RectF(x, top, min(width - pad, x + actualW), top + rowH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        bg.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.rgb(24, 37, 58), Color.rgb(37, 50, 72), Color.rgb(15, 24, 42)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        // Soft glass glows
        bg.shader = RadialGradient(width * .22f, height * .18f, width * .65f,
            intArrayOf(0x557EEBFF, 0x007EEBFF), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        rows.forEachIndexed { r, row ->
            row.forEachIndexed { c, label ->
                val b = keyRect(r, c)
                val selected = r == pressedRow && c == pressedCol
                val lift = if (selected) 5f * density else 0f
                val key = RectF(b.left, b.top - lift, b.right, b.bottom - lift)
                paint.shader = null
                paint.style = Paint.Style.FILL
                paint.color = if (selected) 0x99E7F8FF.toInt() else 0x40FFFFFF
                paint.setShadowLayer(8f * density, 0f, 3f * density, 0x66000000)
                canvas.drawRoundRect(key, 13f * density, 13f * density, paint)
                paint.clearShadowLayer()
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f * density
                paint.shader = LinearGradient(key.left, key.top, key.right, key.bottom,
                    intArrayOf(0xCCFFFFFF.toInt(), 0x33FFFFFF, 0x66B9E8FF), null, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(key, 13f * density, 13f * density, paint)
                paint.shader = null
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                paint.textSize = (if (label == "SPACE") 12f else 19f) * density
                val shown = if (shift && label.length == 1) label.uppercase() else if (label == "SPACE") "English (US)" else label
                canvas.drawText(shown, key.centerX(), key.centerY() + paint.textSize * .34f, paint)
                // top highlight gives the wet-glass edge
                paint.color = 0x55FFFFFF
                canvas.drawRoundRect(key.left + 5f*density, key.top + 2f*density, key.right - 5f*density, key.top + 4f*density, 2f*density, 2f*density, paint)
            }
        }
        if (popupText.isNotEmpty()) {
            val w = 54f * density
            val h = 65f * density
            val bubble = RectF(popupX - w/2, popupY - h + 7f*density, popupX + w/2, popupY + 7f*density)
            paint.style = Paint.Style.FILL
            paint.color = 0xDDF7FCFF.toInt()
            paint.setShadowLayer(14f*density, 0f, 5f*density, 0x88000000.toInt())
            canvas.drawRoundRect(bubble, 18f*density, 18f*density, paint)
            paint.clearShadowLayer()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.2f*density
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawRoundRect(bubble, 18f*density, 18f*density, paint)
            paint.style = Paint.Style.FILL
            paint.color = 0xFF152238.toInt()
            paint.textSize = 31f*density
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(popupText, popupX, bubble.centerY() + 11f*density, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                pressedRow = -1; pressedCol = -1
                rows.forEachIndexed { r, row -> row.indices.forEach { c ->
                    val b = keyRect(r, c)
                    if (b.contains(x, y)) { pressedRow = r; pressedCol = c }
                } }
                if (pressedRow >= 0) {
                    val label = rows[pressedRow][pressedCol]
                    popupText = when (label) { "SPACE" -> " "; "⌫", "↵", "⇧", "123", ",", "." -> label; else -> if (shift) label.uppercase() else label }
                    val b = keyRect(pressedRow, pressedCol)
                    popupX = b.centerX(); popupY = b.top - 4f*density
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (pressedRow >= 0 && pressedCol >= 0) {
                    val label = rows[pressedRow][pressedCol]
                    when (label) {
                        "⇧" -> shift = !shift
                        "SPACE" -> onKey("SPACE")
                        else -> onKey(if (shift && label.length == 1) label.uppercase() else label)
                    }
                }
                pressedRow = -1; pressedCol = -1; popupText = ""
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> { pressedRow = -1; pressedCol = -1; popupText = ""; invalidate(); return true }
        }
        return true
    }
}
