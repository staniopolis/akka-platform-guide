package shopping.order

import akka.actor.typed.ActorRef
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.DurableStateBehavior
import akka.persistence.typed.scaladsl.StateEffect

object ShoppingOrder {

  trait Command extends CborSerializable

  trait Response extends CborSerializable
  case class MissingOrder(id: String, msg: String) extends Response
  case class CurrentOrder(id: String, item: Map[String, Int]) extends Response

  case class CreateOrder(id: String, items: Map[String, Int]) extends Command
  case class ReadOrder(id: String, replyTo: ActorRef[Response]) extends Command
  case class UpdateOrder(id: String, items: Map[String, Int]) extends Command
  case class DeleteOrder(id: String, replyTo: ActorRef[String]) extends Command

  sealed trait Order extends CborSerializable
  case object EmptyOrder extends Order

  case class Item(id: String, quantity: Int) extends CborSerializable
  case class PlacedOrder(id: String, items: List[Item]) extends Order

  def create(id: String): DurableStateBehavior[Command, Order] =
    DurableStateBehavior(
      persistenceId = PersistenceId("order", id),
      emptyState = EmptyOrder,
      commandHandler = {
        case (EmptyOrder, cmd)         => createOrder(cmd)
        case (order: PlacedOrder, cmd) => applyCommand(order, cmd)
      })

  def createOrder(cmd: Command): StateEffect[Order] =
    cmd match {
      case CreateOrder(id, itemsMap) =>
        val items = itemsMap.map { case (id, quantity) =>
          Item(id, quantity)
        }.toList
        StateEffect.persist(PlacedOrder(id, items))

      case ReadOrder(id, replyTo) =>
        StateEffect.none.thenReply(replyTo) { r =>
          MissingOrder(id, s"Order $id not found")
        }

      case _: UpdateOrder => StateEffect.unhandled
      case _: DeleteOrder => StateEffect.unhandled
    }

  def applyCommand(order: PlacedOrder, cmd: Command): StateEffect[Order] =
    cmd match {
      case _: CreateOrder => StateEffect.unhandled

      case ReadOrder(id, replyTo) =>
        val itemsMap = order.items.map(i => (i.id, i.quantity)).toMap
        StateEffect.none.thenReply(replyTo) { _ => CurrentOrder(id, itemsMap) }

      case UpdateOrder(id, itemsMap) =>
        val items = itemsMap.map { case (id, quantity) =>
          Item(id, quantity)
        }.toList
        StateEffect.persist(order.copy(items = items))

      case DeleteOrder(id, replyTo) =>
        StateEffect.delete().thenReply(replyTo) { s =>
          "Order is cancelled"
        }
    }
}
