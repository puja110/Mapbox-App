package com.example.mapboxdemo.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

fun createPoiMarkerBitmap(context: Context, title: String): Bitmap {
    val density = context.resources.displayMetrics.density
    val paddingHorizontal = (16 * density).toInt()
    val paddingVertical = (8 * density).toInt()
    val arrowSize = (12 * density).toInt()
    val arrowMarginTop = (4 * density).toInt()

    val label = TextView(context).apply {
        text = title
        setTextColor(Color.WHITE)
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        setTypeface(null, Typeface.BOLD)
        setBackgroundDrawable(
            GradientDrawable().apply {
                setColor("#2962FF".toColorInt())
            }
        )
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val arrow = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(arrowSize, arrowSize).apply {
            topMargin = arrowMarginTop
            gravity = Gravity.CENTER_HORIZONTAL
        }
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor("#2962FF".toColorInt())
        }
        rotation = 45f
    }

    val container = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        addView(label)
        addView(arrow)
    }

    container.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
    )
    container.layout(0, 0, container.measuredWidth, container.measuredHeight)

    val width = container.measuredWidth.takeIf { it > 0 } ?: 1
    val height = container.measuredHeight.takeIf { it > 0 } ?: 1

    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    container.draw(canvas)

    return bitmap
}




