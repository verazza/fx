package fx

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Line
import scalafx.scene.layout.Pane

object Tetris extends JFXApp3 {
  val numCols = 10
  val numRows = 20
  val cellSize = 30
  val boardWidth = numCols * cellSize
  val boardHeight = numRows * cellSize

  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title.value = "ScalaFX Tetris"
      scene = new Scene(boardWidth, boardHeight) {
        fill = Color.LightGray
        val gridLines = new Pane {
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
        content = gridLines
      }
    }
  }
}
