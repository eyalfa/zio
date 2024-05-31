package zio.internal

import java.util.concurrent.atomic.AtomicReference
import scala.annotation.tailrec

class FiberInbox extends AtomicReference(List.empty[FiberMessage]) {
  var dequeued, pending = List.empty[FiberMessage]
  var nonEmptyHint = false

  def isEmpty =
    !nonEmptyHint &&
      (this.get() eq Nil)


  //def nonEmpty = dequeued.nonEmpty || (this.get().nonEmpty)

  def add(msg : FiberMessage): Unit = {
    nonEmptyHint = true
    this.getAndUpdate(msg :: _)
  }

  def addLocal(msg : FiberMessage): Unit = {
    //adds by the fiber itself, these are basically racing with external adds,
    //we can arbitrarily decide they're winning
    pending = msg :: pending
    nonEmptyHint = true
  }

  def poll() : FiberMessage = {
    if(dequeued ne Nil) {
      val res = dequeued.head
      dequeued = dequeued.tail
      res
    } else {
      val reversed = if(pending ne Nil) pending else this.getAndSet(Nil)
      pending = Nil
      nonEmptyHint = false
      if(reversed eq Nil) {
        null
      } else {
        @tailrec def go(rest: ::[FiberMessage]): FiberMessage = {
          val t = rest.tail
          if (t eq Nil)
            rest.head
          else {
            dequeued = rest.head :: dequeued
            nonEmptyHint = true
            go(t.asInstanceOf[::[FiberMessage]])
          }
        }

        go(reversed.asInstanceOf[::[FiberMessage]])
      }
    }
  }


}

object FiberInbox {
  class Node(val msg : FiberMessage, var next : Node)
}