package fx

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.text.Text
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.scene.paint.Color
import scalafx.event.ActionEvent
import scalafx.Includes._
import fx.tetris.ui.Tetris

object Menu extends JFXApp3 with Gaming {
  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title.value = menuTitle
      scene = new Scene(300, 200) {
        fill = Color.LightGreen
        val welcomeText = new Text {
          text = menuTitle
          style = "-fx-font-size: 20pt"
        }

        val tetrisButton = new Button {
          text = tetrisButtonText
          onAction = (event: ActionEvent) => {
            Tetris.start()
            // stage.close()
          }
        }

        val layout = new VBox {
          padding = Insets(20)
          spacing = 10
          children = Seq(welcomeText, tetrisButton)
          alignment = scalafx.geometry.Pos.CENTER
        }
        content = layout
      }
    }
  }
}
