package fx.tetris.logic

import scalafx.scene.paint.Color
import scala.collection.mutable

class TetrisGameLogic {

  import GameConstants._

  // Game State
  var board: Array[Array[Option[Color]]] = Array.fill(NumRows, NumCols)(None)
  var currentFallingTetromino: FallingTetromino = createNewFallingTetromino()
  var gameOver: Boolean = false
  var score: Int = 0

  // Animation Timer related state
  var currentFallInterval: Long = FallIntervalNormal
  var lastFallTime: Long = 0L

  // Lock Delay State
  var isGrounded: Boolean = false // ミノが接地しているか
  var lockDelayStartTime: Long = 0L // 接地開始時刻、またはロック遅延タイマー
  val initialLockDelayNanos: Long = 500_000_000L // 0.5秒のロック遅延 (調整可能)
  var currentLockDelayNanos: Long = initialLockDelayNanos
  var rotationsSinceGrounded: Int = 0 // 接地してからの回転回数
  val maxRotationsForLockReset: Int = 15 // ロック遅延がリセットされる回転回数の上限 (調整可能)
  var successfulMovesSinceGrounded: Int = 0 // 接地後の移動/回転回数 (簡易的なリセット制御用)

  def resetGame(): Unit = {
    board = Array.fill(NumRows, NumCols)(None)
    currentFallingTetromino = createNewFallingTetromino()
    gameOver = false
    score = 0
    currentFallInterval = FallIntervalNormal
    lastFallTime = System.nanoTime()
    resetLockDelayState()
  }

  private def resetLockDelayState(): Unit = {
    isGrounded = false
    lockDelayStartTime = 0L
    rotationsSinceGrounded = 0
    successfulMovesSinceGrounded = 0
    currentLockDelayNanos = initialLockDelayNanos
  }

  private def createNewFallingTetromino(): FallingTetromino = {
    resetLockDelayState() // 新しいミノが出現するたびにロック遅延状態をリセット
    val (name, shape, color) = Tetromino.getRandomTetromino()
    val tetromino = new FallingTetromino(name, shape, color)
    tetromino.x = NumCols / 2 - shape(0).length / 2
    tetromino.y = 0
    tetromino
  }

  def isValidPosition(
    tetromino: FallingTetromino,
    testX: Int,
    testY: Int,
    testShape: Array[Array[Int]]
  ): Boolean = {
    for (r <- testShape.indices; c <- testShape(r).indices) {
      if (testShape(r)(c) == 1) {
        val boardX = testX + c
        val boardY = testY + r
        if (boardX < 0 || boardX >= NumCols || boardY >= NumRows) return false
        if (boardY >= 0 && board(boardY)(boardX).isDefined) return false
      }
    }
    true
  }

