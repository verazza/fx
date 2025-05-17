package fx.tetris.ui

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.paint.Color // Color は TetrisDrawer でも使うので注意
import scalafx.scene.layout.{Pane, StackPane, VBox}
import scalafx.scene.control.Button
import scalafx.geometry.{Insets, Pos}
import scalafx.stage.Stage
import fx.tetris.logic.{
  TetrisGameLogic,
  GameConstants
} // FallingTetromino は GameLogic 経由でアクセス
import scalafx.animation.{AnimationTimer, KeyFrame, Timeline}
import scalafx.util.Duration
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.text.Text
import scalafx.application.Platform

import scala.collection.mutable

object TetrisUI {

  import GameConstants._
  // TetrisDrawer をインポートするか、完全修飾名で呼び出す。ここではインポートを推奨。
  import TetrisDrawer._

  private var animationTimerInstance: Option[AnimationTimer] = None
  private var isPaused: Boolean = false
  private var gameOverDisplayTimeline: Option[Timeline] = None

  def getStage(id: Int = 0): Stage = {
    val gameLogic = new TetrisGameLogic()
    gameLogic.resetGame()
    isPaused = false

    val keysPressed = mutable.Set[KeyCode]()
    var continuousMoveDirection: Option[Int] = None
    var moveKeyDownTime = 0L
    var lastMoveTime = 0L

    // --- UI要素の作成 ---
    val gameOverText = new Text {
      text = "GAME OVER"
      style =
        "-fx-font-size: 40pt; -fx-fill: red; -fx-stroke: black; -fx-stroke-width: 1;"
      layoutX = BoardWidth / 2 - 120
      layoutY = BoardHeight / 3
      visible = false
    }

    val scoreText = new Text {
      style = "-fx-font-size: 16pt; -fx-fill: white;"
      visible = false
    }

    // ポーズメニューボタン
    val resumeButtonP = new Button("ゲームに戻る (Esc)") {
      prefWidth = 200; style = "-fx-font-size: 14pt;"
    }
    val restartButtonP = new Button("最初からやり直す") {
      prefWidth = 200; style = "-fx-font-size: 14pt;"
    }
    val backToMenuButtonP = new Button("メニュー画面に戻る") {
      prefWidth = 200; style = "-fx-font-size: 14pt;"
    }
    val quitGameButtonP = new Button("ゲームを終了する") {
      prefWidth = 200; style = "-fx-font-size: 14pt;"
    }

    val pauseMenuPane = new VBox {
      spacing = 15; alignment = Pos.Center; padding = Insets(30)
      style =
        "-fx-background-color: rgba(50, 50, 50, 0.85); -fx-background-radius: 15; -fx-border-color: silver; -fx-border-width: 2; -fx-border-radius: 13;"
      visible = false; maxWidth = 250
      children = Seq(
        new Text("ポーズ中") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
        resumeButtonP,
        restartButtonP,
        backToMenuButtonP,
        quitGameButtonP
      )
    }

    // ゲームオーバーメニューボタン
    val restartButtonGO = new Button("もう一度プレイ") {
      prefWidth = 220; style = "-fx-font-size: 14pt;"
    }
    val backToMenuButtonGO = new Button("メインメニューに戻る") {
      prefWidth = 220; style = "-fx-font-size: 14pt;"
    }
    val quitGameButtonGO = new Button("ゲーム終了") {
      prefWidth = 220; style = "-fx-font-size: 14pt;"
    }

    val gameOverMenuPane = new VBox {
      spacing = 15; alignment = Pos.Center; padding = Insets(30)
      style =
        "-fx-background-color: rgba(70, 0, 0, 0.9); -fx-background-radius: 15; -fx-border-color: darkred; -fx-border-width: 2; -fx-border-radius: 13;"
      visible = false; maxWidth = 280
      children = Seq(
        new Text("結果") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
        scoreText,
        restartButtonGO,
        backToMenuButtonGO,
        quitGameButtonGO
      )
    }

    val gamePane = new Pane
    // gameOverText は gamePane の子として追加し、表示/非表示は drawGameUI 内で制御する
    // gamePane.children.add(gameOverText) // 初期追加は drawGameUI に任せる

    val rootPane = new StackPane {
      children = Seq(gamePane, pauseMenuPane, gameOverMenuPane)
      style = "-fx-background-color: LightGray;"
    }

    val currentStage = new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(BoardWidth, BoardHeight) {
        content = rootPane

        // 初期描画
        drawGameUI(
          gamePane,
          gameLogic.currentFallingTetromino,
          gameLogic.board,
          gameLogic.gameOver,
          gameOverText
        )

        val timer = AnimationTimer { now =>
          if (gameLogic.gameOver) {
            if (!gameOverMenuPane.visible.value) {
              animationTimerInstance.foreach(_.stop())
              gameOverText.visible = true // GAME OVER テキスト自体は表示状態にする
              startGameOverTextAnimation(gameOverText)
              scoreText.text = s"スコア: ${gameLogic.score}"
              scoreText.visible = true
              gameOverMenuPane.visible = true
              gameOverMenuPane.toFront()
              // ゲームオーバー確定時の最終描画
              drawGameUI(
                gamePane,
                gameLogic.currentFallingTetromino,
                gameLogic.board,
                gameLogic.gameOver,
                gameOverText
              )
            }
          } else if (!isPaused) {
            continuousMoveDirection.foreach { dir =>
              if (moveKeyDownTime > 0 && (now - moveKeyDownTime > DasDelay)) {
                if (now - lastMoveTime > ArrInterval) {
                  gameLogic.tryMoveHorizontal(dir); lastMoveTime = now
                }
              }
            }
            gameLogic.setSoftDropActive(keysPressed.contains(KeyCode.Down))

            val gameUpdated = gameLogic.updateGameTick(now) // ロック遅延処理も含む

            // 状態が更新されたか、または一定間隔で描画 (ここでは毎フレーム描画)
            // if (gameUpdated) { // 状態更新があった時だけ描画する場合
            drawGameUI(
              gamePane,
              gameLogic.currentFallingTetromino,
              gameLogic.board,
              gameLogic.gameOver,
              gameOverText
            )
            // }
          }
        }
        animationTimerInstance = Some(timer)
        timer.start()

        onKeyPressed = (event: KeyEvent) => {
          if (gameLogic.gameOver) {
            // ゲームオーバー時はキー入力無効 (メニュー操作はマウスのみ)
          } else {
            event.code match {
              case KeyCode.Escape =>
                togglePause(pauseMenuPane, gameLogic)
              case _ =>
                if (!isPaused) {
                  var needsRedraw = false
                  if (
                    !keysPressed.contains(
                      event.code
                    ) || event.code == KeyCode.Down
                  ) {
                    event.code match {
                      case KeyCode.Up =>
                        if (gameLogic.tryRotate(clockwise = true))
                          needsRedraw = true
                      case KeyCode.Control =>
                        if (gameLogic.tryRotate(clockwise = false))
                          needsRedraw = true
                      case KeyCode.Space =>
                        gameLogic.performHardDrop(); needsRedraw = true
                      case KeyCode.Left =>
                        if (gameLogic.tryMoveHorizontal(-1)) {
                          moveKeyDownTime = System.nanoTime();
                          continuousMoveDirection = Some(-1);
                          lastMoveTime = System.nanoTime(); needsRedraw = true
                        }
                      case KeyCode.Right =>
                        if (gameLogic.tryMoveHorizontal(1)) {
                          moveKeyDownTime = System.nanoTime();
                          continuousMoveDirection = Some(1);
                          lastMoveTime = System.nanoTime(); needsRedraw = true
                        }
                      case KeyCode.Down =>
                        gameLogic.setSoftDropActive(true); needsRedraw = true
                      case _ =>
                    }
                  }
                  if (!keysPressed.contains(event.code))
                    keysPressed += event.code

                  if (needsRedraw) {
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
          }
        }
        onKeyReleased = (event: KeyEvent) => {
          keysPressed -= event.code
          if (!gameLogic.gameOver && !isPaused) {
            if (event.code == KeyCode.Down) {
              gameLogic.setSoftDropActive(false)
            }
            if (
              (event.code == KeyCode.Left && continuousMoveDirection.contains(
                -1
              )) ||
              (event.code == KeyCode.Right && continuousMoveDirection.contains(
                1
              ))
            ) {
              continuousMoveDirection = None; moveKeyDownTime = 0L
            }
            // キーを離した際も描画更新が必要な場合がある (例: ソフトドロップ解除)
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
    }

    // --- ポーズメニューボタンのアクション設定 ---
    resumeButtonP.onAction = () => togglePause(pauseMenuPane, gameLogic)
    restartButtonP.onAction = () => {
      stopGameOverTextAnimation()
      gameLogic.resetGame()
      keysPressed.clear(); continuousMoveDirection = None; moveKeyDownTime = 0L
      if (isPaused) togglePause(pauseMenuPane, gameLogic)
      else animationTimerInstance.foreach(_.start())
      gameOverMenuPane.visible = false
      gameOverText.visible = false // リスタート時にGAME OVERテキストも消す
      drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText
      )
    }
    backToMenuButtonP.onAction = () => {
      animationTimerInstance.foreach(_.stop()); stopGameOverTextAnimation()
      currentStage.close()
    }
    quitGameButtonP.onAction = () => {
      animationTimerInstance.foreach(_.stop()); stopGameOverTextAnimation()
      Platform.exit()
    }

    // --- ゲームオーバーメニューボタンのアクション設定 ---
    restartButtonGO.onAction = () => {
      stopGameOverTextAnimation()
      gameLogic.resetGame()
      keysPressed.clear(); continuousMoveDirection = None; moveKeyDownTime = 0L
      gameOverMenuPane.visible = false
      gameOverText.visible = false
      isPaused = false
      animationTimerInstance.foreach(_.start())
      drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText
      )
    }
    backToMenuButtonGO.onAction = () => {
      animationTimerInstance.foreach(_.stop()); stopGameOverTextAnimation()
      currentStage.close()
    }
    quitGameButtonGO.onAction = () => {
      animationTimerInstance.foreach(_.stop()); stopGameOverTextAnimation()
      Platform.exit()
    }

    currentStage.onCloseRequest = () => {
      animationTimerInstance.foreach(_.stop()); stopGameOverTextAnimation()
      println(s"Tetris stage (id: $id) closed.")
    }
    currentStage
  }

  private def togglePause(pauseMenu: VBox, gameLogic: TetrisGameLogic): Unit = {
    if (gameLogic.gameOver) return

    isPaused = !isPaused
    pauseMenu.visible = isPaused
    if (isPaused) {
      pauseMenu.toFront()
      animationTimerInstance.foreach(_.stop())
    } else {
      gameLogic.lastFallTime = System.nanoTime()
      animationTimerInstance.foreach(_.start())
    }
  }

  private def startGameOverTextAnimation(textNode: Text): Unit = {
    stopGameOverTextAnimation()
    val timeline = new Timeline {
      cycleCount = Timeline.Indefinite
      autoReverse = true
      keyFrames = Seq(
        KeyFrame(Duration(0), values = Set(textNode.opacity -> 1.0)),
        KeyFrame(Duration(500), values = Set(textNode.opacity -> 0.3))
      )
    }
    timeline.play()
    gameOverDisplayTimeline = Some(timeline)
  }

  private def stopGameOverTextAnimation(): Unit = {
    gameOverDisplayTimeline.foreach(_.stop())
    gameOverDisplayTimeline = None
  }

  // drawGameUI と drawGridUI は TetrisDrawer に移動したので、ここでは呼び出しのみ
  // これらのメソッド定義は TetrisUI.scala から削除される
}
