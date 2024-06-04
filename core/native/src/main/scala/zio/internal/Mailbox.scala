package zio.internal

import zio.stacktracer.TracingImplicits.disableAutoTrace

class Mailbox[A] extends Serializable {

  protected var read      = new MailboxNode(null)
  private[this] var write = read

  final def add(data: A): Unit = {
    val next = new MailboxNode(data.asInstanceOf[AnyRef])
    write.next = next
    write = next
  }

  final def isEmpty(): Boolean =
    null == read.next

  final def nonEmpty(): Boolean =
    null != read.next

  final def poll(): A = {
    val next = read.next

    if (null == next)
      return null.asInstanceOf[A]

    val data = next.data
    next.data = null
    read = next
    data.asInstanceOf[A]
  }
}

private[internal] class MailboxNode(var data: AnyRef, var next: MailboxNode = null)
