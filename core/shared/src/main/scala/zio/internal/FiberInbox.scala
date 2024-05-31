package zio.internal

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class FiberInbox extends AtomicReference[AnyRef] {
  var dequeued = List.empty[FiberMessage]

  def isEmpty = dequeued.isEmpty && (this.get() eq null)
  def nonEmpty = dequeued.nonEmpty || (this.get() ne null)

  def enqueue(msg : FiberMessage): Unit = {
    this.getAndUpdate{
      case null => msg
      case prev => (msg, prev)
    }
  }
  
  def poll : FiberMessage = {
    dequeued match {
      case res :: rest =>
        dequeued = rest
        res
      case _ =>
        val reversed = this.getAndSet(null)
        reversed match {
          case null => null
          case msg : FiberMessage => msg
          case (msg : FiberMessage, rest : AnyRef) =>
            dequeued = msg :: Nil
            @tailrec def go(rest : AnyRef) : FiberMessage = {
              rest match {
                case res : FiberMessage =>
                  res
                case (next : FiberMessage, nextRest : AnyRef) =>
                  dequeued = next :: dequeued
                  go(nextRest)
              }
            }
            go(rest)
        }
    }
  }


}
