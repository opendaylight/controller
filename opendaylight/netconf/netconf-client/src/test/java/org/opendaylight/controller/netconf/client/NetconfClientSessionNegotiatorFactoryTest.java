package org.opendaylight.controller.netconf.client;

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.apache.sshd.common.SessionListener;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class NetconfClientSessionNegotiatorFactoryTest {
    @Test
    public void testGetSessionNegotiator() throws Exception {
        NetconfClientSessionListener sessionListener = mock(NetconfClientSessionListener.class);
        Timer timer = new HashedWheelTimer();
        SessionListenerFactory listenerFactory = mock(SessionListenerFactory.class);
        doReturn(sessionListener).when(listenerFactory).getSessionListener();

        Channel channel = mock(Channel.class);
        Promise promise = mock(Promise.class);
        NetconfClientSessionNegotiatorFactory negotiatorFactory = new NetconfClientSessionNegotiatorFactory(timer,
                Optional.<NetconfHelloMessageAdditionalHeader>absent(), 200L);

        SessionNegotiator sessionNegotiator = negotiatorFactory.getSessionNegotiator(listenerFactory, channel, promise);
        assertNotNull(sessionNegotiator);
    }
}
