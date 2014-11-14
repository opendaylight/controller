/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.osgi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Optional;
import io.netty.channel.local.LocalAddress;
import java.net.InetSocketAddress;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class NetconfConfigUtilTest {

    private BundleContext bundleContext;

    @Before
    public void setUp() throws Exception {
        bundleContext = mock(BundleContext.class);
    }

    @Test
    public void testNetconfConfigUtil() throws Exception {
        assertEquals(NetconfConfigUtil.getNetconfLocalAddress(), new LocalAddress("netconf"));

        doReturn("").when(bundleContext).getProperty("netconf.connectionTimeoutMillis");
        assertEquals(NetconfConfigUtil.extractTimeoutMillis(bundleContext), 5000);

        doReturn("a").when(bundleContext).getProperty("netconf.connectionTimeoutMillis");
        assertEquals(NetconfConfigUtil.extractTimeoutMillis(bundleContext), 5000);
    }

    @Test
    public void testgetPrivateKeyKey() throws Exception {
        assertEquals(NetconfConfigUtil.getPrivateKeyKey(), "netconf.ssh.pk.path");
    }

    @Test
    public void testgetNetconfServerAddressKey() throws Exception {
        NetconfConfigUtil.InfixProp prop = NetconfConfigUtil.InfixProp.tcp;
        assertEquals(NetconfConfigUtil.getNetconfServerAddressKey(prop), "netconf.tcp.address");
    }

    @Test
    public void testExtractNetconfServerAddress() throws Exception {
        NetconfConfigUtil.InfixProp prop = NetconfConfigUtil.InfixProp.tcp;
        doReturn("").when(bundleContext).getProperty(anyString());
        assertEquals(NetconfConfigUtil.extractNetconfServerAddress(bundleContext, prop), Optional.absent());
    }

    @Test
    public void testExtractNetconfServerAddress2() throws Exception {
        NetconfConfigUtil.InfixProp prop = NetconfConfigUtil.InfixProp.tcp;
        doReturn("1.1.1.1").when(bundleContext).getProperty("netconf.tcp.address");
        doReturn("20").when(bundleContext).getProperty("netconf.tcp.port");
        Optional<InetSocketAddress> inetSocketAddressOptional = NetconfConfigUtil.extractNetconfServerAddress(bundleContext, prop);
        assertTrue(inetSocketAddressOptional.isPresent());
        assertEquals(inetSocketAddressOptional.get(), new InetSocketAddress("1.1.1.1", 20));
    }

    @Test
    public void testGetPrivateKeyPath() throws Exception {
        doReturn("path").when(bundleContext).getProperty("netconf.ssh.pk.path");
        assertEquals(NetconfConfigUtil.getPrivateKeyPath(bundleContext), "path");
    }

    @Test(expected = IllegalStateException.class)
    public void testGetPrivateKeyPath2() throws Exception {
        doReturn(null).when(bundleContext).getProperty("netconf.ssh.pk.path");
        assertEquals(NetconfConfigUtil.getPrivateKeyPath(bundleContext), "path");
    }
}
