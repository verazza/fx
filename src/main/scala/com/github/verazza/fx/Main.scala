package fx

object Main extends Gaming with App {
  println(s"start ${gaming}!")
  ScalaFXHelloWorld.main(Array.empty[String])
}

trait Gaming {
  lazy val gaming: String = "fx"
}
