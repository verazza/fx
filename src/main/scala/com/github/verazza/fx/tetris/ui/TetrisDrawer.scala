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
    holdDisplayPane: Pane // ホールドミノ表示用のPane
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
    holdDisplayPane.children.clear() // ホールド表示エリアをクリア
    // ホールドエリアにもグリッドを描画 (任意)
    drawMiniGrid(
      holdDisplayPane,
      TetrisUI.HoldPaneCellWidth,
      TetrisUI.HoldPaneCellHeight
    )

    heldTetrominoOpt.foreach { heldMino =>
      val shape = heldMino.currentShape // ホールドミノは常に初期形状・向きで表示
      val color = heldMino.color
      // ホールド表示エリアの中央に描画するためのオフセット計算
      // ミノの実際の幅と高さを計算 (4x4の枠内のどこにブロックがあるか)
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

      // ホールドペインの中央に配置するためのオフセット
      val offsetX =
        ((TetrisUI.HoldPaneCellWidth - shapeActualWidth) / 2.0 - minCol).toInt
      val offsetY =
        ((TetrisUI.HoldPaneCellHeight - shapeActualHeight) / 2.0 - minRow).toInt

      for (r <- shape.indices; c <- shape(r).indices) {
        if (shape(r)(c) == 1) {
          val rect = new Rectangle {
            x = (c + offsetX) * CellSize
            y = (r + offsetY) * CellSize
            width = CellSize
            height = CellSize
            fill = color
            stroke = Color.DimGray // 少し薄い枠線
            strokeWidth = 1
          }
          // 描画範囲チェック (ホールドペインからはみ出ないように)
          if (
            rect.x.value >= 0 && rect.x.value + CellSize <= holdDisplayPane.prefWidth.value &&
            rect.y.value >= 0 && rect.y.value + CellSize <= holdDisplayPane.prefHeight.value
          ) {
            holdDisplayPane.children += rect
          }
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
