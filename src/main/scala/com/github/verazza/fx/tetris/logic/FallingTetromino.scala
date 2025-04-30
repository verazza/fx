package fx.tetris.logic

import scalafx.scene.paint.Color

class FallingTetromino(val shape: Array[Array[Int]], val color: Color) {
  var x: Int = 0
  var y: Int = 0

  def moveDown(): Unit = {
    y += 1
  }
}
