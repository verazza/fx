package fx

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle

object Tetris extends JFXApp3 {
  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title.value = "ScalaFX Tetris"
      scene = new Scene(300, 400) {
        fill = Color.LightGray
        content = new Rectangle {
          x = 50
          y = 50
          width = 100
          height = 100
          fill = Color.Red
        }
      }
    }
  }
}
