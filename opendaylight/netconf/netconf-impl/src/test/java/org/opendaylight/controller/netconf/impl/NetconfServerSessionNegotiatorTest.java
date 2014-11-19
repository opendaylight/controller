package org.opendaylight.controller.netconf.impl;

import io.netty.channel.local.LocalAddress;
import org.apache.sshd.common.SshdSocketAddress;
import org.junit.Test;

import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class NetconfServerSessionNegotiatorTest {

    @Test
    public void testGetInetSocketAddress() throws Exception {

        InetSocketAddress socketAddress = new InetSocketAddress(10);

        assertNotNull(NetconfServerSessionNegotiator.getHostName(socketAddress));

        assertEquals(socketAddress.getHostName(),
                NetconfServerSessionNegotiator.getHostName(socketAddress)
                        .getValue());

        socketAddress = new InetSocketAddress("TestPortInet", 20);

        assertEquals(socketAddress.getHostName(),
                NetconfServerSessionNegotiator.getHostName(socketAddress)
                        .getValue());

        assertEquals(String.valueOf(socketAddress.getPort()),
                NetconfServerSessionNegotiator.getHostName(socketAddress)
                        .getKey());

        LocalAddress localAddress = new LocalAddress("TestPortLocal");

        assertEquals(String.valueOf(localAddress.id()),
                NetconfServerSessionNegotiator.getHostName(localAddress)
                        .getValue());

        SshdSocketAddress embeddedAddress = new SshdSocketAddress(
                "TestSshdName", 10);

    }
}