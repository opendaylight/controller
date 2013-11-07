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
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.netconf.util.handler.ssh.virtualsocket.VirtualSocketException;

/**
 * Worker thread class. Handles all downstream and upstream events in SSH Netty pipeline.
 */
public class SshClientAdapter implements Runnable {
    private final SshClient sshClient;
    private final Invoker invoker;

    private SshSession session;
    private InputStream stdOut;
    private InputStream stdErr;
    private OutputStream stdIn;

    private ChannelHandlerContext ctx;
    private ChannelPromise disconnectPromise;

    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private final Object lock = new Object();

    public SshClientAdapter(SshClient sshClient,
                            Invoker invoker) {
        this.sshClient = sshClient;
        this.invoker = invoker;
    }

    public void run() {
        try {
            session = sshClient.openSession();
            invoker.invoke(session);

            stdOut = session.getStdout();
            stdErr = session.getStderr();

            synchronized(lock) {
                stdIn = session.getStdin();
            }

            while (stopRequested.get() == false) {
                byte[] readBuff = new byte[1024];
                int c = stdOut.read(readBuff);

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
                if(disconnectPromise != null) ctx.disconnect(disconnectPromise);
            }
        }
    }

    // TODO: needs rework to match netconf framer API.
    public void write(String message) throws IOException {
        synchronized (lock) {
            if (stdIn == null) throw new IllegalStateException("StdIn not available");
        }
        stdIn.write(message.getBytes());
        stdIn.flush();
    }

    public void stop(ChannelPromise promise) {
        synchronized (lock) {
            stopRequested.set(true);
            disconnectPromise = promise;
        }
    }

    public void start(ChannelHandlerContext ctx) {
        if(this.ctx != null) return; // context is already associated.

        this.ctx = ctx;
        new Thread(this).start();
    }
}
