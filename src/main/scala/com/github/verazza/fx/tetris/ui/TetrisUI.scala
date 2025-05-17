package fx.tetris.ui

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.layout.{Pane, StackPane, VBox}
import scalafx.scene.control.Button
import scalafx.geometry.{Insets, Pos}
import scalafx.stage.Stage
import fx.tetris.logic.{
  TetrisGameLogic,
  GameConstants
} // FallingTetromino は GameLogic 経由
import scalafx.animation.{AnimationTimer, KeyFrame, Timeline, PauseTransition}
import scalafx.util.Duration
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.text.Text
import scalafx.application.Platform
import scalafx.scene.paint.Color // TetrisDrawer で使うので、ここでの直接使用は減らす

import scala.collection.mutable

object TetrisUI {

  import GameConstants._
  import TetrisDrawer._ // TetrisDrawer のメソッドを直接呼び出す

  private var animationTimerInstance: Option[AnimationTimer] = None
  private var isPaused: Boolean = false
  private var gameOverDisplayTimeline: Option[Timeline] = None
  private var gameOverMenuDelay: Option[PauseTransition] = None

  def getStage(id: Int = 0): Stage = {
    val gameLogic = new TetrisGameLogic()
    gameLogic.resetGame()
    isPaused = false

    val keysPressed = mutable.Set[KeyCode]()
    var continuousMoveDirection: Option[Int] = None
    var moveKeyDownTime = 0L
    var lastMoveTime = 0L

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

    // --- ポーズメニューUI定義 ---
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

    // --- ゲームオーバーメニューUI定義 ---
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
    val rootPane = new StackPane {
      children = Seq(gamePane, pauseMenuPane, gameOverMenuPane)
      style = "-fx-background-color: LightGray;"
    }

    val currentStage = new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(BoardWidth, BoardHeight) {
        content = rootPane
        TetrisDrawer.drawGameUI(
          gamePane,
          gameLogic.currentFallingTetromino,
          gameLogic.board,
          gameLogic.gameOver,
          gameOverText
        )

        val timer = AnimationTimer { now =>
          if (
            gameLogic.gameOverPendingAnimation && !gameOverMenuPane.visible.value
          ) {
            if (!gameOverText.visible.value) {
              println(
                "[UI Tick] Game Over Detected by UI. Starting animation sequence."
              )
              animationTimerInstance.foreach(_.stop())
              gameOverText.visible = true
              gameOverText.toFront()
              startGameOverTextAnimation(gameOverText)
              TetrisDrawer.drawGameUI(
                gamePane,
                gameLogic.currentFallingTetromino,
                gameLogic.board,
                gameLogic.gameOver,
                gameOverText
              )
            }

            if (gameOverMenuDelay.isEmpty) {
              println("[UI Tick] Scheduling game over menu display.")
              val delay = new PauseTransition(Duration(2000))
              delay.onFinished = () => {
                println(
                  "[UI Tick] PauseTransition finished. Showing game over menu."
                )
                if (gameLogic.gameOver) {
                  scoreText.text = s"スコア: ${gameLogic.score}"
                  scoreText.visible = true
                  gameOverMenuPane.visible = true
                  gameOverMenuPane
                    .toFront() // gameOverMenuPane を StackPane の最前面に
                }
              }
              delay.play()
              gameOverMenuDelay = Some(delay)
            }
          } else if (
            !isPaused && !gameLogic.gameOver && !gameLogic.gameOverPendingAnimation
          ) {
            continuousMoveDirection.foreach { dir =>
              if (moveKeyDownTime > 0 && (now - moveKeyDownTime > DasDelay)) {
                if (now - lastMoveTime > ArrInterval) {
                  gameLogic.tryMoveHorizontal(dir); lastMoveTime = now
                }
              }
            }
            gameLogic.setSoftDropActive(keysPressed.contains(KeyCode.Down))
            val gameUpdated = gameLogic.updateGameTick(now)
            TetrisDrawer.drawGameUI(
              gamePane,
              gameLogic.currentFallingTetromino,
              gameLogic.board,
              gameLogic.gameOver,
              gameOverText
            )
          }
        }
        animationTimerInstance = Some(timer)
        timer.start()

        onKeyPressed = (event: KeyEvent) => {
          if (gameLogic.gameOver) {
            // Game over actions
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
                    TetrisDrawer.drawGameUI(
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
              (event.code == KeyCode.Right && continuousMoveDirection
                .contains(
                  1
                ))
            ) {
              continuousMoveDirection = None; moveKeyDownTime = 0L
            }
            TetrisDrawer.drawGameUI(
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

    // --- ボタンアクション設定 ---
    resumeButtonP.onAction = () => togglePause(pauseMenuPane, gameLogic)
    restartButtonP.onAction = () => {
      cleanUpBeforeRestartOrExit()
      gameLogic.resetGame()
      keysPressed.clear(); continuousMoveDirection = None; moveKeyDownTime = 0L
      if (isPaused) togglePause(pauseMenuPane, gameLogic)
      else animationTimerInstance.foreach(_.start())
      gameOverMenuPane.visible = false
      gameOverText.visible = false
      TetrisDrawer.drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText
      )
    }
    backToMenuButtonP.onAction = () => {
      cleanUpBeforeRestartOrExit()
      currentStage.close()
    }
    quitGameButtonP.onAction = () => {
      cleanUpBeforeRestartOrExit()
      Platform.exit()
    }

    restartButtonGO.onAction = () => {
      cleanUpBeforeRestartOrExit()
      gameLogic.resetGame()
      keysPressed.clear(); continuousMoveDirection = None; moveKeyDownTime = 0L
      gameOverMenuPane.visible = false
      gameOverText.visible = false
      isPaused = false
      animationTimerInstance.foreach(_.start())
      TetrisDrawer.drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText
      )
    }
    backToMenuButtonGO.onAction = () => {
      cleanUpBeforeRestartOrExit()
      currentStage.close()
    }
    quitGameButtonGO.onAction = () => {
      cleanUpBeforeRestartOrExit()
      Platform.exit()
    }

    currentStage.onCloseRequest = () => {
      cleanUpBeforeRestartOrExit()
      println(s"Tetris stage (id: $id) closed.")
    }
    currentStage
  }

  private def cleanUpBeforeRestartOrExit(): Unit = {
    animationTimerInstance.foreach(_.stop())
    stopGameOverTextAnimation()
    gameOverMenuDelay.foreach(_.stop())
    gameOverMenuDelay = None
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
}
