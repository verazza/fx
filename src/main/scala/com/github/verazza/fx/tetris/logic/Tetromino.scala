package fx.tetris.logic

import scalafx.scene.paint.Color

object Tetromino {
  val iShape: Array[Array[Int]] = Array(
    Array(0, 0, 0, 0),
    Array(1, 1, 1, 1),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val tShape: Array[Array[Int]] = Array(
    Array(0, 1, 0, 0),
    Array(1, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val lShape: Array[Array[Int]] = Array(
    Array(0, 0, 1, 0),
    Array(1, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val jShape: Array[Array[Int]] = Array(
    Array(1, 0, 0, 0),
    Array(1, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val sShape: Array[Array[Int]] = Array(
    Array(0, 1, 1, 0),
    Array(1, 1, 0, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val zShape: Array[Array[Int]] = Array(
    Array(1, 1, 0, 0),
    Array(0, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val oShape: Array[Array[Int]] = Array(
    Array(1, 1, 0, 0),
    Array(1, 1, 0, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val shapes = Map(
    "I" -> iShape,
    "T" -> tShape,
    "L" -> lShape,
    "J" -> jShape,
    "S" -> sShape,
    "Z" -> zShape,
    "O" -> oShape
  )

  val shapeColors = Map(
    "I" -> Color.Cyan,
    "T" -> Color.Purple,
    "L" -> Color.Orange,
    "J" -> Color.Blue,
    "S" -> Color.Green,
    "Z" -> Color.Red,
    "O" -> Color.Yellow
  )
}
