package me.ykrank.s1next.view.page.post.render

import android.content.res.ColorStateList
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.github.ykrank.androidtools.util.ResourceUtil
import me.ykrank.s1next.R
import java.util.WeakHashMap
import kotlin.math.roundToInt

object PostActionMenuPopup {

    data class Item(
        @StringRes val titleRes: Int,
        @DrawableRes val iconRes: Int,
        val onClick: () -> Unit,
    )

    private val touchPoints = WeakHashMap<View, PointF>()

    fun recordTouchPoint(view: View, event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_DOWN ||
            event.actionMasked == MotionEvent.ACTION_MOVE
        ) {
            touchPoints[view] = PointF(event.rawX, event.rawY)
        }
    }

    fun show(anchor: View, topItems: List<Item>, listItems: List<Item>) {
        if (topItems.isEmpty() && listItems.isEmpty()) {
            return
        }
        val context = anchor.context
        val root = anchor.rootView ?: anchor
        val popupRoot = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = AppCompatResources.getDrawable(context, R.drawable.bg_post_action_menu_popup)
            clipToOutline = true
        }
        popupRoot.layoutParams = ViewGroup.LayoutParams(context.dp(POPUP_WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT)
        lateinit var popupWindow: PopupWindow
        val onItemClick: (Item) -> Unit = { item ->
            popupWindow.dismiss()
            item.onClick()
        }
        addTopRow(popupRoot, topItems, onItemClick)
        if (topItems.isNotEmpty() && listItems.isNotEmpty()) {
            popupRoot.addView(createGroupDivider(context))
        }
        addListRows(popupRoot, listItems, onItemClick)

        val popupContentWidth = context.dp(POPUP_WIDTH_DP)
        popupWindow = PopupWindow(
            popupRoot,
            popupContentWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = context.dp(POPUP_ELEVATION_DP).toFloat()
            setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.bg_post_action_menu_popup))
        }

        popupRoot.measure(
            View.MeasureSpec.makeMeasureSpec(popupContentWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupWidth = popupContentWidth
        val popupHeight = popupRoot.measuredHeight
        val touchPoint = touchPoints[anchor] ?: anchor.centerOnScreen()
        val visibleFrame = Rect()
        root.getWindowVisibleDisplayFrame(visibleFrame)
        val margin = context.dp(SCREEN_MARGIN_DP)
        val x = clampPosition(
            (touchPoint.x - popupWidth / 2f).roundToInt(),
            visibleFrame.left + margin,
            visibleFrame.right - popupWidth - margin
        )
        val y = clampPosition(
            (touchPoint.y - popupHeight / 2f).roundToInt(),
            visibleFrame.top + margin,
            visibleFrame.bottom - popupHeight - margin
        )
        popupWindow.showAtLocation(root, Gravity.NO_GRAVITY, x, y)
    }

    private fun addTopRow(container: LinearLayout, items: List<Item>, onItemClick: (Item) -> Unit) {
        if (items.isEmpty()) {
            return
        }
        val context = container.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        items.forEachIndexed { index, item ->
            row.addView(
                createTopCell(context, item, onItemClick),
                LinearLayout.LayoutParams(0, context.dp(TOP_ROW_HEIGHT_DP), 1f)
            )
            if (index < items.lastIndex) {
                row.addView(createDivider(context, vertical = true))
            }
        }
        container.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, context.dp(TOP_ROW_HEIGHT_DP)))
    }

    private fun addListRows(container: LinearLayout, items: List<Item>, onItemClick: (Item) -> Unit) {
        if (items.isEmpty()) {
            return
        }
        val context = container.context
        items.forEachIndexed { index, item ->
            container.addView(createListRow(context, item, onItemClick), LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(LIST_ROW_HEIGHT_DP)
            ))
            if (index < items.lastIndex) {
                container.addView(createDivider(context, vertical = false))
            }
        }
    }

    private fun createTopCell(context: Context, item: Item, onItemClick: (Item) -> Unit): View {
        val iconTint = ColorStateList.valueOf(
            ResourceUtil.getAttrColorInt(context, R.attr.iconPostToolTintColor)
        )
        val textColor = ResourceUtil.getTextColorPrimary(context)
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            foreground = selectableItemBackground(context)
            setPadding(
                context.dp(TOP_CELL_PADDING_HORIZONTAL_DP),
                context.dp(TOP_CELL_PADDING_VERTICAL_DP),
                context.dp(TOP_CELL_PADDING_HORIZONTAL_DP),
                context.dp(TOP_CELL_PADDING_VERTICAL_DP)
            )
            addView(ImageView(context).apply {
                setImageResource(item.iconRes)
                imageTintList = iconTint
            }, LinearLayout.LayoutParams(context.dp(TOP_ICON_SIZE_DP), context.dp(TOP_ICON_SIZE_DP)))
            addView(TextView(context).apply {
                setText(item.titleRes)
                gravity = Gravity.CENTER
                maxLines = 1
                setTextColor(textColor)
                textSize = TOP_TEXT_SIZE_SP
                includeFontPadding = false
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = context.dp(TEXT_TOP_MARGIN_DP)
            })
            setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private fun createListRow(context: Context, item: Item, onItemClick: (Item) -> Unit): View {
        val iconTint = ColorStateList.valueOf(
            ResourceUtil.getAttrColorInt(context, R.attr.iconPostToolTintColor)
        )
        val textColor = ResourceUtil.getTextColorPrimary(context)
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
            foreground = selectableItemBackground(context)
            setPadding(context.dp(LIST_ROW_PADDING_HORIZONTAL_DP), 0, context.dp(LIST_ROW_PADDING_HORIZONTAL_DP), 0)
            addView(ImageView(context).apply {
                setImageResource(item.iconRes)
                imageTintList = iconTint
            }, LinearLayout.LayoutParams(context.dp(LIST_ICON_SIZE_DP), context.dp(LIST_ICON_SIZE_DP)))
            addView(TextView(context).apply {
                setText(item.titleRes)
                maxLines = 1
                setTextColor(textColor)
                textSize = LIST_TEXT_SIZE_SP
                includeFontPadding = false
            }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = context.dp(LIST_TEXT_MARGIN_START_DP)
            })
            setOnClickListener {
                onItemClick(item)
            }
        }
    }

    private fun createGroupDivider(context: Context): View {
        return View(context).apply {
            background = ColorDrawable(groupDividerColor(context))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                context.dp(GROUP_DIVIDER_HEIGHT_DP)
            )
        }
    }

    private fun createDivider(context: Context, vertical: Boolean): View {
        return LineDividerView(context, vertical).apply {
            layoutParams = if (vertical) {
                LinearLayout.LayoutParams(DIVIDER_SIZE_PX, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                    topMargin = context.dp(VERTICAL_DIVIDER_MARGIN_DP)
                    bottomMargin = context.dp(VERTICAL_DIVIDER_MARGIN_DP)
                }
            } else {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DIVIDER_SIZE_PX).apply {
                    leftMargin = context.dp(HORIZONTAL_DIVIDER_MARGIN_DP)
                    rightMargin = context.dp(HORIZONTAL_DIVIDER_MARGIN_DP)
                }
            }
        }
    }

    private fun clampPosition(target: Int, min: Int, max: Int): Int {
        if (max < min) {
            return min
        }
        return target.coerceIn(min, max)
    }

    private fun selectableItemBackground(context: android.content.Context) =
        AppCompatResources.getDrawable(
            context,
            ResourceUtil.getResourceId(context, android.R.attr.selectableItemBackground)
        )

    private fun View.centerOnScreen(): PointF {
        val location = IntArray(2)
        getLocationOnScreen(location)
        return PointF(
            location[0] + width / 2f,
            location[1] + height / 2f
        )
    }

    private fun android.content.Context.dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    private class LineDividerView(context: Context, private val vertical: Boolean) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineDividerColor(context)
            strokeWidth = DIVIDER_SIZE_PX.toFloat()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (vertical) {
                val x = width / 2f
                canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            } else {
                val y = height / 2f
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
            }
        }
    }

    private fun groupDividerColor(context: Context): Int {
        return derivedBackgroundSeparatorColor(context, GROUP_DIVIDER_BLEND_RATIO)
    }

    private fun lineDividerColor(context: Context): Int {
        return derivedBackgroundSeparatorColor(context, LINE_DIVIDER_BLEND_RATIO)
    }

    private fun derivedBackgroundSeparatorColor(context: Context, ratio: Float): Int {
        val background = ResourceUtil.getAttrColorInt(context, android.R.attr.colorBackground)
        val target = if (isDarkColor(background)) Color.WHITE else Color.BLACK
        return blendColor(background, target, ratio)
    }

    private fun isDarkColor(color: Int): Boolean {
        val red = Color.red(color) / 255.0
        val green = Color.green(color) / 255.0
        val blue = Color.blue(color) / 255.0
        val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
        return luminance < 0.5
    }

    private fun blendColor(from: Int, to: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        return Color.argb(
            Color.alpha(from),
            (Color.red(from) * inverseRatio + Color.red(to) * ratio).roundToInt(),
            (Color.green(from) * inverseRatio + Color.green(to) * ratio).roundToInt(),
            (Color.blue(from) * inverseRatio + Color.blue(to) * ratio).roundToInt()
        )
    }

    private const val POPUP_WIDTH_DP = 268
    private const val POPUP_RADIUS_DP = 14
    private const val TOP_ROW_HEIGHT_DP = 60
    private const val LIST_ROW_HEIGHT_DP = 44
    private const val TOP_ICON_SIZE_DP = 18
    private const val LIST_ICON_SIZE_DP = 18
    private const val TEXT_TOP_MARGIN_DP = 4
    private const val TOP_CELL_PADDING_HORIZONTAL_DP = 8
    private const val TOP_CELL_PADDING_VERTICAL_DP = 6
    private const val LIST_ROW_PADDING_HORIZONTAL_DP = 14
    private const val LIST_TEXT_MARGIN_START_DP = 16
    private const val SCREEN_MARGIN_DP = 8
    private const val POPUP_ELEVATION_DP = 12
    private const val GROUP_DIVIDER_HEIGHT_DP = 8
    private const val GROUP_DIVIDER_BLEND_RATIO = 0.08f
    private const val DIVIDER_SIZE_PX = 1
    private const val HORIZONTAL_DIVIDER_MARGIN_DP = 18
    private const val VERTICAL_DIVIDER_MARGIN_DP = 12
    private const val LINE_DIVIDER_BLEND_RATIO = 0.05f
    private const val TOP_TEXT_SIZE_SP = 13f
    private const val LIST_TEXT_SIZE_SP = 14f
}
