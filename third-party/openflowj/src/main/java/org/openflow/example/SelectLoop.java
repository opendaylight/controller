package org.openflow.example;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * Dirt simple SelectLoop for simple java controller
 */


public class SelectLoop {
    protected SelectListener callback;
    protected boolean dontStop;
    protected Object registrationLock;
    protected int registrationRequests = 0;
    protected Queue<Object[]> registrationQueue;
    protected Selector selector;
    protected long timeout;

    public SelectLoop(SelectListener cb) throws IOException {
        callback = cb;
        dontStop = true;
        selector = SelectorProvider.provider().openSelector();
        registrationLock = new Object();
        registrationQueue = new ConcurrentLinkedQueue<Object[]>();
        timeout = 0;
    }

    /**
     * Initializes this SelectLoop
     * @param cb the callback to call when select returns
     * @param timeout the timeout value in milliseconds that select will be
     *        called with
     * @throws IOException
     */
    public SelectLoop(SelectListener cb, long timeout) throws IOException {
        callback = cb;
        dontStop = true;
        selector = SelectorProvider.provider().openSelector();
        registrationLock = new Object();
        registrationQueue = new ConcurrentLinkedQueue<Object[]>();
        this.timeout = timeout;
    }

    public void register(SelectableChannel ch, int ops, Object arg)
            throws ClosedChannelException {
        registrationQueue.add(new Object[] {ch, ops, arg});
    }

    /**
     * Registers the supplied SelectableChannel with this SelectLoop. Note this
     * method blocks until registration proceeds.  It is advised that
     * SelectLoop is intialized with a timeout value when using this method.
     * @param ch the channel
     * @param ops interest ops
     * @param arg argument that will be returned with the SelectListener
     * @return
     * @throws ClosedChannelException
     */
    public synchronized SelectionKey registerBlocking(SelectableChannel ch, int ops, Object arg)
            throws ClosedChannelException {
        synchronized (registrationLock) {
            registrationRequests++;
        }
        selector.wakeup();
        SelectionKey key = ch.register(selector, ops, arg);
        synchronized (registrationLock) {
            registrationRequests--;
            registrationLock.notifyAll();
        }
        return key;
    }

    /****
     * Main top-level IO loop this dispatches all IO events and timer events
     * together I believe this is fairly efficient
     */
    public void doLoop() throws IOException {
        int nEvents;
        processRegistrationQueue();

        while (dontStop) {
            nEvents = selector.select(timeout);
            if (nEvents > 0) {
                for (Iterator<SelectionKey> i = selector.selectedKeys()
                        .iterator(); i.hasNext();) {
                    SelectionKey sk = i.next();
                    i.remove();

                    if (!sk.isValid())
                        continue;

                    Object arg = sk.attachment();
                    callback.handleEvent(sk, arg);
                }
            }

            if (this.registrationQueue.size() > 0)
                processRegistrationQueue();

            if (registrationRequests > 0) {
                synchronized (registrationLock) {
                    while (registrationRequests > 0) {
                        try {
                            registrationLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    protected void processRegistrationQueue() {
        // add any elements in queue
        for (Iterator<Object[]> it = registrationQueue.iterator(); it.hasNext();) {
            Object[] args = it.next();
            SelectableChannel ch = (SelectableChannel) args[0];
            try {
                ch.register(selector, (Integer) args[1], args[2]);
            } catch (CancelledKeyException cke) {
            	continue;
            } catch (ClosedChannelException e) {
            }
            it.remove();
        }
    }

    /**
     * Force this select loop to return immediately and re-enter select, useful
     * for example if a new item has been added to the select loop while it
     * was already blocked.
     */
    public void wakeup() {
       if (selector != null) {
           selector.wakeup();
       }
    }

    /**
     * Shuts down this select loop, may return before it has fully shutdown
     */
    public void shutdown() {
        this.dontStop = false;
        wakeup();
    }
}
