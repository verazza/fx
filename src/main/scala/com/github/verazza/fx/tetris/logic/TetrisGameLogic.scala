package fx.tetris.logic

import scalafx.scene.paint.Color
// import scala.collection.mutable // TetrisGameLogic では直接使わない

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
  var isGrounded: Boolean = false
  var lockDelayStartTime: Long = 0L
  val initialLockDelayNanos: Long = 500_000_000L
  var currentLockDelayNanos: Long = initialLockDelayNanos
  var rotationsSinceGrounded: Int = 0
  val maxRotationsForLockReset: Int = 15
  var successfulMovesSinceGrounded: Int = 0

  var gameOverPendingAnimation: Boolean = false

  // Hold機能関連のétat
  var heldTetromino: Option[FallingTetromino] = None // ホールドされているミノ
  var canHold: Boolean = true // 現在のターンでホールドが可能か

  def resetGame(): Unit = {
    board = Array.fill(NumRows, NumCols)(None)
    currentFallingTetromino =
      createNewFallingTetromino() // これが resetLockDelayState と canHold もリセット
    gameOver = false
    gameOverPendingAnimation = false
    score = 0
    currentFallInterval = FallIntervalNormal
    lastFallTime = System.nanoTime()
    heldTetromino = None // ホールドミノもリセット
    // canHold は createNewFallingTetromino 内で true になる
    println("[Logic] Game Reset")
  }

  private def resetLockDelayState(): Unit = {
    isGrounded = false
    lockDelayStartTime = 0L
    rotationsSinceGrounded = 0
    successfulMovesSinceGrounded = 0
    currentLockDelayNanos = initialLockDelayNanos
  }

  private def createNewFallingTetromino(
    isFromHold: Boolean = false
  ): FallingTetromino = {
    if (!isFromHold) { // ホールドからの呼び出しでなければホールド権をリセット
      canHold = true
    }
    resetLockDelayState()
    val (name, shape, color) = Tetromino.getRandomTetromino()
    val tetromino = new FallingTetromino(name, shape, color)
    var minYInShape = shape.length
    for (r <- shape.indices; c <- shape(r).indices) {
      if (shape(r)(c) == 1) minYInShape = Math.min(minYInShape, r)
    }
    tetromino.x = NumCols / 2 - shape(0).length / 2
    tetromino.y = -minYInShape
    println(
      s"[Logic] Created new tetromino: ${name} at y=${tetromino.y}, x=${tetromino.x}. Can hold: $canHold"
    )
    tetromino
  }

  // ホールド操作を実行するメソッド
  def performHold(): Boolean = {
    if (!canHold || gameOver || gameOverPendingAnimation) return false

    val previouslyHeld = heldTetromino // 現在ホールドされているミノ (あれば)
    heldTetromino = Some(
      new FallingTetromino( // 現在のミノをホールド用にコピー (位置や向きはリセットされる想定)
        currentFallingTetromino.minoName,
        Tetromino.tetrominoData
          .find(_._1 == currentFallingTetromino.minoName)
          .get
          ._2, // 元の形状を取得
        currentFallingTetromino.color
      )
    )

    previouslyHeld match {
      case Some(minoFromHold) =>
        // ホールドされていたミノを現在のミノとして再設定
        currentFallingTetromino = new FallingTetromino(
          minoFromHold.minoName,
          minoFromHold.currentShape,
          minoFromHold.color
        )
        // 初期位置にリセット
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
        // ホールドが空だったので、新しいミノを生成
        currentFallingTetromino = createNewFallingTetromino(isFromHold =
          true
        ) // ホールドからの生成なのでホールド権は消費しない
    }

    canHold = false // このターンではもうホールドできない
    resetLockDelayState() // 新しいミノ（またはホールドから来たミノ）のロック遅延状態をリセット
    lastFallTime = System.nanoTime() // 落下タイミングもリセット
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

        // 盤面外のチェック
        if (boardX < 0 || boardX >= NumCols || boardY >= NumRows) return false
        // 盤面の上限チェック (boardY < 0 は許容するが、固定ブロックとの衝突は盤面内のみ)
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

    // 基本キック + 床/壁からの押し出しを試みる拡張オフセット
    // (0,0)は基本。(-1,0),(1,0)は左右。
    // (0,-1)は上に蹴り上げる(フロアキック)。Iミノはさらに遠くまでテスト。
    val baseOffsets = currentFallingTetromino.minoName match {
      case "I" =>
        List(
          (0, 0),
          (-1, 0),
          (1, 0),
          (-2, 0),
          (2, 0),
          (0, -1),
          (0, -2)
        ) // Iミノは上下にも大きくキック
      case _ => List((0, 0), (-1, 0), (1, 0), (0, -1)) // 他のミノは基本的なキック + 上1
    }
    // さらに、ミノが床や壁に既に近い場合の追加テスト (例: (0, -1) はフロアキックの試み)
    // val floorKickOffsets = List((0, -1), (1, -1), (-1, -1)) // 必要に応じて追加

    var rotatedSuccessfully = false
    // まずは元の位置で試す
    if (
      isValidPosition(
        currentFallingTetromino,
        originalX,
        originalY,
        rotatedShape
      )
    ) {
      // currentFallingTetromino.x, y は変更しない (回転のみ)
      rotatedSuccessfully = true
    } else {
      // キックオフセットを試す
      for ((offX, offY) <- baseOffsets if !rotatedSuccessfully) {
        val testX = originalX + offX
        val testY = originalY + offY // Yオフセットも考慮
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
    currentLockDelayNanos = initialLockDelayNanos // 基本のロック遅延に戻す
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
    currentLockDelayNanos = 50_000_000L // ハードドロップ後は非常に短いロック遅延 (0.05秒)
    rotationsSinceGrounded = maxRotationsForLockReset // 回転による延長はほぼなし
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
    if (gameOver || gameOverPendingAnimation) return false // ゲームオーバー処理中は更新しない

    if (isGrounded) {
      if (currentTime - lockDelayStartTime >= currentLockDelayNanos) {
        // ミノが盤面からはみ出て固定されようとしていないか最終チェック
        if (
          checkIfBlocksAreAboveBoard(
            currentFallingTetromino.x,
            currentFallingTetromino.y,
            currentFallingTetromino.currentShape
          )
        ) {
          // このケースは通常、出現直後のゲームオーバー判定で捕捉されるべきだが、
          // ロック遅延中に状況が変わることは稀。念のため。
          // gameOver = true
          // gameOverPendingAnimation = true
          // return true // ゲームオーバー状態に遷移
        }
        fixAndSpawnNew() // これが gameOver を true にする可能性がある
        stateChanged = true
      } else {
        // ロック遅延中。足場が消えたかチェック
        if (
          isValidPosition(
            currentFallingTetromino,
            currentFallingTetromino.x,
            currentFallingTetromino.y + 1,
            currentFallingTetromino.currentShape
          )
        ) {
          isGrounded = false // 足場がなくなったので自由落下に戻る
          resetLockDelayState() // isGrounded が false になるので、次のtickで自然落下へ
          stateChanged = true
        }
      }
    } else { // isGrounded == false
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
        } else { // 接地した
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
        if (checkY + r < 0) return true // ブロックの一部が盤面の上にある
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

    // canHold = true // ★ ミノが固定されたので、次のミノではホールド可能にする
    // -> createNewFallingTetromino で isFromHold = false の場合に true に設定されるのでここで直接は不要

    val newMino = createNewFallingTetromino() // isFromHold = false (デフォルト)

    if (
      !isValidPositionAfterSpawn(newMino.x, newMino.y, newMino.currentShape)
    ) {
      gameOver = true
      gameOverPendingAnimation = true
      println(
        s"[Logic] !!! GAME OVER DETECTED !!! newMino at y=${newMino.y}. gameOver=$gameOver, pendingAnim=$gameOverPendingAnimation"
      )
    } else {
      currentFallingTetromino = newMino
    }
    lastFallTime = System.nanoTime()
  }

  // 出現直後のisValidPosition。y < 0 の場合も考慮
  private def isValidPositionAfterSpawn(
    testX: Int,
    testY: Int,
    testShape: Array[Array[Int]]
  ): Boolean = {
    for (r <- testShape.indices; c <- testShape(r).indices) {
      if (testShape(r)(c) == 1) {
        val boardX = testX + c
        val boardY = testY + r
        // 壁チェックは同じ
        if (boardX < 0 || boardX >= NumCols || boardY >= NumRows) return false
        // 固定ブロックとの衝突は y >= 0 の範囲のみ
        if (boardY >= 0 && board(boardY)(boardX).isDefined) return false
      }
    }
    true
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
        ) { // 盤面内のみ固定
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
