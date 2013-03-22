
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.openflow.io;

import org.openflow.protocol.*;
import org.openflow.protocol.factory.BasicFactory;

import java.util.*;
import java.nio.channels.*;
import java.net.InetSocketAddress;

import org.junit.Assert;

import org.junit.Test;


/**
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 *
 */
public class OFMessageAsyncStreamTest {
    @Test
    public void testMarshalling() throws Exception {
        OFMessage h = new OFHello();
        
        ServerSocketChannel serverSC = ServerSocketChannel.open();
        serverSC.socket().bind(new java.net.InetSocketAddress(0));
        serverSC.configureBlocking(false);
        
        SocketChannel client = SocketChannel.open(
                new InetSocketAddress("localhost",
                        serverSC.socket().getLocalPort())
                );
        SocketChannel server = serverSC.accept();
        OFMessageAsyncStream clientStream = new OFMessageAsyncStream(client, new BasicFactory());
        OFMessageAsyncStream serverStream = new OFMessageAsyncStream(server, new BasicFactory());
        
        clientStream.write(h);
        while(clientStream.needsFlush()) {
            clientStream.flush();
        }
        List<OFMessage> l = serverStream.read();
        Assert.assertEquals(l.size(), 1);
        OFMessage m = l.get(0);
        Assert.assertEquals(m.getLength(),h.getLength());
        Assert.assertEquals(m.getVersion(), h.getVersion());
        Assert.assertEquals(m.getType(), h.getType());
        Assert.assertEquals(m.getType(), h.getType());
    }
}
