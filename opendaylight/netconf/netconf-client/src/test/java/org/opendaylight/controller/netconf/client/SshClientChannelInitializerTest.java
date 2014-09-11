package org.opendaylight.controller.netconf.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Promise;
import org.junit.Test;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SshClientChannelInitializerTest {
    @Test
    public void test() throws Exception {

        AuthenticationHandler authenticationHandler = mock(AuthenticationHandler.class);
        NetconfClientSessionNegotiatorFactory negotiatorFactory = mock(NetconfClientSessionNegotiatorFactory.class);
        NetconfClientSessionListener sessionListener = mock(NetconfClientSessionListener.class);

        SessionNegotiator sessionNegotiator = mock(SessionNegotiator.class);
        doReturn("").when(sessionNegotiator).toString();
        doReturn(sessionNegotiator).when(negotiatorFactory).getSessionNegotiator(any(SessionListenerFactory.class), any(Channel.class), any(Promise.class));
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        doReturn(pipeline).when(pipeline).addAfter(anyString(), anyString(), any(ChannelHandler.class));
        Channel channel = mock(Channel.class);
        doReturn(pipeline).when(channel).pipeline();
        doReturn("").when(channel).toString();
        doReturn(pipeline).when(pipeline).addFirst(any(ChannelHandler.class));
        doReturn(pipeline).when(pipeline).addLast(anyString(), any(ChannelHandler.class));

        Promise<NetconfClientSession> promise = mock(Promise.class);
        doReturn("").when(promise).toString();

        SshClientChannelInitializer initializer = new SshClientChannelInitializer(authenticationHandler, negotiatorFactory,
                sessionListener);
        initializer.initialize(channel, promise);
        verify(pipeline, times(1)).addFirst(any(ChannelHandler.class));
    }
}
