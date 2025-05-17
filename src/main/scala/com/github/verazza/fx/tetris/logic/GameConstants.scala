package fx.tetris.logic

object GameConstants {
  val NumCols = 10
  val NumRows = 20
  val CellSize = 30
  val BoardWidth: Int = NumCols * CellSize
  val BoardHeight: Int = NumRows * CellSize

  val FallIntervalNormal = 700_000_000L // 通常の落下間隔 (0.7秒)
  val FallIntervalSoftDrop = 50_000_000L // ソフトドロップ時の落下間隔 (0.05秒)

  val DasDelay = 160_000_000L // Auto Shift 開始までの遅延 (0.16秒)
  val ArrInterval = 50_000_000L // Auto Repeat Rate (0.05秒)
}
