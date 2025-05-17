package fx.tetris.logic

import scalafx.scene.paint.Color

class FallingTetromino(initialShape: Array[Array[Int]], val color: Color) {
  var x: Int = 0
  var y: Int = 0
  var currentShape: Array[Array[Int]] = initialShape

  def moveDown(): Unit = {
    y += 1
  }

  def moveLeft(): Unit = {
    x -= 1
  }

  def moveRight(): Unit = {
    x += 1
  }

  // 時計回りに回転した新しい形状を返す (実際の形状は変更しない)
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
  }
}
