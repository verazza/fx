package fx.tetris.logic

import scalafx.scene.paint.Color

object Tetromino {
  val iShape: Array[Array[Int]] = Array(
    Array(0, 0, 0, 0),
    Array(1, 1, 1, 1),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val shapeColors = Map(
    "I" -> Color.Cyan
  )
}
