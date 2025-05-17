package fx.tetris.logic

import scalafx.scene.paint.Color
import scala.util.Random

object Tetromino {
  val rd = new Random

  val iShape: Array[Array[Int]] = Array(
    Array(0, 0, 0, 0),
    Array(1, 1, 1, 1),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val tShape: Array[Array[Int]] = Array(
    Array(0, 1, 0, 0),
    Array(1, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val lShape: Array[Array[Int]] = Array(
    Array(0, 0, 1, 0),
    Array(1, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val jShape: Array[Array[Int]] = Array(
    Array(1, 0, 0, 0),
    Array(1, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val sShape: Array[Array[Int]] = Array(
    Array(0, 1, 1, 0),
    Array(1, 1, 0, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val zShape: Array[Array[Int]] = Array(
    Array(1, 1, 0, 0),
    Array(0, 1, 1, 0),
    Array(0, 0, 0, 0),
    Array(0, 0, 0, 0)
  )

  val oShape: Array[Array[Int]] =
    Array( // O Shape is often 2x2, fitting in a 4x4 grid.
      Array(0, 1, 1, 0),
      Array(0, 1, 1, 0),
      Array(0, 0, 0, 0),
      Array(0, 0, 0, 0)
    )

  // またはよりコンパクトなO Shape (2x2)
  // val oShape: Array[Array[Int]] = Array(
  //   Array(1, 1),
  //   Array(1, 1)
  // )
  // この場合、FallingTetrominoの回転ロジックや初期位置決めを少し調整する必要があるかもしれません。
  // 今回は4x4の枠内で定義されたものを使います。

  val tetrominoData: Seq[(String, Array[Array[Int]], Color)] = Seq(
    ("I", iShape, Color.Cyan),
    ("T", tShape, Color.Purple),
    ("L", lShape, Color.Orange),
    ("J", jShape, Color.Blue),
    ("S", sShape, Color.Green),
    ("Z", zShape, Color.Red),
    ("O", oShape, Color.Yellow)
  )

  def getRandomTetromino(): (Array[Array[Int]], Color) = {
    val randomIndex = rd.nextInt(tetrominoData.length)
    val selected = tetrominoData(randomIndex)
    (selected._2, selected._3)
  }

  // Tetromino.Getterは直接は使わず、getRandomTetrominoで新しいミノ情報を取得するようにします。
  // 既存のコードでGetterを使っている部分は、このgetRandomTetrominoを使うように変更します。}
}
