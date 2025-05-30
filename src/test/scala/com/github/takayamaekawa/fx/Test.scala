package fx

import scala.util.Random

object Test {
  def main(args: Array[String]) {
    val r = new Random
    val r1 = 20 + r.nextInt((30 - 20) + 1)
    println(r1)

    val r2 = r.between(0, 6)
    println(r2)
  }
}
