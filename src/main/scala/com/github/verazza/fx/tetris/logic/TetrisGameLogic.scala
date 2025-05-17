package fx.tetris.logic

import scalafx.scene.paint.Color
import scala.collection.mutable

class TetrisGameLogic {

  import GameConstants._ // 定数をインポート

  // Game State
  var board: Array[Array[Option[Color]]] = Array.fill(NumRows, NumCols)(None)
  var currentFallingTetromino: FallingTetromino = createNewFallingTetromino()
  var gameOver: Boolean = false
  var score: Int = 0

  // Key State (ロジック内でキー状態を直接持つより、コントローラーから指示を受ける方が良いが、今回は簡略化のため一部含む)
  // Animation Timer related state
  var currentFallInterval: Long = FallIntervalNormal
  var lastFallTime: Long = 0L

  def resetGame(): Unit = {
    board = Array.fill(NumRows, NumCols)(None)
    currentFallingTetromino = createNewFallingTetromino()
    gameOver = false
    score = 0
    currentFallInterval = FallIntervalNormal
    lastFallTime = System.nanoTime() // ★ 現在時刻で初期化
  }

  private def createNewFallingTetromino(): FallingTetromino = {
    val (name, shape, color) = Tetromino.getRandomTetromino()
    val tetromino = new FallingTetromino(name, shape, color)
    tetromino.x = NumCols / 2 - shape(0).length / 2
    tetromino.y = 0 // 初期Y座標。場合によっては完全に盤面外から出現させることも
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

        if (boardX < 0 || boardX >= NumCols || boardY >= NumRows) {
          return false
        }
        if (boardY >= 0 && board(boardY)(boardX).isDefined) {
          return false
        }
      }
    }
    true
  }

  def tryMoveHorizontal(dx: Int): Boolean = {
    if (
      isValidPosition(
        currentFallingTetromino,
        currentFallingTetromino.x + dx,
        currentFallingTetromino.y,
        currentFallingTetromino.currentShape
      )
    ) {
      currentFallingTetromino.x += dx
      true
    } else {
      false
    }
  }

  def tryRotate(clockwise: Boolean): Boolean = {
    if (currentFallingTetromino.minoName == "O") return false // Oミノは回転しない

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
    for ((dx, dy) <- kickOffsets if !rotatedSuccessfully) {
      val testX = originalX + dx
      val testY = originalY + dy
      if (
        isValidPosition(currentFallingTetromino, testX, testY, rotatedShape)
      ) {
        currentFallingTetromino.x = testX
        currentFallingTetromino.y = testY
        rotatedSuccessfully = true
      }
    }

    if (!rotatedSuccessfully) {
      currentFallingTetromino.currentShape = originalShape
      currentFallingTetromino.x = originalX
      currentFallingTetromino.y = originalY
      currentFallingTetromino.orientation = originalOrientation
      false
    } else {
      true
    }
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
      // score += (ghostY - currentFallingTetromino.y) * 1 // ハードドロップスコア（任意）
    }
    fixAndSpawnNew()
    lastFallTime = System.nanoTime() // 落下タイマーリセット
  }

  // 外部から落下間隔を変更できるようにするメソッド
  def setSoftDropActive(active: Boolean): Unit = {
    currentFallInterval =
      if (active) FallIntervalSoftDrop else FallIntervalNormal
  }

  def updateGameTick(currentTime: Long): Boolean = { // 状態が更新されたらtrueを返す
    var updated = false
    if (!gameOver) {
      if (currentTime - lastFallTime >= currentFallInterval) {
        moveDownAndFixInternal()
        lastFallTime = currentTime
        updated = true
      }
    }
    updated
  }

  private def moveDownAndFixInternal(): Unit = {
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
      fixAndSpawnNew()
    }
  }

  private def fixAndSpawnNew(): Unit = {
    fixTetromino()
    val linesClearedCount = clearLines()
    if (linesClearedCount > 0) {
      score += (linesClearedCount * 100 * linesClearedCount) // スコア計算例
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
