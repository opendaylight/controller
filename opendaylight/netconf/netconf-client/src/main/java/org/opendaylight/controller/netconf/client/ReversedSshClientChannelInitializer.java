/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client;

import io.netty.channel.ChannelHandler;
import java.io.IOException;
import java.net.SocketAddress;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.sshd.common.channel.ChannelAsyncOutputStream;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.DefaultCloseFuture;
import org.apache.sshd.common.io.IoService;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.Buffer;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.ReversedAsyncSshHandler;

final class ReversedSshClientChannelInitializer extends SshClientChannelInitializer {

    private final IoSession tcpSession;

    public ReversedSshClientChannelInitializer(final AuthenticationHandler authHandler,
                                               final NetconfClientSessionNegotiatorFactory negotiatorFactory,
                                               final NetconfClientSessionListener sessionListener, final IoSession tcpSession) {
        super(authHandler, negotiatorFactory, sessionListener);
        this.tcpSession = tcpSession;
    }

    @Override
    protected ChannelHandler getSshHandler() throws IOException {
        return ReversedAsyncSshHandler.createForNetconfSubsystem(getAuthenticationHandler(), new org.apache.sshd.common.io.IoSession() {
            @Override
            public long getId() {
                return tcpSession.getId();
            }

            @Override
            public Object getAttribute(final Object key) {
                return tcpSession.getAttribute(key);
            }

            @Override
            public Object setAttribute(final Object key, final Object value) {
                return tcpSession.getAttribute(key, value);
            }

            @Override
            public SocketAddress getRemoteAddress() {
                return tcpSession.getRemoteAddress();
            }

            @Override
            public SocketAddress getLocalAddress() {
                return tcpSession.getLocalAddress();
            }

            @Override
            public IoWriteFuture write(final Buffer buffer) {
                final ChannelAsyncOutputStream.IoWriteFutureImpl ioWriteFuture = new ChannelAsyncOutputStream.IoWriteFutureImpl(buffer);
                final WriteFuture write = tcpSession.write(buffer);
                write.addListener(new IoFutureListener<WriteFuture>() {
                    @Override
                    public void operationComplete(final WriteFuture future) {
                        if (future.isWritten()) {
                            ioWriteFuture.setValue(true);
                        } else {
                            // TODO check the value type expected + if exception can go there
                            ioWriteFuture.setValue(future.getException());
                        }
                    }
                });

                return ioWriteFuture;
            }

            @Override
            public CloseFuture close(final boolean immediately) {
                final DefaultCloseFuture defaultCloseFuture = new DefaultCloseFuture(null);
                tcpSession.close(immediately).addListener(new IoFutureListener<org.apache.mina.core.future.CloseFuture>() {
                    @Override
                    public void operationComplete(final org.apache.mina.core.future.CloseFuture future) {
                        if(future.isClosed()) {
                            defaultCloseFuture.setValue(true);
                        } else {
                            // TODO check the value type expected
                            defaultCloseFuture.setValue(false);
                        }
                    }
                });
                return defaultCloseFuture;
            }

            @Override
            public IoService getService() {
                throw new UnsupportedOperationException("No service available");
            }

            @Override
            public boolean isClosed() {
                return tcpSession.getCloseFuture().isClosed();
            }

            @Override
            public boolean isClosing() {
                return tcpSession.isClosing();
            }
        });
    }
}
