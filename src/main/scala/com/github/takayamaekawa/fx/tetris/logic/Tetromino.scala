package fx.tetris.logic

import scalafx.scene.paint.Color
import scala.util.Random

object Tetromino {
  val rd = new Random

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

  val oShape: Array[Array[Int]] =
    Array(
      Array(0, 1, 1, 0),
      Array(0, 1, 1, 0),
      Array(0, 0, 0, 0),
      Array(0, 0, 0, 0)
    )

  val tetrominoData: Seq[(String, Array[Array[Int]], Color)] = Seq(
    ("I", iShape, Color.Cyan),
    ("T", tShape, Color.Purple),
    ("L", lShape, Color.Orange),
    ("J", jShape, Color.Blue),
    ("S", sShape, Color.Green),
    ("Z", zShape, Color.Red),
    ("O", oShape, Color.Yellow)
  )

  def getRandomTetromino(): (String, Array[Array[Int]], Color) = {
    val randomIndex = rd.nextInt(tetrominoData.length)
    val selected = tetrominoData(randomIndex)
    (selected._1, selected._2, selected._3) // 名前、形状、色を返す
  }
}
