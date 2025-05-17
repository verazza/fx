package fx.tetris.ui

import scalafx.scene.layout.Pane
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Line, Rectangle}
import scalafx.scene.text.Text
import fx.tetris.logic.{FallingTetromino, GameConstants}

object TetrisDrawer {

  import GameConstants._

  def drawGameUI(
    pane: Pane, // メインのゲーム盤面用Pane
    fallingTetromino: FallingTetromino,
    currentBoard: Array[Array[Option[Color]]],
    isGameOver: Boolean,
    txtGameOver: Text,
    heldTetrominoOpt: Option[FallingTetromino], // ホールドされているミノ
    holdDisplayPane: Pane, // ホールドミノ表示用のPane
    nextTetrominos: Seq[FallingTetromino], // Nextミノのシーケンス
    nextDisplayPanes: Seq[Pane] // Nextミノ表示用のPaneのシーケンス
  ): Unit = {
    // 1. メインゲーム盤面の描画
    pane.children.clear()
    drawGridUI(pane)

    for (r <- 0 until NumRows; c <- 0 until NumCols) {
      currentBoard(r)(c).foreach { color =>
        val rect = new Rectangle {
          x = c * CellSize; y = r * CellSize; width = CellSize;
          height = CellSize; fill = color; stroke = Color.Black;
          strokeWidth = 0.5
        }
        pane.children += rect
      }
    }

    if (!isGameOver) {
      val shape = fallingTetromino.currentShape
      val color = fallingTetromino.color
      val startX = fallingTetromino.x
      val startY = fallingTetromino.y
      for (row <- shape.indices; col <- shape(row).indices) {
        if (shape(row)(col) == 1) {
          val currentBlockX = startX + col
          val currentBlockY = startY + row
          if (currentBlockY >= 0) {
            val rect = new Rectangle {
              x = currentBlockX * CellSize; y = currentBlockY * CellSize;
              width = CellSize; height = CellSize; fill = color;
              stroke = Color.Black; strokeWidth = 1
            }
            pane.children += rect
          }
        }
      }
    }

    if (isGameOver && txtGameOver.visible.value) {
      if (!pane.children.contains(txtGameOver)) pane.children.add(txtGameOver)
      txtGameOver.toFront()
    }

    // 2. ホールドミノの描画
    holdDisplayPane.children.clear()
    drawMiniGrid(
      holdDisplayPane,
      TetrisUI.HoldPaneCellWidth,
      TetrisUI.HoldPaneCellHeight
    )
    heldTetrominoOpt.foreach { heldMino =>
      drawTetrominoInMiniPane(
        holdDisplayPane,
        heldMino,
        TetrisUI.HoldPaneCellWidth,
        TetrisUI.HoldPaneCellHeight
      )
    }

    // 3. Nextミノの描画
    for ((nextMino, nextPane) <- nextTetrominos.zip(nextDisplayPanes)) {
      nextPane.children.clear()
      drawMiniGrid(
        nextPane,
        TetrisUI.NextPaneCellWidth,
        TetrisUI.NextPaneCellHeight
      )
      drawTetrominoInMiniPane(
        nextPane,
        nextMino,
        TetrisUI.NextPaneCellWidth,
        TetrisUI.NextPaneCellHeight
      )
    }
  }

  // 小さなペインにテトリミノを描画するヘルパーメソッド
  private def drawTetrominoInMiniPane(
    miniPane: Pane, // ★ 引数名は miniPane
    tetromino: FallingTetromino,
    paneCellWidth: Int,
    paneCellHeight: Int
  ): Unit = {
    val shape = tetromino.currentShape
    val color = tetromino.color

    var minCol = shape(0).length; var maxCol = -1
    var minRow = shape.length; var maxRow = -1
    for (r <- shape.indices; c <- shape(r).indices) {
      if (shape(r)(c) == 1) {
        minCol = Math.min(minCol, c); maxCol = Math.max(maxCol, c)
        minRow = Math.min(minRow, r); maxRow = Math.max(maxRow, r)
      }
    }
    val shapeActualWidth = if (maxCol >= minCol) maxCol - minCol + 1 else 0
    val shapeActualHeight = if (maxRow >= minRow) maxRow - minRow + 1 else 0

    // offsetX と offsetY の計算は元のままで良い (paneCellWidth/Height を使う)
    // val offsetX = ((paneCellWidth - shapeActualWidth) / 2.0 - minCol).toInt
    // val offsetY = ((paneCellHeight - shapeActualHeight) / 2.0 - minRow).toInt

    val miniCellSize = CellSize * 0.8

    for (r <- shape.indices; c <- shape(r).indices) {
      if (shape(r)(c) == 1) {
        val rect = new Rectangle {
          // シンプルな中央寄せ (ペインサイズとミノの実際のピクセル幅から)
          // ★★★ 修正箇所: pane を miniPane に変更 ★★★
          this.x =
            (miniPane.prefWidth.value - shapeActualWidth * miniCellSize) / 2 + (c - minCol) * miniCellSize
          this.y =
            (miniPane.prefHeight.value - shapeActualHeight * miniCellSize) / 2 + (r - minRow) * miniCellSize

          width = miniCellSize
          height = miniCellSize
          fill = color
          stroke = Color.DimGray
          strokeWidth = 1
        }
        // 描画範囲チェック (miniPaneからはみ出ないように)
        if (
          rect.x.value >= 0 && rect.x.value + miniCellSize <= miniPane.prefWidth.value &&
          rect.y.value >= 0 && rect.y.value + miniCellSize <= miniPane.prefHeight.value
        ) {
          miniPane.children += rect
        }
      }
    }
  }
  private def drawMiniGrid(pane: Pane, cols: Int, rows: Int): Unit = {
    for (i <- 0 to cols) {
      pane.children += new Line {
        startX = i * CellSize; startY = 0
        endX = i * CellSize; endY = rows * CellSize
        stroke = Color.rgb(80, 80, 80); strokeWidth = 0.5
      }
    }
    for (i <- 0 to rows) {
      pane.children += new Line {
        startX = 0; startY = i * CellSize
        endX = cols * CellSize; endY = i * CellSize
        stroke = Color.rgb(80, 80, 80); strokeWidth = 0.5
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
