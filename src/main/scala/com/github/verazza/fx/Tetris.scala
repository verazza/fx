package fx

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.layout.Pane
import scalafx.scene.shape.Line

object Tetris extends JFXApp3 {
  val numCols = 10
  val numRows = 20
  val cellSize = 30 // 各セルのサイズ (ピクセル)
  val boardWidth = numCols * cellSize
  val boardHeight = numRows * cellSize

  val iShape: Array[Array[Int]] = Array(
    Array(0, 0, 0, 0),
    Array(1, 1, 1, 1),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val shapeColors = Map(
    "I" -> Color.Cyan
  )

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title.value = "ScalaFX Tetris"
      scene = new Scene(boardWidth, boardHeight) {
        fill = Color.LightGray
        val gamePane = new Pane {
          val currentTetrominoShape: Array[Array[Int]] = iShape
          val color = shapeColors("I")
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
