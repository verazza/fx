package fx.tetris.ui

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.layout.Pane
import scalafx.scene.shape.Line
import scalafx.stage.Stage
import fx.tetris.logic.{Tetromino, FallingTetromino}
import scalafx.animation.AnimationTimer

object Tetris {
  val numCols = 10
  val numRows = 20
  val cellSize = 30
  val boardWidth = numCols * cellSize
  val boardHeight = numRows * cellSize

  def getStage(id: Int = 0): Stage = {
    new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(boardWidth, boardHeight) {
        fill = Color.LightGray
        val gamePane = new Pane

        val tetrominoGetter = new Tetromino.Getter
        val currentFallingTetromino: FallingTetromino =
          new FallingTetromino(
            tetrominoGetter.getMino(),
            tetrominoGetter.getColor()
          )
        currentFallingTetromino.x = numCols / 2 - 2
        currentFallingTetromino.y = 0

        drawGame(gamePane, currentFallingTetromino)

        var lastFallTime = 0L

        val timer = AnimationTimer { _ =>
          val now = System.nanoTime()
          val interval = 1_000_000_000L
          val fallInterval = interval

          if (now - lastFallTime >= fallInterval) {
            currentFallingTetromino.moveDown()
            drawGame(gamePane, currentFallingTetromino)
            lastFallTime = now
          }
        }
        timer.start()

        drawGrid(gamePane)

        content = gamePane
      }
    }
  }

  private def drawGame(pane: Pane, tetromino: FallingTetromino): Unit = {
    pane.children.clear()
    drawGrid(pane)

    val shape = tetromino.shape
    val color = tetromino.color
    val startX = tetromino.x
    val startY = tetromino.y

    for (
      row <- shape.indices;
      col <- shape(row).indices
    ) {
      if (shape(row)(col) == 1) {
        val rect = new Rectangle {
          x = (startX + col) * cellSize
          y = (startY + row) * cellSize
          width = cellSize
          height = cellSize
          fill = color
          stroke = Color.Black
        }
        pane.children += rect
      }
    }
  }

  private def drawGrid(pane: Pane): Unit = {
    for (i <- 0 to numCols) {
      pane.children += new Line {
        startX = i * cellSize
        startY = 0
        endX = i * cellSize
        endY = boardHeight
        stroke = Color.Gray
      }
    }
    for (i <- 0 to numRows) {
      pane.children += new Line {
        startX = 0
        startY = i * cellSize
        endX = boardWidth
        endY = i * cellSize
        stroke = Color.Gray
      }
    }
  }
}
