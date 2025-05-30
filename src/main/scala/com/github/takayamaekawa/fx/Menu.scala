package fx

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.control.Button
import scalafx.scene.text.Text
import scalafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.scene.paint.Color // Color.White などで使用
import scalafx.event.ActionEvent
import scalafx.Includes._
import fx.tetris.ui.TetrisUI
import scalafx.stage.Screen
import scalafx.geometry.Pos

object Menu extends JFXApp3 with Gaming {
  override def start(): Unit = {
    stage = new JFXApp3.PrimaryStage {
      title.value = menuTitle
      // シーンのサイズは維持しつつ、fill は透明にするか、VBoxの背景に合わせる
      scene = new Scene(350, 550) { // 少しサイズを大きくしてパディングを見栄え良くする案
        fill = Color.rgb(40, 40, 40) // VBoxの背景に似た暗い色にするか、Transparent

        val welcomeText = new Text {
          text = menuTitle
          style = "-fx-font-size: 24pt; -fx-fill: white;" // テキスト色を白に変更
          // 必要であれば -fx-font-family も指定
        }

        val tetrisButton = new Button {
          text = tetrisButtonText
          style =
            "-fx-font-size: 14pt; -fx-base: #4CAF50; -fx-text-fill: white;" // ボタンのスタイルも少し変更
          prefWidth = 200 // ボタンの幅を統一
          onAction = (event: ActionEvent) => TetrisUI.getStage().show()
        }

        // デバッグボタン用のスタイル
        val debugButtonStyle =
          "-fx-font-size: 11pt; -fx-base: #f0ad4e; -fx-text-fill: white;"
        val debugButtonPrefWidth = 250 // デバッグボタンの幅

        val debugButtons = List[Button](
          new Button {
            text = "テトリスを新規ウィンドウで立ち上げる(×6)"
            style = debugButtonStyle
            prefWidth = debugButtonPrefWidth
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
                val tetrisStage = TetrisUI.getStage(i + 1)
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
            text = "未設定1" // テキストを少し具体的に
            style = debugButtonStyle
            prefWidth = debugButtonPrefWidth
            onAction = (event: ActionEvent) => {}
          },
          new Button {
            text = "未設定2" // テキストを少し具体的に
            style = debugButtonStyle
            prefWidth = debugButtonPrefWidth
            onAction = (event: ActionEvent) => {}
          }
        )

        val debugSubLayout = new VBox {
          spacing = 10
          children = debugButtons
          visible = false
          managed <== visible
          alignment = Pos.Center
        }

        val debugStartButton: Button = new Button {
          text = "▼デバッグオプション ▼"
          style =
            "-fx-font-size: 12pt; -fx-base: #5bc0de; -fx-text-fill: white;" // スタイル変更
          prefWidth = 200
          onAction = (event: ActionEvent) => {
            debugSubLayout.visible = !debugSubLayout.visible.value
            if (debugSubLayout.visible.value) {
              text = "▲デバッグオプションを隠す ▲"
            } else {
              text = "▼デバッグオプション ▼"
            }
          }
        }

        val layout = new VBox {
          padding = Insets(30) // パディングを少し増やす
          spacing = 20 // 要素間のスペースを増やす
          children =
            Seq(welcomeText, tetrisButton, debugStartButton, debugSubLayout)
          alignment = Pos.Center
          // ★★★ VBoxにスタイルを適用 ★★★
          style = """
            -fx-background-color: rgba(70, 70, 90, 0.9);
            -fx-background-radius: 20;
            -fx-border-color: rgba(150, 150, 180, 0.5);
            -fx-border-width: 2;
            -fx-border-radius: 18;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0.5, 0, 0);
          """.stripMargin // 少し濃いめの青紫系背景に、ドロップシャドウを追加
        }
        // VBox がシーン全体を覆うように設定 (任意)
        // VBox.setVgrow(layout, Priority.Always) // VBoxを縦に引き伸ばす場合

        content = layout // Sceneのcontentを直接VBoxに
      }
    }
  }
}
