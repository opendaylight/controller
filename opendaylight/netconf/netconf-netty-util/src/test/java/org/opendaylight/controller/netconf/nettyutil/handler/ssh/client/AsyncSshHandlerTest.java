/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.nettyutil.handler.ssh.client;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.net.SocketAddress;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.channel.ChannelSubsystem;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.future.OpenFuture;
import org.apache.sshd.common.future.CloseFuture;
import org.apache.sshd.common.future.SshFuture;
import org.apache.sshd.common.future.SshFutureListener;
import org.apache.sshd.common.io.IoInputStream;
import org.apache.sshd.common.io.IoOutputStream;
import org.apache.sshd.common.io.IoReadFuture;
import org.apache.sshd.common.io.IoWriteFuture;
import org.apache.sshd.common.util.Buffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;

public class AsyncSshHandlerTest {

    @Mock
    private SshClient sshClient;
    @Mock
    private AuthenticationHandler authHandler;
    @Mock
    private ChannelHandlerContext ctx;
    @Mock
    private Channel channel;
    @Mock
    private SocketAddress remoteAddress;
    @Mock
    private SocketAddress localAddress;

    private AsyncSshHandler asyncSshHandler;

    private SshFutureListener<ConnectFuture> sshConnectListener;
    private SshFutureListener<AuthFuture> sshAuthListener;
    private SshFutureListener<OpenFuture> sshChannelOpenListener;

    private ChannelPromise promise;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        stubAuth();
        stubSshClient();
        stubChannel();
        stubCtx();
        stubRemoteAddress();

        promise = getMockedPromise();

