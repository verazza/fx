package fx.tetris.ui

import scalafx.scene.layout.Pane
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Line, Rectangle}
import scalafx.scene.text.Text
import fx.tetris.logic.{FallingTetromino, GameConstants}

object TetrisDrawer {

  import GameConstants._

  def drawGameUI(
    pane: Pane,
    fallingTetromino: FallingTetromino,
    currentBoard: Array[Array[Option[Color]]],
    isGameOver: Boolean,
    txtGameOver: Text
  ): Unit = {
    pane.children.clear()
    drawGridUI(pane) // 1. グリッドを描画

    // 2. 固定されたミノを描画
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

    // 3. 落下中のミノを描画 (ゲームオーバー直前の状態も表示する場合)
    //    ここでは、ゲームオーバーでない場合のみ描画するシンプルな形にしておく
    //    あるいは、ゲームオーバーでも最後の currentFallingTetromino を描画する選択もできる
    if (!isGameOver) {
      val shape = fallingTetromino.currentShape
      val color = fallingTetromino.color
      val startX = fallingTetromino.x
      val startY = fallingTetromino.y

      for (row <- shape.indices; col <- shape(row).indices) {
        if (shape(row)(col) == 1) {
          val currentBlockX = startX + col
          val currentBlockY = startY + row
          // 盤面内に表示される部分のみ描画 (特にY座標が負の場合を考慮)
          if (currentBlockY >= 0) {
            val rect = new Rectangle {
              x = currentBlockX * CellSize
              y = currentBlockY * CellSize
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

    // 4. ゲームオーバーテキストを最後に描画 (他のすべての上に)
    if (isGameOver && txtGameOver.visible.value) {
      // layoutX は TetrisUI 側で設定済みと仮定
      // txtGameOver.layoutX = (BoardWidth - txtGameOver.boundsInLocal.value.getWidth) / 2 // 動的計算は避ける
      if (!pane.children.contains(txtGameOver)) { // 既に追加されていなければ追加
        pane.children.add(txtGameOver)
      }
      txtGameOver.toFront() // gamePane の中で最前面に
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