  def tryMoveHorizontal(dx: Int): Boolean = {
    if (gameOver) return false
    if (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x + dx,
        currentFallingTetromino.y,
        currentFallingTetromino.currentShape
      )
    ) {
      currentFallingTetromino.x += dx
      if (isGrounded) { // 接地中に移動したらロック遅延をリセット
        resetLockDelayTimer()
        successfulMovesSinceGrounded += 1
      }
      true
    } else {
      false
    }
  }

  def tryRotate(clockwise: Boolean): Boolean = {
    if (gameOver || currentFallingTetromino.minoName == "O") return false

    val originalShape = currentFallingTetromino.currentShape
    val originalX = currentFallingTetromino.x
    val originalY = currentFallingTetromino.y
    val originalOrientation = currentFallingTetromino.orientation

    currentFallingTetromino.rotate(clockwise)
    val rotatedShape = currentFallingTetromino.currentShape

    val kickOffsets = currentFallingTetromino.minoName match {
      case "I" =>
        List((0, 0), (-1, 0), (1, 0), (-2, 0), (2, 0), (0, -1), (0, 1))
      case _ => List((0, 0), (-1, 0), (1, 0))
    }

    var rotatedSuccessfully = false
    for ((offX, offY) <- kickOffsets if !rotatedSuccessfully) {
      val testX = originalX + offX
      val testY = originalY + offY
      if (
        isValidPosition(currentFallingTetromino, testX, testY, rotatedShape)
      ) {
        currentFallingTetromino.x = testX
        currentFallingTetromino.y = testY
        rotatedSuccessfully = true
      }
    }

    if (rotatedSuccessfully) {
      if (isGrounded) { // 接地中に回転したらロック遅延を調整
        rotationsSinceGrounded += 1
        successfulMovesSinceGrounded += 1
        if (rotationsSinceGrounded < maxRotationsForLockReset) {
          resetLockDelayTimer()
          // 回転するたびにロック遅延を短くする (例)
          // currentLockDelayNanos = Math.max(100_000_000L, initialLockDelayNanos / (rotationsSinceGrounded + 1))
        } else {
          // 最大回転回数に達したら、遅延タイマーをリセットしない (または非常に短くする)
          // lockDelayStartTime = System.nanoTime() // 即座にロックに向かわせる
        }
      }
    } else { // 回転失敗なら元に戻す
      currentFallingTetromino.currentShape = originalShape
      currentFallingTetromino.x = originalX
      currentFallingTetromino.y = originalY
      currentFallingTetromino.orientation = originalOrientation
    }
    rotatedSuccessfully
  }

  private def resetLockDelayTimer(): Unit = {
    lockDelayStartTime = System.nanoTime()
    // currentLockDelayNanos は回転回数に応じて調整しても良いが、ここではシンプルに固定値を使う
  }

  def performHardDrop(): Unit = {
    if (gameOver) return

    var ghostY = currentFallingTetromino.y
    while (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x,
        ghostY + 1,
        currentFallingTetromino.currentShape
      )
    ) {
      ghostY += 1
    }

    if (ghostY > currentFallingTetromino.y) {
      currentFallingTetromino.y = ghostY
    }
    // ハードドロップ後もロック遅延処理へ移行
    isGrounded = true
    lockDelayStartTime = System.nanoTime() // 短いロック遅延を開始
    currentLockDelayNanos = 100_000_000L // ハードドロップ用の短い遅延 (0.1秒など)
    rotationsSinceGrounded = maxRotationsForLockReset // ハードドロップ後は回転による延長なし
    successfulMovesSinceGrounded = 0
    // fixAndSpawnNew() // すぐには固定しない
    lastFallTime = System.nanoTime() // 自然落下タイマーもリセット
  }

  def setSoftDropActive(active: Boolean): Unit = {
    if (gameOver) return
    currentFallInterval =
      if (active) FallIntervalSoftDrop else FallIntervalNormal
    if (active) {
      // ソフトドロップ開始時に接地していなければ、落下を促す
      // lastFallTime = System.nanoTime() - currentFallInterval // 次のtickで即落下
    }
  }

  def updateGameTick(currentTime: Long): Boolean = {
    var stateChanged = false
    if (gameOver) return false

    if (isGrounded) {
      // 接地している場合、ロック遅延タイマーをチェック
      if (currentTime - lockDelayStartTime >= currentLockDelayNanos) {
        // ロック遅延時間が経過したのでミノを固定
        fixAndSpawnNew()
        stateChanged = true
      } else {
        // ロック遅延中。まだ固定しない。
        // 下にブロックがなければ接地状態を解除 (稀なケース、例えばライン消去で足場が消えた場合)
        if (
          isValidPosition(
            currentFallingTetromino,
            currentFallingTetromino.x,
            currentFallingTetromino.y + 1,
            currentFallingTetromino.currentShape
          )
        ) {
          // isGrounded = false // 足場がなくなったので自由落下に戻る
          // resetLockDelayState()
        }
      }
    } else {
      // 接地していない場合、通常の落下処理
      if (currentTime - lastFallTime >= currentFallInterval) {
        if (
          isValidPosition(
            currentFallingTetromino,
            currentFallingTetromino.x,
            currentFallingTetromino.y + 1,
            currentFallingTetromino.currentShape
          )
        ) {
          currentFallingTetromino.moveDown()
          lastFallTime = currentTime
          stateChanged = true
        } else {
          // 接地した
          isGrounded = true
          lockDelayStartTime = currentTime
          currentLockDelayNanos = initialLockDelayNanos // 通常落下の初期ロック遅延
          rotationsSinceGrounded = 0
          successfulMovesSinceGrounded = 0
          stateChanged = true // 接地状態になったことも変化とみなす
        }
      }
    }
    stateChanged
  }

  // moveDownAndFixInternal は updateGameTick に統合されたため削除または変更
  // private def moveDownAndFixInternal(): Unit = { ... }

  private def fixAndSpawnNew(): Unit = {
    fixTetromino()
    val linesClearedCount = clearLines()
    if (linesClearedCount > 0) {
      score += (linesClearedCount * 100 * linesClearedCount)
      println(s"Lines Cleared: $linesClearedCount, Total Score: $score")
    }
    currentFallingTetromino =
      createNewFallingTetromino() // これが resetLockDelayState も呼ぶ
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

  private def fixTetromino(): Unit = {
    for (
      r <- currentFallingTetromino.currentShape.indices;
      c <- currentFallingTetromino.currentShape(r).indices
    ) {
      if (currentFallingTetromino.currentShape(r)(c) == 1) {
        val boardX = currentFallingTetromino.x + c
        val boardY = currentFallingTetromino.y + r
        if (
          boardY >= 0 && boardY < NumRows && boardX >= 0 && boardX < NumCols
        ) {
          board(boardY)(boardX) = Some(currentFallingTetromino.color)
        }
      }
    }
  }

  private def clearLines(): Int = {
    var linesClearedThisTurn = 0
    var r = NumRows - 1
    while (r >= 0) {
      var rowIsFull = true
      var c = 0
      while (c < NumCols && rowIsFull) {
        if (board(r)(c).isEmpty) rowIsFull = false
        c += 1
      }
      if (rowIsFull) {
        linesClearedThisTurn += 1
        for (moveDownRow <- r until 0 by -1) {
          for (col <- 0 until NumCols) {
            board(moveDownRow)(col) = board(moveDownRow - 1)(col)
          }
        }
        for (col <- 0 until NumCols) board(0)(col) = None
      } else {
        r -= 1
      }
    }
    linesClearedThisTurn
  }
}