        asyncSshHandler = new AsyncSshHandler(authHandler, sshClient);
    }

    @After
    public void tearDown() throws Exception {
        sshConnectListener = null;
        sshAuthListener = null;
        sshChannelOpenListener = null;
        promise = null;
        asyncSshHandler.close(ctx, getMockedPromise());
    }

    private void stubAuth() throws IOException {
        doReturn("usr").when(authHandler).getUsername();

        final AuthFuture authFuture = mock(AuthFuture.class);
        Futures.addCallback(stubAddListener(authFuture), new SuccessFutureListener<AuthFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<AuthFuture> result) {
                sshAuthListener = result;
            }
        });
        doReturn(authFuture).when(authHandler).authenticate(any(ClientSession.class));
    }

    @SuppressWarnings("unchecked")
    private <T extends SshFuture<T>> ListenableFuture<SshFutureListener<T>> stubAddListener(final T future) {
        final SettableFuture<SshFutureListener<T>> listenerSettableFuture = SettableFuture.create();

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                listenerSettableFuture.set((SshFutureListener<T>) invocation.getArguments()[0]);
                return null;
            }
        }).when(future).addListener(any(SshFutureListener.class));

        return listenerSettableFuture;
    }

    private void stubRemoteAddress() {
        doReturn("remote").when(remoteAddress).toString();
    }

    private void stubCtx() {
        doReturn(channel).when(ctx).channel();
        doReturn(ctx).when(ctx).fireChannelActive();
        doReturn(ctx).when(ctx).fireChannelInactive();
        doReturn(ctx).when(ctx).fireChannelRead(anyObject());
        doReturn(getMockedPromise()).when(ctx).newPromise();
    }

    private void stubChannel() {
        doReturn("channel").when(channel).toString();
    }

    private void stubSshClient() {
        doNothing().when(sshClient).start();
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        Futures.addCallback(stubAddListener(connectFuture), new SuccessFutureListener<ConnectFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<ConnectFuture> result) {
                sshConnectListener = result;
            }
        });
        doReturn(connectFuture).when(sshClient).connect("usr", remoteAddress);
    }

    @Test
    public void testConnectSuccess() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        verify(subsystemChannel).setStreaming(ClientChannel.Streaming.Async);

        verify(promise).setSuccess();
        verifyNoMoreInteractions(promise);
        verify(ctx).fireChannelActive();
    }

    @Test
    public void testRead() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        verify(ctx).fireChannelRead(any(ByteBuf.class));
    }

    @Test
    public void testReadClosed() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoReadFuture mockedReadFuture = asyncOut.read(null);

        Futures.addCallback(stubAddListener(mockedReadFuture), new SuccessFutureListener<IoReadFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<IoReadFuture> result) {
                doReturn(new IllegalStateException()).when(mockedReadFuture).getException();
                doReturn(mockedReadFuture).when(mockedReadFuture).removeListener(Matchers.<SshFutureListener<IoReadFuture>>any());
                doReturn(true).when(asyncOut).isClosing();
                doReturn(true).when(asyncOut).isClosed();
                result.operationComplete(mockedReadFuture);
            }
        });

        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        verify(ctx).fireChannelInactive();
    }

    @Test
    public void testReadFail() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoReadFuture mockedReadFuture = asyncOut.read(null);

        Futures.addCallback(stubAddListener(mockedReadFuture), new SuccessFutureListener<IoReadFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<IoReadFuture> result) {
                doReturn(new IllegalStateException()).when(mockedReadFuture).getException();
                doReturn(mockedReadFuture).when(mockedReadFuture).removeListener(Matchers.<SshFutureListener<IoReadFuture>>any());
                result.operationComplete(mockedReadFuture);
            }
        });

        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        verify(ctx).fireChannelInactive();
    }

    @Test
    public void testWrite() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        final ChannelPromise writePromise = getMockedPromise();
        asyncSshHandler.write(ctx, Unpooled.copiedBuffer(new byte[]{0, 1, 2, 3, 4, 5}), writePromise);

        verify(writePromise).setSuccess();
    }

    @Test
    public void testWriteClosed() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();

        final IoWriteFuture ioWriteFuture = asyncIn.write(null);

        Futures.addCallback(stubAddListener(ioWriteFuture), new SuccessFutureListener<IoWriteFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<IoWriteFuture> result) {
                doReturn(false).when(ioWriteFuture).isWritten();
                doReturn(new IllegalStateException()).when(ioWriteFuture).getException();
                doReturn(true).when(asyncIn).isClosing();
                doReturn(true).when(asyncIn).isClosed();
                result.operationComplete(ioWriteFuture);
            }
        });

        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        final ChannelPromise writePromise = getMockedPromise();
        asyncSshHandler.write(ctx, Unpooled.copiedBuffer(new byte[]{0,1,2,3,4,5}), writePromise);

        verify(writePromise).setFailure(any(Throwable.class));
    }

    @Test
    public void testWritePendingOne() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final IoWriteFuture ioWriteFuture = asyncIn.write(null);

        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        final ChannelPromise firstWritePromise = getMockedPromise();

        // intercept listener for first write, so we can invoke successful write later thus simulate pending of the first write
        final ListenableFuture<SshFutureListener<IoWriteFuture>> firstWriteListenerFuture = stubAddListener(ioWriteFuture);
        asyncSshHandler.write(ctx, Unpooled.copiedBuffer(new byte[]{0,1,2,3,4,5}), firstWritePromise);
        final SshFutureListener<IoWriteFuture> firstWriteListener = firstWriteListenerFuture.get();
        // intercept second listener, this is the listener for pending write for the pending write to know when pending state ended
        final ListenableFuture<SshFutureListener<IoWriteFuture>> pendingListener = stubAddListener(ioWriteFuture);

        final ChannelPromise secondWritePromise = getMockedPromise();
        // now make write throw pending exception
        doThrow(org.apache.sshd.common.io.WritePendingException.class).when(asyncIn).write(any(Buffer.class));
        asyncSshHandler.write(ctx, Unpooled.copiedBuffer(new byte[]{0, 1, 2, 3, 4, 5}), secondWritePromise);

        doReturn(ioWriteFuture).when(asyncIn).write(any(Buffer.class));

        verifyZeroInteractions(firstWritePromise, secondWritePromise);

        // make first write stop pending
        firstWriteListener.operationComplete(ioWriteFuture);

        // notify listener for second write that pending has ended
        pendingListener.get().operationComplete(ioWriteFuture);

        // verify both write promises successful
        verify(firstWritePromise).setSuccess();
        verify(secondWritePromise).setSuccess();
    }

    @Ignore("Pending queue is not limited")
    @Test
    public void testWritePendingMax() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final IoWriteFuture ioWriteFuture = asyncIn.write(null);

        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        final ChannelPromise firstWritePromise = getMockedPromise();

        // intercept listener for first write, so we can invoke successful write later thus simulate pending of the first write
        final ListenableFuture<SshFutureListener<IoWriteFuture>> firstWriteListenerFuture = stubAddListener(ioWriteFuture);
        asyncSshHandler.write(ctx, Unpooled.copiedBuffer(new byte[]{0,1,2,3,4,5}), firstWritePromise);

        final ChannelPromise secondWritePromise = getMockedPromise();
        // now make write throw pending exception
        doThrow(org.apache.sshd.common.io.WritePendingException.class).when(asyncIn).write(any(Buffer.class));
        for (int i = 0; i < 1001; i++) {
            asyncSshHandler.write(ctx, Unpooled.copiedBuffer(new byte[]{0, 1, 2, 3, 4, 5}), secondWritePromise);
        }

        verify(secondWritePromise, times(1)).setFailure(any(Throwable.class));
    }

    @Test
    public void testDisconnect() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);
        sshAuthListener.operationComplete(getSuccessAuthFuture());
        sshChannelOpenListener.operationComplete(getSuccessOpenFuture());

        final ChannelPromise disconnectPromise = getMockedPromise();
        asyncSshHandler.disconnect(ctx, disconnectPromise);

        verify(sshSession).close(anyBoolean());
        verify(disconnectPromise).setSuccess();
        verify(ctx).fireChannelInactive();
    }

    private OpenFuture getSuccessOpenFuture() {
        final OpenFuture failedOpenFuture = mock(OpenFuture.class);
        doReturn(true).when(failedOpenFuture).isOpened();
        return failedOpenFuture;
    }

    private AuthFuture getSuccessAuthFuture() {
        final AuthFuture authFuture = mock(AuthFuture.class);
        doReturn(true).when(authFuture).isSuccess();
        return authFuture;
    }

    private ConnectFuture getSuccessConnectFuture(final ClientSession sshSession) {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        doReturn(true).when(connectFuture).isConnected();

        doReturn(sshSession).when(connectFuture).getSession();
        return connectFuture;
    }

    private ClientSession getMockedSshSession(final ChannelSubsystem subsystemChannel) throws IOException {
        final ClientSession sshSession = mock(ClientSession.class);

        doReturn("sshSession").when(sshSession).toString();
        doReturn("serverVersion").when(sshSession).getServerVersion();
        doReturn(false).when(sshSession).isClosed();
        doReturn(false).when(sshSession).isClosing();
        final CloseFuture closeFuture = mock(CloseFuture.class);
        Futures.addCallback(stubAddListener(closeFuture), new SuccessFutureListener<CloseFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<CloseFuture> result) {
                doReturn(true).when(closeFuture).isClosed();
                result.operationComplete(closeFuture);
            }
        });
        doReturn(closeFuture).when(sshSession).close(false);

        doReturn(subsystemChannel).when(sshSession).createSubsystemChannel(anyString());

        return sshSession;
    }

    private ChannelSubsystem getMockedSubsystemChannel(final IoInputStream asyncOut, final IoOutputStream asyncIn) throws IOException {
        final ChannelSubsystem subsystemChannel = mock(ChannelSubsystem.class);
        doReturn("subsystemChannel").when(subsystemChannel).toString();

        doNothing().when(subsystemChannel).setStreaming(any(ClientChannel.Streaming.class));
        final OpenFuture openFuture = mock(OpenFuture.class);

        Futures.addCallback(stubAddListener(openFuture), new SuccessFutureListener<OpenFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<OpenFuture> result) {
                sshChannelOpenListener = result;
            }
        });

        doReturn(asyncOut).when(subsystemChannel).getAsyncOut();

        doReturn(openFuture).when(subsystemChannel).open();
        doReturn(asyncIn).when(subsystemChannel).getAsyncIn();
        return subsystemChannel;
    }

    private IoOutputStream getMockedIoOutputStream() {
        final IoOutputStream mock = mock(IoOutputStream.class);
        final IoWriteFuture ioWriteFuture = mock(IoWriteFuture.class);
        doReturn(ioWriteFuture).when(ioWriteFuture).addListener(Matchers.<SshFutureListener<IoWriteFuture>>any());
        doReturn(true).when(ioWriteFuture).isWritten();

        Futures.addCallback(stubAddListener(ioWriteFuture), new SuccessFutureListener<IoWriteFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<IoWriteFuture> result) {
                result.operationComplete(ioWriteFuture);
            }
        });

        doReturn(ioWriteFuture).when(mock).write(any(Buffer.class));
        doReturn(false).when(mock).isClosed();
        doReturn(false).when(mock).isClosing();
        return mock;
    }

    private IoInputStream getMockedIoInputStream() {
        final IoInputStream mock = mock(IoInputStream.class);
        final IoReadFuture ioReadFuture = mock(IoReadFuture.class);
        doReturn(null).when(ioReadFuture).getException();
        doReturn(ioReadFuture).when(ioReadFuture).removeListener(Matchers.<SshFutureListener<IoReadFuture>>any());
        doReturn(5).when(ioReadFuture).getRead();
        doReturn(new Buffer(new byte[]{0, 1, 2, 3, 4})).when(ioReadFuture).getBuffer();
        doReturn(ioReadFuture).when(ioReadFuture).addListener(Matchers.<SshFutureListener<IoReadFuture>>any());

        // Always success for read
        Futures.addCallback(stubAddListener(ioReadFuture), new SuccessFutureListener<IoReadFuture>() {
            @Override
            public void onSuccess(final SshFutureListener<IoReadFuture> result) {
                result.operationComplete(ioReadFuture);
            }
        });

        doReturn(ioReadFuture).when(mock).read(any(Buffer.class));
        doReturn(false).when(mock).isClosed();
        doReturn(false).when(mock).isClosing();
        return mock;
    }

    @Test
    public void testConnectFailOpenChannel() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final IoInputStream asyncOut = getMockedIoInputStream();
        final IoOutputStream asyncIn = getMockedIoOutputStream();
        final ChannelSubsystem subsystemChannel = getMockedSubsystemChannel(asyncOut, asyncIn);
        final ClientSession sshSession = getMockedSshSession(subsystemChannel);
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);

        sshAuthListener.operationComplete(getSuccessAuthFuture());

        verify(subsystemChannel).setStreaming(ClientChannel.Streaming.Async);

        try {
            sshChannelOpenListener.operationComplete(getFailedOpenFuture());
            fail("Exception expected");
        } catch (final Exception e) {
            verify(promise).setFailure(any(Throwable.class));
            verifyNoMoreInteractions(promise);
            // TODO should ctx.channelInactive be called if we throw exception ?
        }
    }

    @Test
    public void testConnectFailAuth() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final ClientSession sshSession = mock(ClientSession.class);
        doReturn(true).when(sshSession).isClosed();
        final ConnectFuture connectFuture = getSuccessConnectFuture(sshSession);

        sshConnectListener.operationComplete(connectFuture);

        final AuthFuture authFuture = getFailedAuthFuture();

        try {
            sshAuthListener.operationComplete(authFuture);
            fail("Exception expected");
        } catch (final Exception e) {
            verify(promise).setFailure(any(Throwable.class));
            verifyNoMoreInteractions(promise);
            // TODO should ctx.channelInactive be called ?
        }
    }

    private AuthFuture getFailedAuthFuture() {
        final AuthFuture authFuture = mock(AuthFuture.class);
        doReturn(false).when(authFuture).isSuccess();
        doReturn(new IllegalStateException()).when(authFuture).getException();
        return authFuture;
    }

    private OpenFuture getFailedOpenFuture() {
        final OpenFuture authFuture = mock(OpenFuture.class);
        doReturn(false).when(authFuture).isOpened();
        doReturn(new IllegalStateException()).when(authFuture).getException();
        return authFuture;
    }

    @Test
    public void testConnectFail() throws Exception {
        asyncSshHandler.connect(ctx, remoteAddress, localAddress, promise);

        final ConnectFuture connectFuture = getFailedConnectFuture();
        try {
            sshConnectListener.operationComplete(connectFuture);
            fail("Exception expected");
        } catch (final Exception e) {
            verify(promise).setFailure(any(Throwable.class));
            verifyNoMoreInteractions(promise);
            // TODO should ctx.channelInactive be called ?
        }
    }

    private ConnectFuture getFailedConnectFuture() {
        final ConnectFuture connectFuture = mock(ConnectFuture.class);
        doReturn(false).when(connectFuture).isConnected();
        doReturn(new IllegalStateException()).when(connectFuture).getException();
        return connectFuture;
    }

    private ChannelPromise getMockedPromise() {
        final ChannelPromise promise = mock(ChannelPromise.class);
        doReturn(promise).when(promise).setSuccess();
        doReturn(promise).when(promise).setFailure(any(Throwable.class));
        return promise;
    }

    private static abstract class SuccessFutureListener<T extends SshFuture<T>> implements FutureCallback<SshFutureListener<T>> {

        @Override
        public abstract void onSuccess(final SshFutureListener<T> result);

        @Override
        public void onFailure(final Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
