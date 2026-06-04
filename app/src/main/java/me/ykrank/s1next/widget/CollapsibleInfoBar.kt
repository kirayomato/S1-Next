package me.ykrank.s1next.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.R as AppCompatR
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.ColorUtils
import me.ykrank.s1next.R
import com.github.ykrank.androidtools.R as ToolsR

class CollapsibleInfoBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private var inflatingInternal = false
    private var contentContainer: LinearLayout? = null
    private lateinit var titleView: TextView
    private lateinit var expandIcon: AppCompatImageView

    var title: CharSequence?
        get() = titleView.text
        set(value) {
            titleView.text = value
        }

    var isExpanded: Boolean = false
        set(value) {
            field = value
            contentContainer?.visibility = if (value) View.VISIBLE else View.GONE
            expandIcon.setImageResource(
                if (value) {
                    R.drawable.ic_expand_less
                } else {
                    R.drawable.ic_expand_more
                }
            )
            onExpandedChanged?.invoke(value)
        }

    var onExpandedChanged: ((Boolean) -> Unit)? = null

    init {
        orientation = VERTICAL
        if (background == null) {
            setBackgroundColor(context.resolveInfoBarBackground())
        }

        inflatingInternal = true
        LayoutInflater.from(context).inflate(R.layout.view_collapsible_info_bar, this, true)
        inflatingInternal = false

        titleView = findViewById(R.id.collapsible_info_bar_title)
        expandIcon = findViewById(R.id.collapsible_info_bar_expand_icon)
        contentContainer = findViewById(R.id.collapsible_info_bar_content)

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CollapsibleInfoBar)
        title = typedArray.getText(R.styleable.CollapsibleInfoBar_collapsibleInfoBarTitle)
        isExpanded = typedArray.getBoolean(R.styleable.CollapsibleInfoBar_collapsibleInfoBarExpanded, false)
        typedArray.recycle()

        findViewById<View>(R.id.collapsible_info_bar_header).setOnClickListener {
            isExpanded = !isExpanded
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        val container = contentContainer
        if (inflatingInternal || container == null) {
            super.addView(child, index, params)
        } else {
            container.addView(child, index, params)
        }
    }

    private fun Context.resolveInfoBarBackground(): Int {
        val base = resolveThemeColor(ToolsR.attr.cardViewBackground)
            ?: resolveThemeColor(android.R.attr.colorBackground)
            ?: Color.TRANSPARENT
        val primaryTint = resolveThemeColor(AppCompatR.attr.colorPrimary)?.withOpaqueAlpha()
        val primaryBackground = primaryTint?.let { ColorUtils.blendARGB(base, it, PRIMARY_BLEND_ALPHA) }
        if (primaryBackground != null && !primaryBackground.isTooCloseTo(base)) {
            return primaryBackground
        }
        val onSurface = resolveThemeColor(android.R.attr.textColorPrimary)?.withOpaqueAlpha()
        return onSurface?.let { ColorUtils.blendARGB(base, it, ON_SURFACE_BLEND_ALPHA) } ?: base
    }

    private fun Context.resolveThemeColor(@AttrRes attr: Int): Int? {
        val typedArray = obtainStyledAttributes(intArrayOf(attr))
        return try {
            if (typedArray.hasValue(0)) {
                typedArray.getColor(0, Color.TRANSPARENT)
            } else {
                null
            }
        } finally {
            typedArray.recycle()
        }
    }

    private fun Int.withOpaqueAlpha(): Int {
        return ColorUtils.setAlphaComponent(this, 255)
    }

    private fun Int.isTooCloseTo(other: Int): Boolean {
        val redDelta = Color.red(this) - Color.red(other)
        val greenDelta = Color.green(this) - Color.green(other)
        val blueDelta = Color.blue(this) - Color.blue(other)
        return redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta < MIN_COLOR_DISTANCE_SQUARED
    }

    companion object {
        private const val PRIMARY_BLEND_ALPHA = 0.08f
        private const val ON_SURFACE_BLEND_ALPHA = 0.06f
        private const val MIN_COLOR_DISTANCE_SQUARED = 12 * 12
    }
}
