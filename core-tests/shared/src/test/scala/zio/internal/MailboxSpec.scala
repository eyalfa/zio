package zio.internal

import zio.ZIOBaseSpec
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object MailboxSpec extends ZIOBaseSpec {

  def spec =
    suite("Mailbox")(
      test("preserves elements")(
        check(Gen.chunkOf1(Gen.uuid)) { expected =>
          val q = new Mailbox[AnyRef]
          for {
            consumer <- ZIO
                          .succeed(q.poll())
                          .repeatWhile(_ == null)
                          .replicateZIO(expected.length)
                          .fork
            produce = (a: AnyRef) => ZIO.succeedBlocking(q.add(a))
            _ <-
              ZIO
                .withParallelism(expected.length)(
                  ZIO.foreachParDiscard(expected)(produce)
                )
                .fork
            actual <- consumer.join
          } yield assert(actual)(hasSameElements(expected))
        }
      ),
      test("preserves insertion order with a single producer")(
        check(Gen.chunkOf1(Gen.uuid)) { expected =>
          val q = new Mailbox[AnyRef]
          for {
            consumer <- ZIO
                          .succeed(q.poll())
                          .repeatWhile(_ == null)
                          .replicateZIO(expected.length)
                          .fork
            _      <- ZIO.succeedBlocking(expected.foreach(q.add)).fork
            actual <- consumer.join
          } yield assert(actual)(Assertion.equalTo(expected.toChunk))
        }
      ),

      suite("FiberMailbox") (
        test("enqueue + prepends are visible to poll") {
          val mailbox = new FiberMailbox
          val m1 = FiberMessage.Resume(ZIO.succeed(1))
          val m2 = FiberMessage.InterruptSignal(zio.Cause.empty)
          val m3 = FiberMessage.Resume(ZIO.succeed(2))
          mailbox.add(m1)
          mailbox.add(m2)
          mailbox.prepend2(FiberMessage.YieldNow, FiberMessage.resumeUnit)

          val bldr = zio.Chunk.newBuilder[FiberMessage]
          while(!mailbox.isEmpty) {
            val m = mailbox.poll()
            bldr += m
          }
          val polled = bldr.result()
          mailbox.add(m3)
          assert(mailbox.nonEmpty())(Assertion.isTrue)  &&
          assert(polled)(Assertion.equalTo(zio.Chunk(FiberMessage.YieldNow, FiberMessage.resumeUnit, m1, m2)))
        }
      ),
      test("prepends + enqueue are visible to poll") {
        val mailbox = new FiberMailbox
        val m1 = FiberMessage.Resume(ZIO.succeed(1))
        val m2 = FiberMessage.InterruptSignal(zio.Cause.empty)
        val m3 = FiberMessage.Resume(ZIO.succeed(2))
        mailbox.prepend2(FiberMessage.YieldNow, FiberMessage.resumeUnit)
        mailbox.add(m1)
        mailbox.add(m2)

        val bldr = zio.Chunk.newBuilder[FiberMessage]
        while(!mailbox.isEmpty) {
          val m = mailbox.poll()
          bldr += m
        }
        val polled = bldr.result()
        mailbox.add(m3)
        assert(mailbox.nonEmpty())(Assertion.isTrue)  &&
          assert(polled)(Assertion.equalTo(zio.Chunk(FiberMessage.YieldNow, FiberMessage.resumeUnit, m1, m2)))
      }
    )
}
