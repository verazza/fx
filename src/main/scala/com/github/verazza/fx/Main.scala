package fx

trait Gaming {
  lazy val gaming: String = "fx"
  lazy val menuTitle: String = s"ようこそVerazza.${gaming}へ！"
  lazy val tetrisButtonText: String = "テトリス"
}

object Main extends Gaming with App {
  println(s"start ${gaming}!")
  Menu.main(Array.empty[String])
}
