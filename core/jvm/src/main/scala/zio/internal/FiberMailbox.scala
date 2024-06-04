package zio.internal

class FiberMailbox extends Mailbox[FiberMessage] {

  def prepend1(m1: FiberMessage): Unit = {
    //notice we MUST keep this.read even if its empty, this is required in order to make future adds visible to the reader
    //this results later with isEmpty=false and poll=null, subsequent poll and is ready picks up the rest of the queue (or 'future' adds to the inbox)
    read = new Mailbox.Node(null,
      new Mailbox.Node(m1, this.read))
  }

  def prepend2(m1: FiberMessage, m2: FiberMessage): Unit = {
    //see comment in prepend1
    read = new Mailbox.Node(null,
      new Mailbox.Node(m1,
        new Mailbox.Node(m2, read)
      ))
  }
}
