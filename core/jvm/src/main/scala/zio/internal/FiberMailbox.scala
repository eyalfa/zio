package zio.internal

class FiberMailbox extends Mailbox[FiberMessage] {

  def prepend1(m1: FiberMessage): Unit = {
    val NEXT  = Mailbox.NEXT
    val read  = this.read
    val next  = read.next
    val node1 = new Mailbox.Node(m1, next)

    if (NEXT.compareAndSet(read, next, node1)) return

    NEXT.lazySet(node1, read.next)
    NEXT.lazySet(read, node1)
  }

  def prepend2(m1: FiberMessage, m2: FiberMessage): Unit = {
    val NEXT  = Mailbox.NEXT
    val read  = this.read
    val next  = read.next
    val node2 = new Mailbox.Node(m2, next)
    val node1 = new Mailbox.Node(m1, node2)

    if (NEXT.compareAndSet(read, next, node1)) return

    NEXT.lazySet(node2, read.next)
    NEXT.lazySet(read, node1)
  }
}
