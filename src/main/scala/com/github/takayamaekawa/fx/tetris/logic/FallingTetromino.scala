package fx.tetris.logic

import scalafx.scene.paint.Color

class FallingTetromino(
  val minoName: String,
  initialShape: Array[Array[Int]],
  val color: Color
) {
  var x: Int = 0
  var y: Int = 0
  var currentShape: Array[Array[Int]] = initialShape
  var orientation: Int =
    0 // 0: 初期(北), 1: 時計回り90度(東), 2: 180度(南), 3: 反時計回り90度(西)

  def moveDown(): Unit = {
    y += 1
  }

  def moveLeft(): Unit = {
    x -= 1
  }

  def moveRight(): Unit = {
    x += 1
  }

  def getRotatedShape(clockwise: Boolean = true): Array[Array[Int]] = {
    val N = currentShape.length
    val newShape = Array.ofDim[Int](N, N)
    if (clockwise) {
      for (r <- 0 until N; c <- 0 until N) {
        newShape(c)(N - 1 - r) = currentShape(r)(c)
      }
    } else { // 反時計回り
      for (r <- 0 until N; c <- 0 until N) {
        newShape(N - 1 - c)(r) = currentShape(r)(c)
      }
    }
    newShape
  }

  def rotate(clockwise: Boolean = true): Unit = {
    currentShape = getRotatedShape(clockwise)
    if (clockwise) {
      orientation = (orientation + 1) % 4
    } else {
      orientation = (orientation + 3) % 4 // (orientation - 1 + 4) % 4 と同じ
    }
  }
}
