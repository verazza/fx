package fx.tetris.ui

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.layout.{Pane, StackPane, VBox, HBox, Region}
import scalafx.scene.control.Button
import scalafx.geometry.{Insets, Pos}
import scalafx.stage.Stage
import fx.tetris.logic.{TetrisGameLogic, GameConstants, FallingTetromino}
import scalafx.animation.{AnimationTimer, KeyFrame, Timeline, PauseTransition}
import scalafx.util.Duration
import scalafx.scene.input.{
  KeyCode,
  KeyEvent,
  KeyCombination,
  KeyCodeCombination
}
import scalafx.scene.text.Text
import scalafx.application.Platform
import scalafx.scene.paint.Color

import scala.collection.mutable

object TetrisUI {

  import GameConstants._
  import TetrisDrawer._

  private var animationTimerInstance: Option[AnimationTimer] = None
  private var isPaused: Boolean = false
  private var gameOverDisplayTimeline: Option[Timeline] = None
  private var gameOverMenuDelay: Option[PauseTransition] = None

  val HoldPaneCellWidth = 4
  val HoldPaneCellHeight = 2 // Iミノ縦置きも考慮するなら4が良いかも

  val NextPaneCellWidth = 4
  val NextPaneCellHeight = 2 // 各Nextミノ表示エリアの高さ

