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
  def getRotatedShape(): Array[Array[Int]] = {
    val N =
      currentShape.length // Assuming square matrix for simplicity of rotation
    val newShape = Array.ofDim[Int](N, N)
    for (r <- 0 until N) {
      for (c <- 0 until N) {
        newShape(c)(N - 1 - r) = currentShape(r)(c)
      }
    }
    newShape
  }

  // 形状を実際に回転させる
  def rotateClockwise(): Unit = {
    currentShape = getRotatedShape()
  }

  // 回転前の状態に戻すために、反時計回りに回転するヘルパー (必要に応じて)
  // もしくは、回転前の形状を一時的に保存しておく戦略もアリ
  def undoRotate(): Unit = {
    // 3回時計回りに回転すると反時計回りと同じ
    currentShape = getRotatedShape() // 1
    currentShape = getRotatedShape() // 2
    currentShape = getRotatedShape() // 3
  }
}
