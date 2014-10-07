/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.io.WritePendingException;
import org.apache.sshd.common.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async Ssh writer. Takes messages(byte arrays) and sends them encrypted to remote server.
 * Also handles pending writes by caching requests until pending state is over.
 */
public final class AsyncSshHandlerWriter implements AutoCloseable {

    private static final Logger logger = LoggerFactory
            .getLogger(AsyncSshHandlerWriter.class);

    // public static final int MAX_PENDING_WRITES = 1000;
    // TODO implement Limiting mechanism for pending writes
    // But there is a possible issue with limiting:
    // 1. What to do when queue is full ? Immediate Fail for every request ?
    // 2. At this level we might be dealing with Chunks of messages(not whole messages) and unexpected behavior might occur
    // when we send/queue 1 chunk and fail the other chunks

    private IoOutputStream asyncIn;

    // Order has to be preserved for queued writes
    private final Deque<PendingWriteRequest> pending = new LinkedList<>();

    public AsyncSshHandlerWriter(final IoOutputStream asyncIn) {
        this.asyncIn = asyncIn;
    }

    public synchronized void write(final ChannelHandlerContext ctx,
            final Object msg, final ChannelPromise promise) {
        // TODO check for isClosed, isClosing might be performed by mina SSH internally and is not required here
        // If we are closed/closing, set immediate fail
        if (asyncIn == null || asyncIn.isClosed() || asyncIn.isClosing()) {
            promise.setFailure(new IllegalStateException("Channel closed"));
        } else {
            final ByteBuf byteBufMsg = (ByteBuf) msg;
            if (pending.isEmpty() == false) {
                queueRequest(ctx, byteBufMsg, promise);
                return;
            }

            writeWithPendingDetection(ctx, promise, byteBufMsg);
        }
    }

    private void writeWithPendingDetection(final ChannelHandlerContext ctx, final ChannelPromise promise, final ByteBuf byteBufMsg) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace("Writing request on channel: {}, message: {}", ctx.channel(), byteBufToString(byteBufMsg));
            }
            asyncIn.write(toBuffer(byteBufMsg)).addListener(new SshFutureListener<IoWriteFuture>() {

                        @Override
                        public void operationComplete(final IoWriteFuture future) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Ssh write request finished on channel: {} with result: {}: and ex:{}, message: {}",
                                        ctx.channel(), future.isWritten(), future.getException(), byteBufToString(byteBufMsg));
                            }

                            // Notify success or failure
                            if (future.isWritten()) {
                                promise.setSuccess();
                            } else {
                                logger.warn("Ssh write request failed on channel: {} for message: {}", ctx.channel(), byteBufToString(byteBufMsg), future.getException());
                                promise.setFailure(future.getException());
                            }

                            // Not needed anymore, release
                            byteBufMsg.release();

                            // Check pending queue and schedule next
                            // At this time we are guaranteed that we are not in pending state anymore so the next request should succeed
                            writePendingIfAny();
                        }
                    });
        } catch (final WritePendingException e) {
            queueRequest(ctx, byteBufMsg, promise);
        }
    }

    private synchronized void writePendingIfAny() {
        if (pending.peek() == null) {
            return;
        }

        // In case of pending, reschedule next message from queue
        final PendingWriteRequest pendingWrite = pending.poll();
        final ByteBuf msg = pendingWrite.msg;
        if (logger.isTraceEnabled()) {
            logger.trace("Writing pending request on channel: {}, message: {}", pendingWrite.ctx.channel(), byteBufToString(msg));
        }

        writeWithPendingDetection(pendingWrite.ctx, pendingWrite.promise, msg);
    }

    public static String byteBufToString(final ByteBuf msg) {
        msg.resetReaderIndex();
        final String s = msg.toString(Charsets.UTF_8);
        msg.resetReaderIndex();
        return s;
    }

    private void queueRequest(final ChannelHandlerContext ctx, final ByteBuf msg, final ChannelPromise promise) {
//        try {
        logger.debug("Write pending on channel: {}, queueing, current queue size: {}", ctx.channel(), pending.size());
        if (logger.isTraceEnabled()) {
            logger.trace("Queueing request due to pending: {}", byteBufToString(msg));
        }
        new PendingWriteRequest(ctx, msg, promise).pend(pending);
//        } catch (final Exception ex) {
//            logger.warn("Unable to queue write request on channel: {}. Setting fail for the request: {}", ctx.channel(), ex, byteBufToString(msg));
//            msg.release();
//            promise.setFailure(ex);
//        }
    }

    @Override
    public synchronized void close() {
        asyncIn = null;
    }

    private Buffer toBuffer(final ByteBuf msg) {
        // TODO Buffer vs ByteBuf translate, Can we handle that better ?
        final byte[] temp = new byte[msg.readableBytes()];
        msg.readBytes(temp, 0, msg.readableBytes());
        return new Buffer(temp);
    }

    private static final class PendingWriteRequest {
        private final ChannelHandlerContext ctx;
        private final ByteBuf msg;
        private final ChannelPromise promise;

        public PendingWriteRequest(final ChannelHandlerContext ctx, final ByteBuf msg, final ChannelPromise promise) {
            this.ctx = ctx;
            // Reset reader index, last write (failed) attempt moved index to the end
            msg.resetReaderIndex();
            this.msg = msg;
            this.promise = promise;
        }

        public void pend(final Queue<PendingWriteRequest> pending) {
            // Preconditions.checkState(pending.size() < MAX_PENDING_WRITES,
            // "Too much pending writes(%s) on channel: %s, remote window is not getting read or is too small",
            // pending.size(), ctx.channel());
            Preconditions.checkState(pending.offer(this), "Cannot pend another request write (pending count: %s) on channel: %s",
                    pending.size(), ctx.channel());
        }
    }
}