  def getStage(id: Int = 0): Stage = {
    val gameLogic = new TetrisGameLogic() // resetGame内でNextキューも初期化される
    // gameLogic.resetGame() // TetrisGameLogicのコンストラクタでinitialTetrominoが呼ばれ、その中でresetGame相当の初期化がされるように変更

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

    pauseMenuPane.children = Seq(
      new Text("ポーズ中") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
      resumeButtonP,
      restartButtonP,
      backToMenuButtonP,
      quitGameButtonP
    )
    gameOverMenuPane.children = Seq(
      new Text("結果") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
      scoreText,
      restartButtonGO,
      backToMenuButtonGO,
      quitGameButtonGO
    )

    // --- ホールド表示エリア ---
    val holdPane = new Pane {
      prefWidth = HoldPaneCellWidth * CellSize + 2 // 枠線分
      prefHeight = HoldPaneCellHeight * CellSize + 2
      style =
        "-fx-background-color: rgba(0,0,0,0.3); -fx-border-color: silver; -fx-border-width: 1;"
    }
    val holdText = new Text("HOLD") {
      style = "-fx-font-size: 12pt; -fx-fill: white;"
    }
    val holdArea = new VBox { // テキストとホールドペインをまとめる
      spacing = 5
      alignment = Pos.Center
      children = Seq(holdText, holdPane)
      padding = Insets(10)
    }

    // --- Nextミノ表示エリア ---
    val nextPanes = Seq.fill(NumNextToDisplay)(new Pane {
      prefWidth = NextPaneCellWidth * CellSize + 2
      prefHeight = NextPaneCellHeight * CellSize + 2
      style =
        "-fx-background-color: rgba(0,0,0,0.2); -fx-border-color: silver; -fx-border-width: 1;"
    })
    val nextText = new Text("NEXT") {
      style = "-fx-font-size: 12pt; -fx-fill: white;"
    }
    val nextArea = new VBox {
      spacing = 5
      alignment = Pos.Center
      children = nextText +: nextPanes // テキストと各Nextペイン
      padding = Insets(10)
    }
    // --- Nextミノ表示エリアここまで ---

    val gamePane = new Pane // ゲーム盤面用
    // gamePane.children.add(gameOverText) // gameOverTextはdrawGameUIで管理

    // メインレイアウト: ホールドエリア、ゲーム盤面、(将来的にNextエリア) をHBoxで横に並べる
    val mainLayout = new HBox {
      spacing = 10
      padding = Insets(10)
      alignment = Pos.Center
      children = Seq(
        holdArea,
        gamePane,
        nextArea // Nextエリアを右に追加
      )
    }

    val rootPane = new StackPane {
      children = Seq(
        mainLayout,
        pauseMenuPane,
        gameOverMenuPane
      ) // mainLayout をベースにメニューを重ねる
      style = "-fx-background-color: LightGray;"
    }

    val currentStage = new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      // Sceneのサイズを調整 (BoardWidth + holdArea + nextArea + paddings)
      scene = new Scene(
        BoardWidth + (HoldPaneCellWidth + NextPaneCellWidth) * CellSize + 120,
        BoardHeight + 40
      ) { // 仮サイズ
        content = rootPane
        TetrisDrawer.drawGameUI(
          gamePane,
          gameLogic.currentFallingTetromino,
          gameLogic.board,
          gameLogic.gameOver,
          gameOverText,
          gameLogic.heldTetromino,
          holdPane,
          gameLogic.nextTetrominoQueue.toSeq,
          nextPanes
        )

        val timer = AnimationTimer { now =>
          if (
            gameLogic.gameOverPendingAnimation && !gameOverMenuPane.visible.value
          ) {
            if (!gameOverText.visible.value) {
              animationTimerInstance.foreach(_.stop())
              gameOverText.visible = true
              startGameOverTextAnimation(gameOverText)
              TetrisDrawer.drawGameUI(
                gamePane,
                gameLogic.currentFallingTetromino,
                gameLogic.board,
                gameLogic.gameOver,
                gameOverText,
                gameLogic.heldTetromino,
                holdPane,
                gameLogic.nextTetrominoQueue.toSeq,
                nextPanes
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
              gameOverText,
              gameLogic.heldTetromino,
              holdPane,
              gameLogic.nextTetrominoQueue.toSeq,
              nextPanes
            )

          }
        }
        animationTimerInstance = Some(timer)
        timer.start()

        onKeyPressed = (event: KeyEvent) => {
          if (gameLogic.gameOver) { /* Game over actions */ }
          else {
            event.code match {
              case KeyCode.Escape => togglePause(pauseMenuPane, gameLogic)
              case KeyCode.Shift => // Shiftキーでホールド
                if (!isPaused) {
                  if (gameLogic.performHold()) {
                    TetrisDrawer.drawGameUI(
                      gamePane,
                      gameLogic.currentFallingTetromino,
                      gameLogic.board,
                      gameLogic.gameOver,
                      gameOverText,
                      gameLogic.heldTetromino,
                      holdPane,
                      gameLogic.nextTetrominoQueue.toSeq,
                      nextPanes
                    )
                  }
                }
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
                      gameOverText,
                      gameLogic.heldTetromino,
                      holdPane,
                      gameLogic.nextTetrominoQueue.toSeq,
                      nextPanes
                    )
                  }
                }
            }
          }
        }
        onKeyReleased = (event: KeyEvent) => {
          keysPressed -= event.code
          if (!gameLogic.gameOver && !isPaused) {
            if (event.code == KeyCode.Down) gameLogic.setSoftDropActive(false)
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
            TetrisDrawer.drawGameUI(
              gamePane,
              gameLogic.currentFallingTetromino,
              gameLogic.board,
              gameLogic.gameOver,
              gameOverText,
              gameLogic.heldTetromino,
              holdPane,
              gameLogic.nextTetrominoQueue.toSeq,
              nextPanes
            )
          }
        }
      }
    }

    // ボタンアクション設定 (描画呼び出しにNext情報追加)
    resumeButtonP.onAction = () => togglePause(pauseMenuPane, gameLogic)
    restartButtonP.onAction = () => {
      cleanUpBeforeRestartOrExit()
      gameLogic.resetGame()
      keysPressed.clear(); continuousMoveDirection = None; moveKeyDownTime = 0L
      if (isPaused) togglePause(pauseMenuPane, gameLogic)
      else animationTimerInstance.foreach(_.start())
      gameOverMenuPane.visible = false; gameOverText.visible = false
      TetrisDrawer.drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText,
        gameLogic.heldTetromino,
        holdPane,
        gameLogic.nextTetrominoQueue.toSeq,
        nextPanes
      )
    }
    // ... 他のボタンアクションも同様に描画呼び出しを更新 ...
    backToMenuButtonP.onAction = () => {
      cleanUpBeforeRestartOrExit(); currentStage.close()
    }
    quitGameButtonP.onAction = () => {
      cleanUpBeforeRestartOrExit(); Platform.exit()
    }

    restartButtonGO.onAction = () => {
      cleanUpBeforeRestartOrExit()
      gameLogic.resetGame()
      keysPressed.clear(); continuousMoveDirection = None; moveKeyDownTime = 0L
      gameOverMenuPane.visible = false; gameOverText.visible = false;
      isPaused = false
      animationTimerInstance.foreach(_.start())
      TetrisDrawer.drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText,
        gameLogic.heldTetromino,
        holdPane,
        gameLogic.nextTetrominoQueue.toSeq,
        nextPanes
      )
    }
    backToMenuButtonGO.onAction = () => {
      cleanUpBeforeRestartOrExit(); currentStage.close()
    }
    quitGameButtonGO.onAction = () => {
      cleanUpBeforeRestartOrExit(); Platform.exit()
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
