package zio.internal;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * An unbounded MPSC (multi-producers single consumer) queue that orders
 * elements FIFO (first-in-first-out).
 * 
 * @apiNote The queue is thread-safe provided {@code poll} is invoked by the
 *          single consumer (thread).
 * 
 * @implNote The implementation employs an algorithm described in <a href=
 *           "https://www.1024cores.net/home/lock-free-algorithms/queues/non-intrusive-mpsc-node-based-queue">
 *           Non-intrusive MPSC node-based queue</a> by D. Vyukov.
 */
public class Mailbox<A> implements Serializable {

	protected transient Node read;
	@SuppressWarnings("unused")
	private transient volatile Node write;

	public Mailbox() {
		this.read = this.write = new Node(null);
	}

	/**
	 * Adds the specified element to the queue.
	 */
	final public void add(A data) {
		Node next = new Node(data);
		Node prev = WRITE.getAndSet(this, next);
		//NEXT.lazySet(prev, next);
		prev.lazySetNext(next);
	}

	/**
	 * Returns {@code true} if the queue has no elements. Otherwise, returns
	 * {@code false}.
	 */
	final public boolean isEmpty() {
		return null == read.getNext();
	}

	/**
	 * Returns {@code true} if the queue has elements. Otherwise, returns
	 * {@code false}.
	 */
	final public boolean nonEmpty() {
		return null != read.getNext();
	}

	/**
	 * Removes and returns the oldest element in the queue if the queue has
	 * elements. Otherwise, returns {@code null}.
	 * 
	 * @apiNote This method must be invoked by the single consumer (thread).
	 */
	final public A poll() {
		Node next = read.plainGetNext();

		if (next == null)
			return null;

		@SuppressWarnings("unchecked")
		A data = (A) next.data;
		next.data = null;
		this.read = next;
		if(data == null)
			return poll();
		return data;
	}

	static class Node extends AtomicReference<Node> {
		Object data;
		//volatile Node next;

		Node(Object data) {
			this(data, null);
		}

		Node(Object data, Node next) {
			super(next);
			this.data = data;
			//this.next = next;
		}

		void lazySetNext(Node next) {
			this.lazySet(next);
		}

		Node getNext() {
			return this.get();
		}

		Node plainGetNext() {
			return this.getPlain();
		}
	}

	@SuppressWarnings("rawtypes")
	private static final AtomicReferenceFieldUpdater<Mailbox, Node> WRITE = AtomicReferenceFieldUpdater
			.newUpdater(Mailbox.class, Node.class, "write");
	/*static final AtomicReferenceFieldUpdater<Node, Node> NEXT = AtomicReferenceFieldUpdater.newUpdater(Node.class,
			Node.class, "next");*/
}
