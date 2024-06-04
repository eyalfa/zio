package zio.internal

class FiberMailbox extends Mailbox[FiberMessage] {

  def prepend1(m1: FiberMessage): Unit = {
    val node1 = new MailboxNode(m1, this.read.next)
    this.read.next = node1
  }

  def prepend2(m1: FiberMessage, m2: FiberMessage): Unit = {
    val node2 = new MailboxNode(m2, this.read.next)
    val node1 = new MailboxNode(m1, node2)
    this.read.next = node1
  }
}
