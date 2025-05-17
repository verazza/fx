package fx.tetris.ui

import scalafx.Includes._ // ★★★ この行を追加 ★★★
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
    layoutX = boardWidth / 2 - 120 // テキストの長さに応じて調整
    layoutY = boardHeight / 2
    visible = false
  }

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

        gamePane.children.add(gameOverText) // 先にgameOverTextを追加しておく
        drawGame(gamePane)

        var lastFallTime = 0L
        val fallIntervalNormal = 1_000_000_000L // 1秒
        var currentFallInterval = fallIntervalNormal

        val timer = AnimationTimer { now =>
          if (!gameOver) {
            if (now - lastFallTime >= currentFallInterval) {
              moveDownAndFix()
              lastFallTime = now
              if (
                currentFallInterval != fallIntervalNormal && !keysPressed
                  .contains(KeyCode.Down)
              ) { // ソフトドロップ解除判定
                currentFallInterval = fallIntervalNormal
              }
            }
            drawGame(gamePane)
          } else {
            if (!gameOverText.visible.value) { // visibleがtrueでなければtrueにする
              gameOverText.visible = true
              drawGame(gamePane) // ゲームオーバー表示を確実にするため再描画
            }
          }
        }
        timer.start()

        // 押下中のキーを保持するセット (ソフトドロップ解除判定用)
        var keysPressed = Set[KeyCode]()

        // キー入力処理 (修正箇所)
        onKeyPressed = (event: scalafx.scene.input.KeyEvent) =>
          { // ★★★ 型を scalafx.scene.input.KeyEvent と明示 ★★★
            if (!gameOver) {
              keysPressed += event.code // 押されたキーをセットに追加
              event.code match {
                case KeyCode.Left  => tryMoveHorizontal(-1)
                case KeyCode.Right => tryMoveHorizontal(1)
                case KeyCode.Up    => tryRotate()
                case KeyCode.Down =>
                  currentFallInterval = 50_000_000L // ソフトドロップ (0.05秒)
                case _ => // 何もしない
              }
              drawGame(gamePane)
            }
          }

        onKeyReleased = (event: scalafx.scene.input.KeyEvent) => {
          if (!gameOver) {
            keysPressed -= event.code // 離されたキーをセットから削除
            if (event.code == KeyCode.Down) {
              currentFallInterval = fallIntervalNormal // 下キーが離されたら通常速度に戻す
            }
          }
        }
        content = gamePane
      }
    }
  }

  def resetGame(): Unit = {
    board = Array.fill(numRows, numCols)(None)
    currentFallingTetromino = createNewFallingTetromino()
    gameOver = false
    score = 0
    // gameOverTextはgetStage内で可視性を制御するため、ここでは触らないか、
    // もしgetStage外で参照されるなら初期化が必要
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

        if (boardX < 0 || boardX >= numCols || boardY >= numRows) { // 底の境界チェックは落下時に行うのでここでは y < 0 は不要
          return false
        }
        // boardYが負の場合のチェック（ミノが上部にはみ出ている初期状態など）
        if (boardY < 0) {
          // ゲーム開始直後など、上部にはみ出ていても有効な場合があるので、ここではtrueとするか、
          // boardY >=0 の時だけ board(boardY)(boardX) をチェックする
          // 今回は落下は常に盤面内で行われる前提
        } else if (board(boardY)(boardX).isDefined) {
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
    }
  }

  private def tryRotate(): Unit = {
    val rotatedShape = currentFallingTetromino.getRotatedShape()
    if (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x,
        currentFallingTetromino.y,
        rotatedShape
      )
    ) {
      currentFallingTetromino.currentShape = rotatedShape
    }
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
      val linesClearedCount = clearLines() // 消去したライン数を取得
      if (linesClearedCount > 0) {
        // スコア更新など
        score += linesClearedCount * 100 * linesClearedCount // 例: 複数ライン同時消しで高得点
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
        } else {
          // ミノがボード外に固定されようとした場合（通常は発生しないが、デバッグ用にログ）
          println(
            s"Warning: Attempted to fix block outside board at ($boardX, $boardY)"
          )
        }
      }
    }
  }

  // clearLinesは消去した行数を返すように変更
  private def clearLines(): Int = {
    var linesClearedThisTurn = 0
    var r = numRows - 1 // 一番下の行からチェック
    while (r >= 0) {
      var rowIsFull = true
      for (c <- 0 until numCols) {
        if (board(r)(c).isEmpty) {
          rowIsFull = false
          // break // 一つでも空ならその行は満杯ではない (Scalaにはbreakがないのでフラグで制御)
        }
      }

      if (rowIsFull) {
        linesClearedThisTurn += 1
        // この行を消去し、上の行をすべて下にずらす
        for (moveDownRow <- r until 0 by -1) { // r行目から1行目まで (0行目は特別扱い)
          for (c <- 0 until numCols) {
            board(moveDownRow)(c) = board(moveDownRow - 1)(c)
          }
        }
        // 一番上の行をクリア
        for (c <- 0 until numCols) {
          board(0)(c) = None
        }
        // r はデクリメントしない（同じ行を再チェックするため。上の行が落ちてきて満たされている可能性がある）
      } else {
        r -= 1 // この行は満杯ではなかったので、次の上の行へ
      }
    }
    linesClearedThisTurn
  }

  private def drawGame(
    pane: Pane,
    currentTetromino: FallingTetromino = currentFallingTetromino
  ): Unit = {
    pane.children.clear()
    drawGrid(pane)
    pane.children.add(gameOverText) // gameOverTextは常にリストに入れておき、visibleで制御

    for (r <- 0 until numRows; c <- 0 until numCols) {
      board(r)(c).foreach { color =>
        val rect = new Rectangle {
          x = c * cellSize
          y = r * cellSize
          width = cellSize
          height = cellSize
          fill = color
          stroke = Color.Black
          strokeWidth = 0.5
        }
        pane.children += rect
      }
    }

    if (!gameOver) {
      val shape = currentTetromino.currentShape
      val color = currentTetromino.color
      val startX = currentTetromino.x
      val startY = currentTetromino.y

      for (
        row <- shape.indices;
        col <- shape(row).indices
      ) {
        if (shape(row)(col) == 1) {
          val rect = new Rectangle {
            x = (startX + col) * cellSize
            y = (startY + row) * cellSize
            width = cellSize
            height = cellSize
            fill = color
            stroke = Color.Black
            strokeWidth = 1
          }
          pane.children += rect
        }
      }
    }
  }

  private def drawGrid(pane: Pane): Unit = {
    // (変更なし)
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
