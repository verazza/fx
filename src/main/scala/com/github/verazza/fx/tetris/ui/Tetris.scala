package fx.tetris.ui

import scalafx.Includes._
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.layout.Pane
import scalafx.scene.shape.Line
import scalafx.stage.Stage
import fx.tetris.logic.{Tetromino, FallingTetromino}
import scalafx.animation.AnimationTimer
import scalafx.scene.input.{KeyCode, KeyEvent}
import scalafx.scene.text.Text

import scala.collection.mutable

object Tetris {
  val numCols = 10
  val numRows = 20
  val cellSize = 30
  val boardWidth = numCols * cellSize
  val boardHeight = numRows * cellSize

  var board: Array[Array[Option[Color]]] =
    Array.fill(numRows, numCols)(None)

  var currentFallingTetromino: FallingTetromino = createNewFallingTetromino()
  var gameOver: Boolean = false
  var score: Int = 0

  val gameOverText = new Text {
    text = "GAME OVER"
    style =
      "-fx-font-size: 40pt; -fx-fill: red; -fx-stroke: black; -fx-stroke-width: 1;"
    layoutX = boardWidth / 2 - 120
    layoutY = boardHeight / 2
    visible = false
  }

  // 現在押されているキーを追跡するためのセット
  val keysPressed = mutable.Set[KeyCode]()

  val fallIntervalNormal = 700_000_000L // 通常の落下間隔 (0.7秒)
  val fallIntervalSoftDrop = 50_000_000L // ソフトドロップ時の落下間隔 (0.05秒)
  var currentFallInterval = fallIntervalNormal
  var lastFallTime = 0L

  // 左右移動のDAS (Delayed Auto Shift) のためのパラメータ (簡易版)
  val dasDelay = 160_000_000L // Auto Shift 開始までの遅延 (0.16秒)
  val arrInterval = 50_000_000L // Auto Repeat Rate (0.05秒)
  var lastMoveTime = 0L
  var moveKeyDownTime = 0L
  var continuousMoveDirection: Option[Int] =
    None // None, Some(-1 for left), Some(1 for right)

  def createNewFallingTetromino(): FallingTetromino = {
    val (shape, color) = Tetromino.getRandomTetromino()
    val tetromino = new FallingTetromino(shape, color)
    tetromino.x = numCols / 2 - shape(0).length / 2
    tetromino.y = 0
    tetromino
  }

  def getStage(id: Int = 0): Stage = {
    resetGame()

    new Stage {
      title.value = s"ScalaFX Tetris (id: ${id})"
      scene = new Scene(boardWidth, boardHeight) {
        fill = Color.LightGray
        val gamePane = new Pane

        gamePane.children.add(gameOverText)
        drawGame(gamePane)

        val timer = AnimationTimer { now =>
          if (!gameOver) {
            handleKeyInputContinuous(now) // 押しっぱなしキーの処理

            if (now - lastFallTime >= currentFallInterval) {
              moveDownAndFix()
              lastFallTime = now
            }
            drawGame(gamePane)
          } else {
            if (!gameOverText.visible.value) {
              gameOverText.visible = true
              gameOverText.toFront() // ★ ゲームオーバーテキストを最前面に
            }
          }
        }
        timer.start()

        onKeyPressed = (event: scalafx.scene.input.KeyEvent) => {
          if (!gameOver) {
            if (!keysPressed.contains(event.code)) { // 新しく押されたキーの場合のみ単発アクション
              event.code match {
                case KeyCode.Up =>
                  if (event.controlDown)
                    tryRotate(clockwise = false) // Ctrl+Upで逆回転
                  else tryRotate(clockwise = true) // Upで時計回り回転
                // 左右キーは押しっぱなし処理に移行するので、ここでは初回移動のみ
                case KeyCode.Left =>
                  tryMoveHorizontal(-1)
                  moveKeyDownTime = System.nanoTime()
                  continuousMoveDirection = Some(-1)
                  lastMoveTime = System.nanoTime() // 初回移動時刻
                case KeyCode.Right =>
                  tryMoveHorizontal(1)
                  moveKeyDownTime = System.nanoTime()
                  continuousMoveDirection = Some(1)
                  lastMoveTime = System.nanoTime() // 初回移動時刻
                case _ =>
              }
            }
            keysPressed += event.code // 常にキーセットは更新

            // 下キーは押されたら即座にソフトドロップ開始
            if (event.code == KeyCode.Down) {
              currentFallInterval = fallIntervalSoftDrop
            }
            drawGame(gamePane) // 入力後すぐに描画更新
          }
        }

        onKeyReleased = (event: scalafx.scene.input.KeyEvent) => {
          keysPressed -= event.code
          if (event.code == KeyCode.Down) {
            currentFallInterval = fallIntervalNormal
          }
          if (
            event.code == KeyCode.Left && continuousMoveDirection.contains(
              -1
            ) ||
            event.code == KeyCode.Right && continuousMoveDirection.contains(1)
          ) {
            continuousMoveDirection = None
            moveKeyDownTime = 0L
          }
        }
        content = gamePane
      }
    }
  }

