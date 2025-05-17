package fx.tetris.ui

import scalafx.scene.layout.Pane
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Line, Rectangle}
import scalafx.scene.text.Text
import fx.tetris.logic.{FallingTetromino, GameConstants} // GameConstants をインポート

object TetrisDrawer {

  import GameConstants._ // 定数をインポート

  def drawGameUI(
    pane: Pane,
    fallingTetromino: FallingTetromino,
    currentBoard: Array[Array[Option[Color]]],
    isGameOver: Boolean,
    txtGameOver: Text // ゲームオーバーテキストも描画対象として受け取る
  ): Unit = {
    pane.children.clear() // 描画前に常にクリア
    drawGridUI(pane) // グリッドを描画

    // ゲームオーバーテキストは、isGameOver が true で、かつ visible プロパティが true の場合にのみ追加
    // (点滅アニメーションで opacity が変わるため、visible フラグで制御)
    if (isGameOver && txtGameOver.visible.value) {
      pane.children.add(txtGameOver)
    }

    // 1. 固定されたミノを描画
    for (r <- 0 until NumRows; c <- 0 until NumCols) {
      currentBoard(r)(c).foreach { color =>
        val rect = new Rectangle {
          x = c * CellSize
          y = r * CellSize
          width = CellSize
          height = CellSize
          fill = color
          stroke = Color.Black
          strokeWidth = 0.5
        }
        pane.children += rect
      }
    }

    // 2. 落下中のミノを描画 (ゲームオーバーでなければ)
    if (!isGameOver) {
      val shape = fallingTetromino.currentShape
      val color = fallingTetromino.color
      val startX = fallingTetromino.x
      val startY = fallingTetromino.y

      for (row <- shape.indices; col <- shape(row).indices) {
        if (shape(row)(col) == 1) {
          val rect = new Rectangle {
            x = (startX + col) * CellSize
            y = (startY + row) * CellSize
            width = CellSize
            height = CellSize
            fill = color
            stroke = Color.Black
            strokeWidth = 1
          }
          pane.children += rect
        }
      }
    }
  }

  def drawGridUI(pane: Pane): Unit = {
    for (i <- 0 to NumCols) {
      pane.children += new Line {
        startX = i * CellSize
        startY = 0
        endX = i * CellSize
        endY = BoardHeight
        stroke = Color.DarkGray
        strokeWidth = 0.5
      }
    }
    for (i <- 0 to NumRows) {
      pane.children += new Line {
        startX = 0
        startY = i * CellSize
        endX = BoardWidth
        endY = i * CellSize
        stroke = Color.DarkGray
        strokeWidth = 0.5
      }
    }
  }
}
