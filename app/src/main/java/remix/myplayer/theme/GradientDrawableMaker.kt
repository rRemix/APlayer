package remix.myplayer.theme

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import remix.myplayer.util.ColorUtil

class GradientDrawableMaker {
  private var width = 0
  private var height = 0
  private var shape = GradientDrawable.RECTANGLE
  private var alpha = 1f
  private var color = Color.WHITE
  private var corner = 0f
  private var strokeSize = 0
  private var strokeColor = Color.WHITE

  fun width(width: Int): GradientDrawableMaker {
    this.width = width
    return this
  }

  fun height(height: Int): GradientDrawableMaker {
    this.height = height
    return this
  }

  fun shape(shape: Int): GradientDrawableMaker {
    this.shape = shape
    return this
  }

  fun alpha(alpha: Float): GradientDrawableMaker {
    this.alpha = alpha
    return this
  }

  fun color(color: Int): GradientDrawableMaker {
    this.color = color
    return this
  }

  fun corner(corner: Float): GradientDrawableMaker {
    this.corner = corner
    return this
  }

  fun strokeSize(strokeSize: Int): GradientDrawableMaker {
    this.strokeSize = strokeSize
    return this
  }

  fun strokeColor(strokeColor: Int): GradientDrawableMaker {
    this.strokeColor = strokeColor
    return this
  }


  fun make(): GradientDrawable {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(ColorUtil.adjustAlpha(color, alpha))
    gradientDrawable.shape = shape
    if (corner > 0)
      gradientDrawable.setCornerRadius(corner)
    if (strokeSize > 0)
      gradientDrawable.setStroke(strokeSize, strokeColor)

    if (width > 0 && height > 0)
      gradientDrawable.setSize(width, height)

    return gradientDrawable
  }
}