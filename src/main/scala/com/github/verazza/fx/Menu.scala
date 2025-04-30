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
import scalafx.stage.Screen

object Menu extends JFXApp3 with Gaming {
  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title.value = menuTitle
      scene = new Scene(300, 500) {
        fill = Color.LightGreen
        val welcomeText = new Text {
          text = menuTitle
          style = "-fx-font-size: 20pt"
        }

        val tetrisButton = new Button {
          text = tetrisButtonText
          onAction = (event: ActionEvent) => Tetris.getStage().show()
        }

        val debugButtons = List[Button](
          new Button {
            text = "テトリスを新規ウィンドウで立ち上げる(×6)"
            onAction = (event: ActionEvent) => {
              val screenBounds = Screen.primary.getVisualBounds
              val screenWidth = screenBounds.getWidth
              val screenHeight = screenBounds.getHeight

              val screenCenterX = screenWidth / 2
              val screenCenterY = screenHeight / 2

              val windowWidth = 250
              val windowHeight = 350

              val horizontalPadding = 30
              val verticalPadding = 30

              val gridCols = 3
              val gridRows = 2

              val totalGridWidth =
                gridCols * windowWidth + (gridCols - 1) * horizontalPadding
              val totalGridHeight =
                gridRows * windowHeight + (gridRows - 1) * verticalPadding

              val gridStartX = screenCenterX - totalGridWidth / 2
              val gridStartY = screenCenterY - totalGridHeight / 2

              for (i <- 0 to 5) {
                val tetrisStage = Tetris.getStage(i + 1)

                val row = i / gridCols
                val col = i % gridCols

                val posX = gridStartX + col * (windowWidth + horizontalPadding)
                val posY = gridStartY + row * (windowHeight + verticalPadding)

                tetrisStage.setX(posX)
                tetrisStage.setY(posY)

                tetrisStage.show()
              }
            }
          },
          new Button {
            text = "未設定"
            onAction = (event: ActionEvent) => {}
          },
          new Button {
            text = "未設定"
            onAction = (event: ActionEvent) => {}
          }
        )

        val debugStartButton: Button = new Button {
          text = "▼デバッグオプション ▼"
          onAction = (event: ActionEvent) => {
            for (debugButton <- debugButtons) {
              layout.children.add(debugButton)
            }
          }
        }

        val layout = new VBox {
          padding = Insets(20)
          spacing = 10
          children = Seq(welcomeText, tetrisButton, debugStartButton)
          alignment = scalafx.geometry.Pos.CENTER
        }
        content = layout
      }
    }
  }
}