  private def handleKeyInputContinuous(now: Long): Unit = {
    // 左右の連続移動 (DAS)
    continuousMoveDirection.foreach { dir =>
      if (moveKeyDownTime > 0) { // キーが押されている
        if (now - moveKeyDownTime > dasDelay) { // DAS遅延を超えたか
          if (now - lastMoveTime > arrInterval) { // ARR間隔を超えたか
            tryMoveHorizontal(dir)
            lastMoveTime = now
          }
        }
      }
    }

    // ソフトドロップ (下キーが押されていれば継続)
    if (keysPressed.contains(KeyCode.Down)) {
      currentFallInterval = fallIntervalSoftDrop
    } else {
      // 他の要因でソフトドロップになっていた場合、下キーが離れていれば通常に戻す
      // (onKeyReleasedでも処理しているが、念のため)
      if (currentFallInterval == fallIntervalSoftDrop) {
        currentFallInterval = fallIntervalNormal
      }
    }
  }

  def resetGame(): Unit = {
    board = Array.fill(numRows, numCols)(None)
    currentFallingTetromino = createNewFallingTetromino()
    gameOver = false
    score = 0
    keysPressed.clear()
    continuousMoveDirection = None
    moveKeyDownTime = 0L
    lastFallTime = System.nanoTime() // タイマーリセット時に初期化
    currentFallInterval = fallIntervalNormal
    if (gameOverText != null) gameOverText.visible = false
  }

  private def isValidPosition(
    tetromino: FallingTetromino,
    testX: Int,
    testY: Int,
    testShape: Array[Array[Int]]
  ): Boolean = {
    for (r <- testShape.indices; c <- testShape(r).indices) {
      if (testShape(r)(c) == 1) {
        val boardX = testX + c
        val boardY = testY + r

        if (boardX < 0 || boardX >= numCols || boardY >= numRows) {
          return false
        }
        if (boardY >= 0 && board(boardY)(boardX).isDefined) { // boardY < 0 は形状が盤面の上に一部ある状態なので許容
          return false
        }
      }
    }
    true
  }

