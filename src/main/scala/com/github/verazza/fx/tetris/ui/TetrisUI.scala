package fx.tetris.ui

import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.layout.{Pane, StackPane, VBox}
import scalafx.scene.control.Button
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.shape.Line
import scalafx.stage.Stage
import fx.tetris.logic.{TetrisGameLogic, FallingTetromino, GameConstants}
import scalafx.animation.AnimationTimer
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.text.Text
import scalafx.application.Platform
import scala.collection.mutable

object TetrisUI {

  import GameConstants._

  private var animationTimerInstance: Option[AnimationTimer] = None
  private var isPaused: Boolean = false

  def getStage(id: Int = 0): Stage = {
    val gameLogic = new TetrisGameLogic()
    gameLogic.resetGame()

    isPaused = false

    val gameOverText = new Text {
      text = "GAME OVER"
      style =
        "-fx-font-size: 40pt; -fx-fill: red; -fx-stroke: black; -fx-stroke-width: 1;"
      layoutX = BoardWidth / 2 - 120
      layoutY = BoardHeight / 2
      visible = false
    }
    val keysPressed = mutable.Set[KeyCode]()
    var continuousMoveDirection: Option[Int] = None
    var moveKeyDownTime = 0L
    var lastMoveTime = 0L

    // --- ポーズメニューUIの作成 ---
    val pauseMenuPane = new VBox {
      spacing = 15
      alignment = Pos.Center
      padding = Insets(30)
      style =
        "-fx-background-color: rgba(50, 50, 50, 0.85); -fx-background-radius: 15; -fx-border-color: silver; -fx-border-width: 2; -fx-border-radius: 13;"
      visible = false
      maxWidth = 250

      val resumeButtonInternal = new Button("ゲームに戻る (Esc)") { // 変数名を変更して衝突を避ける
        prefWidth = 200
        style = "-fx-font-size: 14pt;"
      }
      val restartButtonInternal = new Button("最初からやり直す") { // 変数名を変更
        prefWidth = 200
        style = "-fx-font-size: 14pt;"
      }
      val backToMenuButtonInternal = new Button("メニュー画面に戻る") { // 変数名を変更
        prefWidth = 200
        style = "-fx-font-size: 14pt;"
      }
      val quitGameButtonInternal = new Button("ゲームを終了する") { // 変数名を変更
        prefWidth = 200
        style = "-fx-font-size: 14pt;"
      }
      children = Seq(
        new Text("ポーズ中") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
        resumeButtonInternal, // 変更後の変数名を使用
        restartButtonInternal,
        backToMenuButtonInternal,
        quitGameButtonInternal
      )
    }
    // --- ポーズメニューUIここまで ---

    // --- メインのゲームペインとルートペイン ---
    val gamePane = new Pane
    gamePane.children.add(gameOverText)

    val rootPane = new StackPane {
      children = Seq(gamePane, pauseMenuPane)
      style = "-fx-background-color: LightGray;"
    }
    // --- ルートペインここまで ---

    // ★★★ ボタンアクション設定をここに移動 ★★★
    // `pauseMenuPane` の子要素として定義されたボタンを参照するために、
    // `pauseMenuPane.children` から取得するか、あるいはボタンの定義を `pauseMenuPane` の外で行い、
    // `children` に追加する際に参照を保持する。
    // ここでは、pauseMenuPaneの初期化ブロック内で定義した変数名 (Internalをつけたもの) を使います。
    // ただし、これらの変数はVBoxの初期化ブロックのスコープ内なので、直接ここからはアクセスできません。
    // より良い方法は、ボタンをgetStageスコープで定義し、VBoxのchildrenに追加することです。

    // ボタンをgetStageスコープで定義
    val resumeButton = new Button("ゲームに戻る (Esc)") {
      prefWidth = 200
      style = "-fx-font-size: 14pt;"
    }
    val restartButton = new Button("最初からやり直す") {
      prefWidth = 200
      style = "-fx-font-size: 14pt;"
    }
    val backToMenuButton = new Button("メニュー画面に戻る") {
      prefWidth = 200
      style = "-fx-font-size: 14pt;"
    }
    val quitGameButton = new Button("ゲームを終了する") {
      prefWidth = 200
      style = "-fx-font-size: 14pt;"
    }

    // pauseMenuPane の children を更新して、これらのボタンを使用
    pauseMenuPane.children = Seq(
      new Text("ポーズ中") { style = "-fx-font-size: 20pt; -fx-fill: white;" },
      resumeButton,
      restartButton,
      backToMenuButton,
      quitGameButton
    )

    // これで、以下のアクション設定が正しくボタンを参照できます。
    resumeButton.onAction = () => togglePause(pauseMenuPane, gameLogic)
    restartButton.onAction = () => {
      gameLogic.resetGame()
      keysPressed.clear()
      continuousMoveDirection = None
      moveKeyDownTime = 0L
      if (isPaused) togglePause(pauseMenuPane, gameLogic)
      else animationTimerInstance.foreach(_.start())
      drawGameUI(
        gamePane,
        gameLogic.currentFallingTetromino,
        gameLogic.board,
        gameLogic.gameOver,
        gameOverText
      )
    }
    backToMenuButton.onAction = () => {
      // stage を参照する必要があるため、stage の定義後にこのアクションを設定するか、
      // stage をこのラムダにキャプチャさせる。
      // ここでは、stage を直接参照せず、後で stage.close() を呼び出すようにする。
      // このアクションは stage の初期化ブロック内に移動するのが適切。
    }
    quitGameButton.onAction = () => {
      animationTimerInstance.foreach(_.stop())
      Platform.exit()
    }

    val currentStage = new Stage { // stage変数名を変更して、後で参照できるようにする
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(BoardWidth, BoardHeight) {
        content = rootPane

        val timer = AnimationTimer { now =>
          if (!isPaused && !gameLogic.gameOver) {
            continuousMoveDirection.foreach { dir =>
              if (moveKeyDownTime > 0 && (now - moveKeyDownTime > DasDelay)) {
                if (now - lastMoveTime > ArrInterval) {
                  gameLogic.tryMoveHorizontal(dir)
                  lastMoveTime = now
                }
              }
            }
            gameLogic.setSoftDropActive(keysPressed.contains(KeyCode.Down))

            if (gameLogic.updateGameTick(now)) {
              // game state updated
            }
            drawGameUI(
              gamePane,
              gameLogic.currentFallingTetromino,
              gameLogic.board,
              gameLogic.gameOver,
              gameOverText
            )

          } else if (gameLogic.gameOver) {
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
            animationTimerInstance.foreach(_.stop())
          }
        }
        animationTimerInstance = Some(timer)
        timer.start()

        onKeyPressed = (event: KeyEvent) => {
          event.code match {
            case KeyCode.Escape =>
              togglePause(pauseMenuPane, gameLogic)
            case _ =>
              if (!isPaused && !gameLogic.gameOver) {
                var needsRedraw = false
                if (!keysPressed.contains(event.code)) {
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
                    case _ =>
                  }
                }
                keysPressed += event.code
                if (event.code == KeyCode.Down) {
                  gameLogic.setSoftDropActive(true)
                  needsRedraw = true
                }
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
      }
    }

    // backToMenuButton のアクションをここで設定 (currentStage を参照できるように)
    backToMenuButton.onAction = () => {
      animationTimerInstance.foreach(_.stop())
      currentStage.close() // ここで currentStage を参照
    }

    currentStage.onCloseRequest = () => {
      animationTimerInstance.foreach(_.stop())
      println(s"Tetris stage (id: $id) closed.")
    }

    currentStage
  }

  private def togglePause(pauseMenu: VBox, gameLogic: TetrisGameLogic): Unit = {
    isPaused = !isPaused
    pauseMenu.visible = isPaused
    if (isPaused) {
      pauseMenu.toFront()
      animationTimerInstance.foreach(_.stop())
    } else {
      gameLogic.lastFallTime = System.nanoTime() // ポーズ解除直後から通常のインターバルで落下開始
      animationTimerInstance.foreach(_.start())
    }
  }

  private def drawGameUI(
    pane: Pane,
    fallingTetromino: FallingTetromino,
    currentBoard: Array[Array[Option[Color]]],
    isGameOver: Boolean,
    txtGameOver: Text
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
