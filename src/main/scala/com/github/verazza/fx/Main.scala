package fx

object Main extends Gaming with App {
  println(s"start ${gaming}!")
}

trait Gaming {
  lazy val gaming: String = "fx"
}