  private def tryMoveHorizontal(dx: Int): Unit = {
    if (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x + dx,
        currentFallingTetromino.y,
        currentFallingTetromino.currentShape
      )
    ) {
      currentFallingTetromino.x += dx
      // lastMoveTime = System.nanoTime() // DAS用: 移動したら最終移動時刻を更新
    }
  }

  private def tryRotate(clockwise: Boolean): Unit = {
    val originalShape = currentFallingTetromino.currentShape
    currentFallingTetromino.rotate(clockwise) // 一旦回転させてみる
    val rotatedShape = currentFallingTetromino.currentShape

    if (
      !isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x,
        currentFallingTetromino.y,
        rotatedShape
      )
    ) {
      currentFallingTetromino.currentShape = originalShape // ダメなら元に戻す
    }
    // TODO: ウォールキック/フロアキックなどの高度な回転処理
  }

  private def moveDownAndFix(): Unit = {
    if (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x,
        currentFallingTetromino.y + 1,
        currentFallingTetromino.currentShape
      )
    ) {
      currentFallingTetromino.moveDown()
    } else {
      fixTetromino()
      val linesClearedCount = clearLines()
      if (linesClearedCount > 0) {
        score += linesClearedCount * 100 * linesClearedCount // スコア計算例
        println(s"Lines Cleared: $linesClearedCount, Total Score: $score")
      }
      currentFallingTetromino = createNewFallingTetromino()
      if (
        !isValidPosition(
          currentFallingTetromino,
          currentFallingTetromino.x,
          currentFallingTetromino.y,
          currentFallingTetromino.currentShape
        )
      ) {
        gameOver = true
        println("GAME OVER!")
      }
    }
  }

  private def fixTetromino(): Unit = {
    for (
      r <- currentFallingTetromino.currentShape.indices;
      c <- currentFallingTetromino.currentShape(r).indices
    ) {
      if (currentFallingTetromino.currentShape(r)(c) == 1) {
        val boardX = currentFallingTetromino.x + c
        val boardY = currentFallingTetromino.y + r
        if (
          boardY >= 0 && boardY < numRows && boardX >= 0 && boardX < numCols
        ) {
          board(boardY)(boardX) = Some(currentFallingTetromino.color)
        }
      }
    }
  }

  private def clearLines(): Int = {
    var linesClearedThisTurn = 0
    var r = numRows - 1
    while (r >= 0) {
      var rowIsFull = true
      var c = 0
      while (c < numCols && rowIsFull) {
        if (board(r)(c).isEmpty) {
          rowIsFull = false
        }
        c += 1
      }

      if (rowIsFull) {
        linesClearedThisTurn += 1
        for (moveDownRow <- r until 0 by -1) {
          for (col <- 0 until numCols) {
            board(moveDownRow)(col) = board(moveDownRow - 1)(col)
          }
        }
        for (col <- 0 until numCols) {
          board(0)(col) = None
        }
        // rはデクリメントしない (同じ行を再チェック)
      } else {
        r -= 1 // 次の上の行へ
      }
    }
    linesClearedThisTurn
  }

  private def drawGame(pane: Pane): Unit = {
    pane.children.clear()
    drawGrid(pane)
    pane.children.add(
      gameOverText
    ) // gameOverTextは常にchildrenに含め、visibilityとtoFrontで制御

    for (r <- 0 until numRows; c <- 0 until numCols) {
      board(r)(c).foreach { color =>
        val rect = new Rectangle {
          x = c * cellSize; y = r * cellSize
          width = cellSize; height = cellSize
          fill = color
          stroke = Color.Black; strokeWidth = 0.5
        }
        pane.children += rect
      }
    }

    if (!gameOver) {
      val shape = currentFallingTetromino.currentShape
      val color = currentFallingTetromino.color
      val startX = currentFallingTetromino.x
      val startY = currentFallingTetromino.y

      for (row <- shape.indices; col <- shape(row).indices) {
        if (shape(row)(col) == 1) {
          val rect = new Rectangle {
            x = (startX + col) * cellSize; y = (startY + row) * cellSize
            width = cellSize; height = cellSize
            fill = color
            stroke = Color.Black; strokeWidth = 1
          }
          pane.children += rect
        }
      }
    }
    if (gameOver && gameOverText.visible.value) { // ゲームオーバーならテキストを最前面に
      gameOverText.toFront()
    }
  }

  private def drawGrid(pane: Pane): Unit = {
    for (i <- 0 to numCols) {
      pane.children += new Line {
        startX = i * cellSize; startY = 0
        endX = i * cellSize; endY = boardHeight
        stroke = Color.DarkGray; strokeWidth = 0.5
      }
    }
    for (i <- 0 to numRows) {
      pane.children += new Line {
        startX = 0; startY = i * cellSize
        endX = boardWidth; endY = i * cellSize
        stroke = Color.DarkGray; strokeWidth = 0.5
      }
    }
  }
}
