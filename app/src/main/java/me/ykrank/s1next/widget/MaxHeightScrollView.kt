package me.ykrank.s1next.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class MaxHeightScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ScrollView(context, attrs, defStyleAttr) {

    private val maxHeight: Int

    init {
        val typedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.maxHeight))
        maxHeight = typedArray.getDimensionPixelSize(0, 0)
        typedArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val limitedHeightMeasureSpec = if (maxHeight > 0) {
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthMeasureSpec, limitedHeightMeasureSpec)
    }
}
