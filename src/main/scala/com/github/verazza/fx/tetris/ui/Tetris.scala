// src/main/scala/com/github/verazza/fx/tetris/ui/TetrisUI.scala
package fx.tetris.ui

import scalafx.Includes._
import scalafx.application.JFXApp3 // Mainで使うのでここでは不要かも
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.layout.Pane
import scalafx.scene.shape.Line
import scalafx.stage.Stage
import fx.tetris.logic.{
  TetrisGameLogic,
  FallingTetromino,
  GameConstants
} // FallingTetromino も logic にある前提
import scalafx.animation.AnimationTimer
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.text.Text
import scala.collection.mutable

object TetrisUI { // object または class でUIを構築するメソッドを提供する

  import GameConstants._ // 定数をインポート

  def getStage(id: Int = 0): Stage = {
    val gameLogic = new TetrisGameLogic() // ゲームロジックのインスタンスを作成
    gameLogic.resetGame() // ゲーム状態を初期化

    // UI要素
    val gameOverText = new Text {
      text = "GAME OVER"
      style =
        "-fx-font-size: 40pt; -fx-fill: red; -fx-stroke: black; -fx-stroke-width: 1;"
      layoutX = BoardWidth / 2 - 120
      layoutY = BoardHeight / 2
      visible = false
    }

    // キー入力状態
    val keysPressed = mutable.Set[KeyCode]()
    var continuousMoveDirection: Option[Int] = None
    var moveKeyDownTime = 0L
    var lastMoveTime = 0L

    new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(BoardWidth, BoardHeight) {
        fill = Color.LightGray
        val gamePane = new Pane
        gamePane.children.add(gameOverText)

        // 初期描画
        drawGameUI(
          gamePane,
          gameLogic.currentFallingTetromino,
          gameLogic.board,
          gameLogic.gameOver,
          gameOverText
        )

        val timer = AnimationTimer { now =>
          if (!gameLogic.gameOver) {
            // 押しっぱなしキーの処理 (左右移動DAS)
            continuousMoveDirection.foreach { dir =>
              if (moveKeyDownTime > 0 && (now - moveKeyDownTime > DasDelay)) {
                if (now - lastMoveTime > ArrInterval) {
                  if (gameLogic.tryMoveHorizontal(dir)) { // 移動できたら描画更新のトリガー
                    // 描画はタイマーの最後にまとめて行う
                  }
                  lastMoveTime = now
                }
              }
            }
            // ソフトドロップ状態の更新
            gameLogic.setSoftDropActive(keysPressed.contains(KeyCode.Down))

            // 時間経過によるゲーム進行
            gameLogic.updateGameTick(now) // ゲーム状態を更新

            // 描画
            drawGameUI(
              gamePane,
              gameLogic.currentFallingTetromino,
              gameLogic.board,
              gameLogic.gameOver,
              gameOverText
            )

          } else { // ゲームオーバー時の処理
            if (!gameOverText.visible.value) {
              gameOverText.visible = true
              gameOverText.toFront()
              drawGameUI(
                gamePane,
                gameLogic.currentFallingTetromino,
                gameLogic.board,
                gameLogic.gameOver,
                gameOverText
              )
            }
          }
        }
        timer.start()

        onKeyPressed = (event: KeyEvent) => {
          if (!gameLogic.gameOver) {
            var needsRedraw = false
            if (!keysPressed.contains(event.code)) { // 新しく押されたキー
              event.code match {
                case KeyCode.Up =>
                  if (gameLogic.tryRotate(clockwise = true)) needsRedraw = true
                case KeyCode.Control =>
                  if (gameLogic.tryRotate(clockwise = false)) needsRedraw = true
                case KeyCode.Space =>
                  gameLogic.performHardDrop()
                  needsRedraw = true // ハードドロップ後は必ず再描画
                case KeyCode.Left =>
                  if (gameLogic.tryMoveHorizontal(-1)) {
                    moveKeyDownTime = System.nanoTime()
                    continuousMoveDirection = Some(-1)
                    lastMoveTime = System.nanoTime()
                    needsRedraw = true
                  }
                case KeyCode.Right =>
                  if (gameLogic.tryMoveHorizontal(1)) {
                    moveKeyDownTime = System.nanoTime()
                    continuousMoveDirection = Some(1)
                    lastMoveTime = System.nanoTime()
                    needsRedraw = true
                  }
                case _ =>
              }
            }
            keysPressed += event.code

            // ソフトドロップ状態の即時反映 (キーを押した瞬間から)
            if (event.code == KeyCode.Down) {
              gameLogic.setSoftDropActive(true)
              // gameLogic.updateGameTick(System.nanoTime()) // 即座に1マス落とす場合
              needsRedraw = true
            }

            if (needsRedraw) { // 状態変更があった場合に描画
              drawGameUI(
                gamePane,
                gameLogic.currentFallingTetromino,
                gameLogic.board,
                gameLogic.gameOver,
                gameOverText
              )
            }
          }
        }

        onKeyReleased = (event: KeyEvent) => {
          keysPressed -= event.code
          if (event.code == KeyCode.Down) {
            gameLogic.setSoftDropActive(false)
          }
          if (
            (event.code == KeyCode.Left && continuousMoveDirection.contains(
              -1
            )) ||
            (event.code == KeyCode.Right && continuousMoveDirection.contains(1))
          ) {
            continuousMoveDirection = None
            moveKeyDownTime = 0L
          }
        }
        content = gamePane
      }
    }
  }

  // 描画専門のメソッド
  private def drawGameUI(
    pane: Pane,
    fallingTetromino: FallingTetromino, // logic.FallingTetromino を受け取る
    currentBoard: Array[Array[Option[Color]]],
    isGameOver: Boolean,
    txtGameOver: Text // UI要素も引数で渡す
  ): Unit = {
    pane.children.clear()
    drawGridUI(pane)
    pane.children.add(txtGameOver)

    for (r <- 0 until NumRows; c <- 0 until NumCols) {
      currentBoard(r)(c).foreach { color =>
        val rect = new Rectangle {
          x = c * CellSize; y = r * CellSize
          width = CellSize; height = CellSize
          fill = color
          stroke = Color.Black; strokeWidth = 0.5
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
          val rect = new Rectangle {
            x = (startX + col) * CellSize; y = (startY + row) * CellSize
            width = CellSize; height = CellSize
            fill = color
            stroke = Color.Black; strokeWidth = 1
          }
          pane.children += rect
        }
      }
    }

    if (isGameOver) {
      if (!txtGameOver.visible.value) txtGameOver.visible = true
      txtGameOver.toFront()
    } else {
      if (txtGameOver.visible.value) txtGameOver.visible = false
    }
  }

  private def drawGridUI(pane: Pane): Unit = {
    for (i <- 0 to NumCols) {
      pane.children += new Line {
        startX = i * CellSize; startY = 0
        endX = i * CellSize; endY = BoardHeight
        stroke = Color.DarkGray; strokeWidth = 0.5
      }
    }
    for (i <- 0 to NumRows) {
      pane.children += new Line {
        startX = 0; startY = i * CellSize
        endX = BoardWidth; endY = i * CellSize
        stroke = Color.DarkGray; strokeWidth = 0.5
      }
    }
  }
}
