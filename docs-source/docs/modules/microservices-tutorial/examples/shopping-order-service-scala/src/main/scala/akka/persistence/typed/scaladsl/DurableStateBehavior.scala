package akka.persistence.typed.scaladsl

import akka.actor.typed.ActorRef
import akka.actor.typed.internal.BehaviorImpl.DeferredBehavior
import akka.persistence.typed.PersistenceId

object DurableStateBehavior {

  type CommandHandler[Command, State] = (State, Command) => StateEffect[State]

  def apply[Command, State](
      persistenceId: PersistenceId,
      emptyState: State,
      commandHandler: CommandHandler[Command, State])
      : DurableStateBehavior[Command, State] = ???
}

trait DurableStateBehavior[Command, State] extends DeferredBehavior[Command] {
  def persistenceId: PersistenceId
}

trait StateEffect[+State]

trait ReplyStateEffect[+State] extends StateEffect[State]

trait StateEffectBuilder[State] extends StateEffect[State] {
  def thenRun(callback: State => Unit): StateEffectBuilder[State]
  def thenStop(): StateEffectBuilder[State]
  def thenReply[ReplyMessage](replyTo: ActorRef[ReplyMessage])(
      replyWithMessage: State => ReplyMessage): ReplyStateEffect[State]
  def thenNoReply(): ReplyStateEffect[State]
}

object StateEffect {

  /**
   * Insert or update.
   *
   * Maybe 'save' was be a better term, but since we
   * already use persist elsewhere, makes sense to keep same term
   */
  def persist[State](state: State): StateEffectBuilder[State] = Persist(state)

  /**
   * Major difference with respect to EventSourced, a CRUD model can be deleted
   *
   * A delete must trigger a stop or reload the empty state
   * In any case, it needs to start from scratch.
   */
  def delete[State](): StateEffectBuilder[State] =
    Delete.asInstanceOf[StateEffectBuilder[State]]

  def none[State]: StateEffectBuilder[State] =
    PersistNothing.asInstanceOf[StateEffectBuilder[State]]

  def unhandled[State]: StateEffectBuilder[State] =
    Unhandled.asInstanceOf[StateEffectBuilder[State]]
}
case class Persist[State](state: State) extends StateEffectImpl[State]
case object Delete extends StateEffectImpl[Nothing]
case object PersistNothing extends StateEffectImpl[Nothing]
case object Unhandled extends StateEffectImpl[Nothing]

abstract class StateEffectImpl[State] extends StateEffectBuilder[State] {
  override def thenRun(callback: State => Unit): StateEffectBuilder[State] =
    this

  override def thenStop(): StateEffectBuilder[State] = this

  override def thenReply[ReplyMessage](replyTo: ActorRef[ReplyMessage])(
      replyWithMessage: State => ReplyMessage): ReplyStateEffect[State] = ???

  override def thenNoReply(): ReplyStateEffect[State] = ???

}
