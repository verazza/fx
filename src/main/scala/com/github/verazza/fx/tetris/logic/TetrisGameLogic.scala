package fx.tetris.logic

import scalafx.scene.paint.Color
import scala.collection.mutable // mutable.Queue のためにインポート

class TetrisGameLogic {

  import GameConstants._

  // ★★★ nextTetrominoQueue の初期化を先に移動 ★★★
  val nextTetrominoQueue: mutable.Queue[FallingTetromino] = mutable.Queue()

  // Game State
  var board: Array[Array[Option[Color]]] = Array.fill(NumRows, NumCols)(None)
  var currentFallingTetromino: FallingTetromino =
    initialTetromino() // これで安全に初期化できる
  var gameOver: Boolean = false
  var score: Int = 0

  // Animation Timer related state
  var currentFallInterval: Long = FallIntervalNormal
  var lastFallTime: Long = 0L

  // Lock Delay State
  var isGrounded: Boolean = false
  var lockDelayStartTime: Long = 0L
  val initialLockDelayNanos: Long = 500_000_000L
  var currentLockDelayNanos: Long = initialLockDelayNanos
  var rotationsSinceGrounded: Int = 0
  val maxRotationsForLockReset: Int = 15
  var successfulMovesSinceGrounded: Int = 0

  var gameOverPendingAnimation: Boolean = false

  // Hold機能関連
  var heldTetromino: Option[FallingTetromino] = None
  var canHold: Boolean = true

  // Nextミノ関連の初期化は上に移動済み

  // 初期化時に最初のミノとNextミノを準備
  private def initialTetromino(): FallingTetromino = {
    populateNextQueue() // 先にNextキューを満たす
    pullFromNextQueue() // 最初のミノをキューから取得
  }

  def resetGame(): Unit = {
    board = Array.fill(NumRows, NumCols)(None)
    gameOver = false
    gameOverPendingAnimation = false
    score = 0
    currentFallInterval = FallIntervalNormal
    lastFallTime = System.nanoTime()

    heldTetromino = None
    canHold = true

    nextTetrominoQueue.clear()
    populateNextQueue()

    currentFallingTetromino = pullFromNextQueue()
    resetLockDelayState()
    println("[Logic] Game Reset. Next queue populated.")
  }

  private def resetLockDelayState(): Unit = {
    isGrounded = false
    lockDelayStartTime = 0L
    rotationsSinceGrounded = 0
    successfulMovesSinceGrounded = 0
    currentLockDelayNanos = initialLockDelayNanos
  }

  private def pullFromNextQueue(): FallingTetromino = {
    if (nextTetrominoQueue.isEmpty) {
      populateNextQueue()
    }
    val nextMino = nextTetrominoQueue.dequeue()
    addNewToNextQueue()

    var minYInShape = nextMino.currentShape.length
    for (
      r <- nextMino.currentShape.indices; c <- nextMino.currentShape(r).indices
    ) {
      if (nextMino.currentShape(r)(c) == 1)
        minYInShape = Math.min(minYInShape, r)
    }
    nextMino.x = NumCols / 2 - nextMino.currentShape(0).length / 2
    nextMino.y = -minYInShape

    println(
      s"[Logic] Pulled from Next: ${nextMino.minoName}. Queue size: ${nextTetrominoQueue.length}"
    )
    nextMino
  }

  private def populateNextQueue(): Unit = {
    // nextTetrominoQueue が null でないことをここで期待 (初期化順で解決)
    while (nextTetrominoQueue.length < NumNextToDisplay) {
      addNewToNextQueue()
    }
  }

  private def addNewToNextQueue(): Unit = {
    val (name, shape, color) = Tetromino.getRandomTetromino()
    val newNext = new FallingTetromino(name, shape, color)
    nextTetrominoQueue.enqueue(newNext)
  }

  private def assignNewFallingTetromino(isFromHold: Boolean = false): Unit = {
    if (!isFromHold) {
      canHold = true
    }
    resetLockDelayState()
    currentFallingTetromino = pullFromNextQueue()
    println(
      s"[Logic] Assigned new falling tetromino: ${currentFallingTetromino.minoName}. Can hold: $canHold"
    )
  }

