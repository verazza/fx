package fx.tetris.logic

object GameConstants {
  val NumCols = 10
  val NumRows = 20
  val CellSize = 30
  val BoardWidth: Int = NumCols * CellSize
  val BoardHeight: Int = NumRows * CellSize

  val FallIntervalNormal = 700_000_000L
  val FallIntervalSoftDrop = 50_000_000L

  val DasDelay = 160_000_000L
  val ArrInterval = 50_000_000L

  val NumNextToDisplay = 3 // ★ 表示するNextミノの数
}
