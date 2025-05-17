// src/main/scala/com/github/verazza/fx/tetris/ui/TetrisUI.scala
package fx.tetris.ui

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.{Rectangle, Line} // Line を明示的にインポート
import scalafx.scene.layout.{Pane, StackPane, VBox}
import scalafx.scene.control.Button
import scalafx.geometry.{Insets, Pos}
import scalafx.stage.Stage
import fx.tetris.logic.{
  TetrisGameLogic,
  FallingTetromino,
  GameConstants
}
import scalafx.animation.{
  AnimationTimer,
  Timeline,
  KeyFrame
}
import scalafx.util.Duration
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.text.Text
import scalafx.application.Platform

import scala.collection.mutable

object TetrisUI {

  import GameConstants._

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
      style = "-fx-font-size: 40pt; -fx-fill: red; -fx-stroke: black; -fx-stroke-width: 1;"
      layoutX = BoardWidth / 2 - 120
      layoutY = BoardHeight / 3
      visible = false
    }

    val scoreText = new Text {
      style = "-fx-font-size: 16pt; -fx-fill: white;"
      visible = false
    }

    // --- ポーズメニューボタンの定義 ---
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

    // --- ポーズメニューUI ---
    val pauseMenuPane = new VBox {
      spacing = 15
      alignment = Pos.Center
      padding = Insets(30)
      style = "-fx-background-color: rgba(50, 50, 50, 0.85); -fx-background-radius: 15; -fx-border-color: silver; -fx-border-width: 2; -fx-border-radius: 13;"
      visible = false
      maxWidth = 250
      children = Seq(
        new Text("ポーズ中") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
        resumeButtonP,
        restartButtonP,
        backToMenuButtonP,
        quitGameButtonP
      )
    }

    // --- ゲームオーバーメニューボタンの定義 ---
    val restartButtonGO = new Button("もう一度プレイ") {
      prefWidth = 220; style = "-fx-font-size: 14pt;"
    }
    val backToMenuButtonGO = new Button("メインメニューに戻る") {
      prefWidth = 220; style = "-fx-font-size: 14pt;"
    }
    val quitGameButtonGO = new Button("ゲーム終了") {
      prefWidth = 220; style = "-fx-font-size: 14pt;"
    }

    // --- ゲームオーバーメニューUI ---
    val gameOverMenuPane = new VBox {
      spacing = 15
      alignment = Pos.Center
      padding = Insets(30)
      style = "-fx-background-color: rgba(70, 0, 0, 0.9); -fx-background-radius: 15; -fx-border-color: darkred; -fx-border-width: 2; -fx-border-radius: 13;"
      visible = false
      maxWidth = 280
      children = Seq(
        new Text("結果") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
        scoreText,
        restartButtonGO,
        backToMenuButtonGO,
        quitGameButtonGO
      )
    }

    val gamePane = new Pane
    gamePane.children.add(gameOverText)

    val rootPane = new StackPane {
      children = Seq(gamePane, pauseMenuPane, gameOverMenuPane)
      style = "-fx-background-color: LightGray;"
    }

    val currentStage = new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(BoardWidth, BoardHeight) {
        content = rootPane

        val timer = AnimationTimer { now =>
          if (gameLogic.gameOver) {
            if (!gameOverMenuPane.visible.value) {
              animationTimerInstance.foreach(_.stop())
              gameOverText.visible = true
              startGameOverTextAnimation(gameOverText)
              scoreText.text = s"スコア: ${gameLogic.score}"
              scoreText.visible = true
              gameOverMenuPane.visible = true
              gameOverMenuPane.toFront()
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
            gameLogic.updateGameTick(now)
            drawGameUI(
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
            // Game over, no key input for game play
          } else {
            event.code match {
              case KeyCode.Escape =>
                togglePause(pauseMenuPane, gameLogic) // gameOverMenuPane はここでは不要
              case _ =>
                if (!isPaused) {
                  var needsRedraw = false
                  if (!keysPressed.contains(event.code) || event.code == KeyCode.Down) {
                    event.code match {
                      case KeyCode.Up      => if (gameLogic.tryRotate(clockwise = true)) needsRedraw = true
                      case KeyCode.Control => if (gameLogic.tryRotate(clockwise = false)) needsRedraw = true
                      case KeyCode.Space   => gameLogic.performHardDrop(); needsRedraw = true
                      case KeyCode.Left =>
                        if (gameLogic.tryMoveHorizontal(-1)) {
                          moveKeyDownTime = System.nanoTime(); continuousMoveDirection = Some(-1); lastMoveTime = System.nanoTime(); needsRedraw = true
                        }
                      case KeyCode.Right =>
                        if (gameLogic.tryMoveHorizontal(1)) {
                          moveKeyDownTime = System.nanoTime(); continuousMoveDirection = Some(1); lastMoveTime = System.nanoTime(); needsRedraw = true
                        }
                      case KeyCode.Down =>
                        gameLogic.setSoftDropActive(true); needsRedraw = true
                      case _ =>
                    }
                  }
                  if (!keysPressed.contains(event.code)) keysPressed += event.code

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
            if ((event.code == KeyCode.Left && continuousMoveDirection.contains(-1)) ||
                (event.code == KeyCode.Right && continuousMoveDirection.contains(1))) {
              continuousMoveDirection = None; moveKeyDownTime = 0L
            }
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
      if (isPaused) togglePause(pauseMenuPane, gameLogic) // ポーズ解除
      else animationTimerInstance.foreach(_.start())
      gameOverMenuPane.visible = false
      drawGameUI(gamePane, gameLogic.currentFallingTetromino, gameLogic.board, gameLogic.gameOver, gameOverText)
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
      drawGameUI(gamePane, gameLogic.currentFallingTetromino, gameLogic.board, gameLogic.gameOver, gameOverText)
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

  // togglePause の引数から gameOverMenu を削除 (直接操作しないため)
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

  private def startGameOverTextAnimation(textNode: Text): Unit = { /* ... (変更なし) ... */ }
  private def stopGameOverTextAnimation(): Unit = { /* ... (変更なし) ... */ }

  private def drawGameUI(
      pane: Pane,
      fallingTetromino: FallingTetromino,
      currentBoard: Array[Array[Option[Color]]],
      isGameOver: Boolean,
      txtGameOver: Text
  ): Unit = {
    pane.children.clear()
    drawGridUI(pane) // グリッドを先に描画

    // ゲームオーバーテキストは、isGameOver が true で、かつ visible プロパティが true の場合にのみ追加
    // (点滅アニメーションで opacity が変わるため、visible フラグで制御)
    if (isGameOver && txtGameOver.visible.value) {
        pane.children.add(txtGameOver)
    }
    
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
    // gameOverText.toFront() は、StackPane で gameOverMenuPane が表示される際にそちらで行うため、
    // gamePane 内での toFront は不要。
  }

  private def drawGridUI(pane: Pane): Unit = {
    for (i <- 0 to NumCols) {
      pane.children += new Line { // ここで Line が正しく解決されるか
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
