/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.handler.ssh.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.netconf.util.handler.ssh.virtualsocket.VirtualSocketException;


/**
 * Worker thread class. Handles all downstream and upstream events in SSH Netty
 * pipeline.
 */
public class SshClientAdapter implements Runnable {
    private static final int BUFFER_SIZE = 1024;

    private final SshClient sshClient;
    private final Invoker invoker;

    private OutputStream stdIn;

    private Queue<ByteBuf> postponed = new LinkedList<>();

    private ChannelHandlerContext ctx;
    private ChannelPromise disconnectPromise;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final Object lock = new Object();

    public SshClientAdapter(SshClient sshClient, Invoker invoker) {
        this.sshClient = sshClient;
        this.invoker = invoker;
    }

    public void run() {
        try {
            SshSession session = sshClient.openSession();
            invoker.invoke(session);
            InputStream stdOut = session.getStdout();
            session.getStderr();

            synchronized (lock) {

                stdIn = session.getStdin();
                ByteBuf message;
                while ((message = postponed.poll()) != null) {
                    writeImpl(message);
                }
            }

            while (stopRequested.get() == false) {
                byte[] readBuff = new byte[BUFFER_SIZE];
                int c = stdOut.read(readBuff);
                if (c == -1) {
                    continue;
                }
                byte[] tranBuff = new byte[c];
                System.arraycopy(readBuff, 0, tranBuff, 0, c);

                ByteBuf byteBuf = Unpooled.buffer(c);
                byteBuf.writeBytes(tranBuff);
                ctx.fireChannelRead(byteBuf);
            }

        } catch (VirtualSocketException e) {
            // Netty closed connection prematurely.
            // Just pass and move on.
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    public void write(ByteBuf message) throws IOException {
        synchronized (lock) {
            if (stdIn == null) {
                postponed.add(message);
                return;
            }
            writeImpl(message);
        }
    }

    private void writeImpl(ByteBuf message) throws IOException {
        message.getBytes(0, stdIn, message.readableBytes());
        stdIn.flush();
    }

    public void stop(ChannelPromise promise) {
        synchronized (lock) {
            stopRequested.set(true);
            disconnectPromise = promise;
        }
    }

    public void start(ChannelHandlerContext ctx) {
        if (this.ctx != null) {
            // context is already associated.
            return;
        }
        this.ctx = ctx;
        new Thread(this).start();
    }
}
