/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.util.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener on async input stream from SSH session.
 * This listeners schedules reads in a loop until the session is closed or read fails.
 */
public final class AsyncSshHandlerReader implements SshFutureListener<IoReadFuture>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncSshHandlerReader.class);

    private static final int BUFFER_SIZE = 8192;

    private final AutoCloseable connectionClosedCallback;
    private final ReadMsgHandler readHandler;

    private final String channelId;
    private IoInputStream asyncOut;
    private Buffer buf;
    private IoReadFuture currentReadFuture;

    public AsyncSshHandlerReader(final AutoCloseable connectionClosedCallback, final ReadMsgHandler readHandler, final String channelId, final IoInputStream asyncOut) {
        this.connectionClosedCallback = connectionClosedCallback;
        this.readHandler = readHandler;
        this.channelId = channelId;
        this.asyncOut = asyncOut;
        buf = new Buffer(BUFFER_SIZE);
        asyncOut.read(buf).addListener(this);
    }

    @Override
    public synchronized void operationComplete(final IoReadFuture future) {
        if(future.getException() != null) {
            if(asyncOut.isClosed() || asyncOut.isClosing()) {
                // Ssh dropped
                LOG.debug("Ssh session dropped on channel: {}", channelId, future.getException());
            } else {
                LOG.warn("Exception while reading from SSH remote on channel {}", channelId, future.getException());
            }
            invokeDisconnect();
            return;
        }

        if (future.getRead() > 0) {
            final ByteBuf msg = Unpooled.wrappedBuffer(buf.array(), 0, future.getRead());
            if(LOG.isTraceEnabled()) {
                LOG.trace("Reading message on channel: {}, message: {}", channelId, AsyncSshHandlerWriter.byteBufToString(msg));
            }
            readHandler.onMessageRead(msg);

            // Schedule next read
            buf = new Buffer(BUFFER_SIZE);
            currentReadFuture = asyncOut.read(buf);
            currentReadFuture.addListener(this);
        }
    }

    private void invokeDisconnect() {
        try {
            connectionClosedCallback.close();
        } catch (final Exception e) {
            // This should not happen
            throw new IllegalStateException(e);
        }
    }

    @Override
    public synchronized void close() {
        // Remove self as listener on close to prevent reading from closed input
        if(currentReadFuture != null) {
            currentReadFuture.removeListener(this);
            currentReadFuture = null;
        }

        asyncOut = null;
    }

    public interface ReadMsgHandler {

        void onMessageRead(ByteBuf msg);
    }
}
