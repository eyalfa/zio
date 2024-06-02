package zio.internal

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class FiberInbox extends AtomicReference[FiberInbox.MsgQueue](FiberInbox.MsgQueue.Nil) {
  var nonEmptyHint = false

  def isEmpty =
    !nonEmptyHint &&
      (this.get().isEmpty)

  def add(msg : FiberMessage): Unit = {
    nonEmptyHint = true
    this.getAndUpdate(_.enqueue(msg))
  }

  def addLocal(msg : FiberMessage): Unit = {
    add(msg)
  }

  def poll() : FiberMessage = {
    val curr = this.get()
    if (curr.isEmpty)
      null
    else {
      nonEmptyHint = !curr.tail.isEmpty
      if(!this.compareAndSet(curr, curr.tail)) {
        //curr.tail may be non empty, but in any case the CAS failures indicates another add
        nonEmptyHint = true
        this.updateAndGet(_.tail)
      }
      curr.head
    }
  }
}

object FiberInbox {
  sealed trait MsgQueue {
    val isEmpty : Boolean = false
    def head : FiberMessage
    def tail : MsgQueue

    def enqueue(msg : FiberMessage) : MsgQueue
  }

  object MsgQueue {
    //read optimized queue impl
    case object Nil extends MsgQueue {

      override val isEmpty: Boolean = true

      override def head: FiberMessage = ???

      override def tail: MsgQueue = this

      def enqueue(msg : FiberMessage) =
        Single(msg)
    }

    case class Single(head: FiberMessage) extends MsgQueue {
      override def tail: MsgQueue = Nil

      def enqueue(msg : FiberMessage) =
        Two(head, msg)
    }

    case class Two(head : FiberMessage, next : FiberMessage) extends MsgQueue {
      override lazy val tail: MsgQueue = Single(next)
      def enqueue(msg : FiberMessage) =
        Cons(head,
          Two(next, msg)
        )
    }

    case class Cons(head : FiberMessage, tail : MsgQueue) extends MsgQueue {
      def enqueue(msg : FiberMessage) =
        copy(tail = tail.enqueue(msg))
    }
  }
}