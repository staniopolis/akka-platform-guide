package shopping.cart

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorSystem
import akka.management.scaladsl.AkkaManagement
import org.slf4j.LoggerFactory
import scala.util.control.NonFatal

object Main {

  val logger = LoggerFactory.getLogger("shopping.cart.Main")

  def main(args: Array[String]): Unit = {
    val system = ActorSystem[Nothing](Behaviors.empty, "ShoppingCartService")
    try {
      AkkaManagement(system).start()
    } catch {
      case NonFatal(e) =>
        logger.error("Terminating due to initialization failure.", e)
        system.terminate()
    }
  }

}
