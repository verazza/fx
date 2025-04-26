package fx.tetris.ui

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.layout.Pane
import scalafx.scene.shape.Line
import scalafx.stage.Stage
import fx.tetris.logic.Tetromino

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
        val gamePane = new Pane {
          val tm = new Tetromino.Getter
          val currentTetrominoShape = tm.getMino()
          val color = tm.getColor()
          val startX = numCols / 2 - 2
          val startY = 0

          for (
            row <- currentTetrominoShape.indices;
            col <- currentTetrominoShape(row).indices
          ) {
            if (currentTetrominoShape(row)(col) == 1) {
              val rect = new Rectangle {
                x = (startX + col) * cellSize
                y = (startY + row) * cellSize
                width = cellSize
                height = cellSize
                fill = color
                stroke = Color.Black
              }
              children += rect
            }
          }

          for (i <- 0 to numCols) {
            children += new Line {
              startX = i * cellSize
              startY = 0
              endX = i * cellSize
              endY = boardHeight
              stroke = Color.Gray
            }
          }
          for (i <- 0 to numRows) {
            children += new Line {
              startX = 0
              startY = i * cellSize
              endX = boardWidth
              endY = i * cellSize
              stroke = Color.Gray
            }
          }
        }
        content = gamePane
      }
    }
  }
}
