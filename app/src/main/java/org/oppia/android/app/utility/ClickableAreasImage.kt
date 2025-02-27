package org.oppia.android.app.utility

import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import org.oppia.android.R
import org.oppia.android.app.model.ImageWithRegions.LabeledRegion
import org.oppia.android.app.model.UserAnswerState
import org.oppia.android.app.player.state.ImageRegionSelectionInteractionView
import org.oppia.android.app.shim.ViewBindingShim
import org.oppia.android.app.translation.AppLanguageResourceHandler
import kotlin.math.roundToInt

/** Helper class to handle clicks on an image along with highlighting the selected region. */
class ClickableAreasImage(
  private val imageView: ImageRegionSelectionInteractionView,
  private val parentView: FrameLayout,
  private val listener: OnClickableAreaClickedListener,
  bindingInterface: ViewBindingShim,
  private val isAccessibilityEnabled: Boolean,
  private val clickableAreas: List<LabeledRegion>,
  userAnswerState: UserAnswerState,
  private val resourceHandler: AppLanguageResourceHandler
) {
  private var imageLabel: String? = null
  private val defaultRegionView by lazy { bindingInterface.getDefaultRegion(parentView) }

  init {
    imageView.initializeShowRegionTouchListener()

    if (userAnswerState.imageLabel.isNotBlank()) {
      imageLabel = userAnswerState.imageLabel
    }
  }

  /**
   * Called when an image is clicked.
   *
   * @param x the relative x coordinate according to image
   * @param y the relative y coordinate according to image
   */
  private fun onPhotoTap(x: Float, y: Float) {
    // Show default region for non-accessibility cases and this will be only called when user taps
    // on unspecified region.
    if (!isAccessibilityEnabled) {
      resetRegionSelectionViews()
      defaultRegionView.setBackgroundResource(R.drawable.selected_region_background)
      defaultRegionView.x = x
      defaultRegionView.y = y
      listener.onClickableAreaTouched(DefaultRegionClickedEvent(x, y))
    }
  }

  /** Function to remove the background from the views. */
  private fun resetRegionSelectionViews() {
    parentView.forEachIndexed { index: Int, childView: View ->
      // Remove any previously selected region excluding 0th index(image view)
      if (index > 0) {
        childView.setBackgroundResource(0)
        if (childView.tag != null) {
          val regionLabel = childView.tag as String
          clickableAreas.find { it.label == regionLabel }?.let { clickableArea ->
            updateRegionContentDescription(
              childView,
              clickableArea,
              regionLabel == imageLabel
            )
          }
        }
      }
    }
  }

  /** Get X co-ordinate scaled according to image. */
  private fun getXCoordinate(x: Float): Float {
    return x * getImageViewContentWidth()
  }

  /** Get Y co-ordinate scaled according to image. */
  private fun getYCoordinate(y: Float): Float {
    return y * getImageViewContentHeight()
  }

  private fun getImageViewContentWidth(): Int {
    return imageView.width - imageView.paddingStart - imageView.paddingEnd
  }

  private fun getImageViewContentHeight(): Int {
    return imageView.height - imageView.paddingTop - imageView.paddingBottom
  }

  /** Add selectable regions to [FrameLayout]. */
  fun addRegionViews() {
    // Remove all views other than the default region & selectable image.
    parentView.children.filter {
      it.id != imageView.id && it.id != defaultRegionView.id
    }.forEach(parentView::removeView)
    clickableAreas.forEach { clickableArea ->
      val imageRect = RectF(
        getXCoordinate(clickableArea.region.area.upperLeft.x),
        getYCoordinate(clickableArea.region.area.upperLeft.y),
        getXCoordinate(clickableArea.region.area.lowerRight.x),
        getYCoordinate(clickableArea.region.area.lowerRight.y)
      )
      val layoutParams = FrameLayout.LayoutParams(
        imageRect.width().roundToInt(),
        imageRect.height().roundToInt()
      )
      val newView = View(parentView.context)
      // ClickableArea coordinates are not laid-out properly in RTL. The image region coordinates
      // are from left-to-right with an upper left origin and touch coordinates from Android start
      // from the right in RTL mode. Thus, to avoid this situation, force layout direction to LTR in
      // all situations.
      ViewCompat.setLayoutDirection(parentView, ViewCompat.LAYOUT_DIRECTION_LTR)
      newView.layoutParams = layoutParams
      newView.x = imageRect.left
      newView.y = imageRect.top
      newView.isClickable = true
      newView.isFocusable = true
      newView.isFocusableInTouchMode = true
      newView.tag = clickableArea.label

      val isInitiallySelected = clickableArea.label.equals(imageLabel)
      updateRegionContentDescription(newView, clickableArea, isInitiallySelected)

      newView.initializeToggleRegionTouchListener(clickableArea)

      if (isInitiallySelected) {
        showOrHideRegion(newView = newView, clickableArea = clickableArea)
      }
      if (isAccessibilityEnabled) {
        // Make default region visibility gone when talkback enabled to avoid any accidental touch.
        defaultRegionView.isVisible = false
        newView.setOnClickListener {
          showOrHideRegion(newView, clickableArea)
        }
      }

      parentView.addView(newView)
    }

    // Ensure that the children views are properly computed. The specific flow below is recommended
    // by https://stackoverflow.com/a/42430695/3689782 where it's also explained in great detail.
    // The 'post' seems necessary since, from observation, requesting layout & invalidation doesn't
    // always work (perhaps since this method can be called during a layout step), so posting
    // ensures that the views are eventually computed. It's not obvious why Android sometimes
    // doesn't compute the region view dimensions, but it results in the interaction being
    // non-interactive (though it's recoverable with back & forward navigation or rotation, this
    // isn't likely to be obvious to learners and it's a generally poor user experience).
    parentView.post {
      parentView.children.forEach(View::forceLayout)
      parentView.invalidate()
      parentView.requestLayout()
    }
  }

  private fun showOrHideRegion(newView: View, clickableArea: LabeledRegion) {
    if (clickableArea.label != imageLabel) {
      imageLabel = clickableArea.label
      resetRegionSelectionViews()
      newView.setBackgroundResource(R.drawable.selected_region_background)

      listener.onClickableAreaTouched(
        NamedRegionClickedEvent(
          clickableArea.label,
          generateContentDescription(clickableArea, true)
        )
      )
    }
  }

  private fun View.initializeShowRegionTouchListener() {
    setOnTouchListener { view, motionEvent ->
      if (motionEvent.action == MotionEvent.ACTION_DOWN) {
        onPhotoTap(motionEvent.x, motionEvent.y)
      }
      view.performClick()
      return@setOnTouchListener false
    }
  }

  private fun View.initializeToggleRegionTouchListener(clickableArea: LabeledRegion) {
    setOnTouchListener { view, event ->
      if (event.action == MotionEvent.ACTION_DOWN) {
        showOrHideRegion(this@initializeToggleRegionTouchListener, clickableArea)
      }
      view.performClick()
      return@setOnTouchListener true
    }
  }

  private fun generateContentDescription(
    clickableArea: LabeledRegion,
    isSelected: Boolean
  ): String = if (isSelected) {
    resourceHandler.getStringInLocaleWithWrapping(
      R.string.selected_image_region_selection_content_description,
      clickableArea.label
    )
  } else {
    resourceHandler.getStringInLocaleWithWrapping(
      R.string.unselected_image_region_selection_content_description,
      clickableArea.label
    )
  }

  private fun updateRegionContentDescription(
    view: View,
    clickableArea: LabeledRegion,
    isSelected: Boolean
  ) {
    view.contentDescription = generateContentDescription(clickableArea, isSelected)
  }
}