  def performHold(): Boolean = {
    if (!canHold || gameOver || gameOverPendingAnimation) return false

    val previouslyHeld = heldTetromino
    val currentMinoShapeData =
      Tetromino.tetrominoData.find(_._1 == currentFallingTetromino.minoName).get
    heldTetromino = Some(
      new FallingTetromino(
        currentFallingTetromino.minoName,
        currentMinoShapeData._2,
        currentFallingTetromino.color
      )
    )

    previouslyHeld match {
      case Some(minoFromHold) =>
        currentFallingTetromino = new FallingTetromino(
          minoFromHold.minoName,
          minoFromHold.currentShape,
          minoFromHold.color
        )
        var minYInShape = minoFromHold.currentShape.length
        for (
          r <- minoFromHold.currentShape.indices;
          c <- minoFromHold.currentShape(r).indices
        ) {
          if (minoFromHold.currentShape(r)(c) == 1)
            minYInShape = Math.min(minYInShape, r)
        }
        currentFallingTetromino.x =
          NumCols / 2 - minoFromHold.currentShape(0).length / 2
        currentFallingTetromino.y = -minYInShape
      case None =>
        assignNewFallingTetromino(isFromHold = true)
    }

    canHold = false
    resetLockDelayState()
    lastFallTime = System.nanoTime()
    println(
      s"[Logic] Performed hold. Held: ${heldTetromino.map(_.minoName)}, Current: ${currentFallingTetromino.minoName}. Can hold: $canHold"
    )
    true
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
  def isValidPositionAfterSpawn(
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
    if (gameOver || gameOverPendingAnimation) return false
    if (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x + dx,
        currentFallingTetromino.y,
        currentFallingTetromino.currentShape
      )
    ) {
      currentFallingTetromino.x += dx
      if (isGrounded) {
        resetLockDelayTimer()
        successfulMovesSinceGrounded += 1
      }
      true
    } else {
      false
    }
  }
  def tryRotate(clockwise: Boolean): Boolean = {
    if (
      gameOver || gameOverPendingAnimation || currentFallingTetromino.minoName == "O"
    ) return false

    val originalShape = currentFallingTetromino.currentShape
    val originalX = currentFallingTetromino.x
    val originalY = currentFallingTetromino.y
    val originalOrientation = currentFallingTetromino.orientation

    currentFallingTetromino.rotate(clockwise)
    val rotatedShape = currentFallingTetromino.currentShape

    val baseOffsets = currentFallingTetromino.minoName match {
      case "I" =>
        List((0, 0), (-1, 0), (1, 0), (-2, 0), (2, 0), (0, -1), (0, -2))
      case _ => List((0, 0), (-1, 0), (1, 0), (0, -1))
    }

    var rotatedSuccessfully = false
    if (
      isValidPosition(
        currentFallingTetromino,
        originalX,
        originalY,
        rotatedShape
      )
    ) {
      rotatedSuccessfully = true
    } else {
      for ((offX, offY) <- baseOffsets if !rotatedSuccessfully) {
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
    }

    if (rotatedSuccessfully) {
      if (isGrounded) {
        rotationsSinceGrounded += 1
        successfulMovesSinceGrounded += 1
        if (rotationsSinceGrounded < maxRotationsForLockReset) {
          resetLockDelayTimer()
        }
      }
    } else {
      currentFallingTetromino.currentShape = originalShape
      currentFallingTetromino.x = originalX
      currentFallingTetromino.y = originalY
      currentFallingTetromino.orientation = originalOrientation
    }
    rotatedSuccessfully
  }
  private def resetLockDelayTimer(): Unit = {
    lockDelayStartTime = System.nanoTime()
    currentLockDelayNanos = initialLockDelayNanos
  }
  def performHardDrop(): Unit = {
    if (gameOver || gameOverPendingAnimation) return

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
    isGrounded = true
    lockDelayStartTime = System.nanoTime()
    currentLockDelayNanos = 50_000_000L
    rotationsSinceGrounded = maxRotationsForLockReset
    successfulMovesSinceGrounded = 0
    lastFallTime = System.nanoTime()
  }
  def setSoftDropActive(active: Boolean): Unit = {
    if (gameOver || gameOverPendingAnimation) return
    currentFallInterval =
      if (active) FallIntervalSoftDrop else FallIntervalNormal
  }
  def updateGameTick(currentTime: Long): Boolean = {
    var stateChanged = false
    if (gameOver || gameOverPendingAnimation) {
      return false
    }

    if (isGrounded) {
      if (currentTime - lockDelayStartTime >= currentLockDelayNanos) {
        fixAndSpawnNew()
        stateChanged = true
      } else {
        if (
          isValidPosition(
            currentFallingTetromino,
            currentFallingTetromino.x,
            currentFallingTetromino.y + 1,
            currentFallingTetromino.currentShape
          )
        ) {
          isGrounded = false
          resetLockDelayState()
          stateChanged = true
        }
      }
    } else {
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
          isGrounded = true
          lockDelayStartTime = currentTime
          currentLockDelayNanos = initialLockDelayNanos
          rotationsSinceGrounded = 0
          successfulMovesSinceGrounded = 0
          stateChanged = true
        }
      }
    }
    stateChanged
  }
  private def checkIfBlocksAreAboveBoard(
    checkX: Int,
    checkY: Int,
    shape: Array[Array[Int]]
  ): Boolean = {
    for (r <- shape.indices; c <- shape(r).indices) {
      if (shape(r)(c) == 1) {
        if (checkY + r < 0) return true
      }
    }
    false
  }
  private def fixAndSpawnNew(): Unit = {
    fixTetromino()

    val linesClearedCount = clearLines()
    if (linesClearedCount > 0) {
      score += (linesClearedCount * 100 * linesClearedCount)
      println(s"[Logic] Lines Cleared: $linesClearedCount, Total Score: $score")
    }

    assignNewFallingTetromino()

    if (
      !isValidPositionAfterSpawn(
        currentFallingTetromino.x,
        currentFallingTetromino.y,
        currentFallingTetromino.currentShape
      )
    ) {
      gameOver = true
      gameOverPendingAnimation = true
      println(
        s"[Logic] !!! GAME OVER DETECTED !!! newMino at y=${currentFallingTetromino.y}. gameOver=$gameOver, pendingAnim=$gameOverPendingAnimation"
      )
    } else {
      println(
        s"[Logic] Spawn successful for newMino. gameOver=$gameOver, pendingAnim=$gameOverPendingAnimation"
      )
    }
    lastFallTime = System.nanoTime()
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
