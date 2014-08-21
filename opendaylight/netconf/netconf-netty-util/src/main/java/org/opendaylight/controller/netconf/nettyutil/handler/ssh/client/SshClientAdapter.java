/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Worker thread class. Handles all downstream and upstream events in SSH Netty
 * pipeline.
 */
class SshClientAdapter implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SshClientAdapter.class);

    private static final int BUFFER_SIZE = 1024;

    private final SshClient sshClient;
    private final Invoker invoker;

    private OutputStream stdIn;

    private final Queue<ByteBuf> postponed = new LinkedList<>();

    private ChannelHandlerContext ctx;
    private ChannelPromise disconnectPromise;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final Object lock = new Object();

    public SshClientAdapter(final SshClient sshClient, final Invoker invoker) {
        this.sshClient = sshClient;
        this.invoker = invoker;
    }

    // TODO ganymed spawns a Thread that receives the data from remote inside TransportManager
    // Get rid of this thread and reuse Ganymed internal thread (not sure if its possible without modifications in ganymed)
    public void run() {
        try {
            final SshSession session = sshClient.openSession();
            invoker.invoke(session);
            final InputStream stdOut = session.getStdout();

            synchronized (lock) {
                stdIn = session.getStdin();
                while (postponed.peek() != null) {
                    writeImpl(postponed.poll());
                }
            }

            while (!stopRequested.get()) {
                final byte[] readBuff = new byte[BUFFER_SIZE];
                final int c = stdOut.read(readBuff);
                if (c == -1) {
                    continue;
                }

                ctx.fireChannelRead(Unpooled.copiedBuffer(readBuff, 0, c));
            }
        } catch (final Exception e) {
            logger.error("Unexpected exception", e);
        } finally {
            sshClient.close();

            synchronized (lock) {
                if (disconnectPromise != null) {
                    ctx.disconnect(disconnectPromise);
                }
            }
        }
    }

    // TODO: needs rework to match netconf framer API.
    public void write(final ByteBuf message) throws IOException {
        synchronized (lock) {
            if (stdIn == null) {
                postponed.add(message);
                return;
            }
            writeImpl(message);
        }
    }

    private void writeImpl(final ByteBuf message) throws IOException {
        message.getBytes(0, stdIn, message.readableBytes());
        message.release();
        stdIn.flush();
    }

    public void stop(final ChannelPromise promise) {
        synchronized (lock) {
            stopRequested.set(true);
            disconnectPromise = promise;
        }
    }

    public Thread start(final ChannelHandlerContext ctx, final ChannelFuture channelFuture) {
        checkArgument(channelFuture.isSuccess());
        checkNotNull(ctx.channel().remoteAddress());
        synchronized (this) {
            checkState(this.ctx == null);
            this.ctx = ctx;
        }
        final String threadName = toString();
        final Thread thread = new Thread(this, threadName);
        thread.start();
        return thread;
    }

    @Override
    public String toString() {
        return "SshClientAdapter{" +
                "sshClient=" + sshClient +
                '}';
    }
}
