package zio.internal

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class FiberInbox extends AtomicReference[AnyRef](FiberInbox.MsgQueue.Nil) {

  def isEmpty = {
    val curr = this.get()
    curr eq FiberInbox.MsgQueue.Nil
  }

  def add(msg : FiberMessage): Unit = {
    this.getAndUpdate{curr =>
      if(curr eq FiberInbox.MsgQueue.Nil)
        msg
      else if(curr.isInstanceOf[FiberMessage])
        FiberInbox.MsgQueue.Two(curr.asInstanceOf[FiberMessage], msg)
      else
        curr.asInstanceOf[FiberInbox.MsgQueue].enqueue(msg)
    }
  }

  def addLocal(msg : FiberMessage): Unit = {
    add(msg)
  }

  def poll() : FiberMessage = {
    val curr = this.getOpaque
    if (curr eq FiberInbox.MsgQueue.Nil) //might be false negative, but a subsequent isEmpty will figure this out
      null
    else if(curr.isInstanceOf[FiberMessage] &&
      this.compareAndSet(curr, FiberInbox.MsgQueue.Nil)
    )
      curr.asInstanceOf[FiberMessage]
    else {
      val curr2 = this.getAndUpdate{
        case q : FiberInbox.MsgQueue =>
          q.tail
        case _ =>
          FiberInbox.MsgQueue.Nil
      }

      if(curr2.isInstanceOf[FiberMessage])
        curr2.asInstanceOf[FiberMessage]
      else {
        val currQ = curr2.asInstanceOf[FiberInbox.MsgQueue]
        if(currQ.isEmpty) null else currQ.head
      }
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