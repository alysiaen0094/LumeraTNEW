package com.lumera.app.ui.home

import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec

/**
 * ============================================================================
 * PIVOT BRING INTO VIEW SPEC - Netflix-Grade Premium Scroll Physics
 * ============================================================================
 *
 * This custom BringIntoViewSpec implementation solves two problems:
 *
 * 1. PIVOT ALIGNMENT (not just visibility):
 *    Standard BringIntoViewSpec only ensures an item is visible (minimum scroll).
 *    This causes "focus jumping" where the focus indicator lands at random positions.
 *    
 *    Our solution: Force the focused item's LEFT EDGE to align at a fixed
 *    "pivot point" (e.g., 10% from the left edge of the container).
 *    This creates a consistent, predictable focus position.
 *
 * 2. PREMIUM TWEEN ANIMATION:
 *    Uses tween with FastOutSlowInEasing for predictable, non-oscillating motion.
 *    This feels more premium on TV than spring animations which can oscillate.
 *
 * 3. RESTORATION SKIP:
 *    When returning from details screen, skip scrolling entirely to prevent
 *    visible scroll animation. Focus lands instantly where it was.
 *
 * MATH EXPLANATION:
 * - `offset`: The focused item's current position relative to viewport start
 * - `size`: The focused item's width
 * - `containerSize`: The viewport/container width
 * - `targetOffset`: Where we WANT the item's left edge to be (the pivot point)
 * - `scrollDelta`: How much to scroll = current position - desired position
 *
 * When scrollDelta > 0: Item is to the RIGHT of pivot → scroll RIGHT (content moves left)
 * When scrollDelta < 0: Item is to the LEFT of pivot → scroll LEFT (content moves right)
 * When scrollDelta = 0: Item is exactly at pivot → no scroll needed
 * ============================================================================
 */
@OptIn(ExperimentalFoundationApi::class)
class FocusPivotSpec(
    private val pivotFraction: Float = 0.1f,
    private val customOffset: Float? = null,
    private val skipScrollProvider: (() -> Boolean)? = null,
    private val stiffnessProvider: (() -> Float)? = null
) : BringIntoViewSpec {

    // Dynamic stiffness: StiffnessLow for single presses (premium feel),
    // StiffnessHigh for rapid navigation (keeps up with long-press)
    // No animation: snap immediately to the focused item position.
    @Deprecated("", level = DeprecationLevel.HIDDEN)
    override val scrollAnimationSpec: androidx.compose.animation.core.AnimationSpec<Float>
        get() = snap()

    /**
     * CALCULATE SCROLL DISTANCE
     * Returns the amount to scroll to bring the item to the pivot position.
     *
     * @param offset Current position of item's leading edge relative to viewport
     * @param size Width of the focused item
     * @param containerSize Width of the viewport/container
     * @return Scroll delta (positive = scroll right, negative = scroll left, 0 = no scroll)
     */
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        // Skip scroll entirely during restoration (back from details)
        if (skipScrollProvider?.invoke() == true) return 0f
        
        // Calculate the target pivot position (where item's left edge should land)
        val targetOffset = customOffset ?: (containerSize * pivotFraction)

        // Calculate scroll delta: how far the item is from the pivot
        val scrollDelta = offset - targetOffset

        // Safety: If item is already at or very close to pivot, don't scroll
        // This prevents micro-adjustments and jitter
        return if (kotlin.math.abs(scrollDelta) < 1f) 0f else scrollDelta
    }
}
