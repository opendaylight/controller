/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import java.io.IOException;
import java.io.InputStream;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionPreferences;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.xml.sax.SAXException;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfClientSessionNegotiatorFactory implements SessionNegotiatorFactory {

    private final Timer timer;

    public NetconfClientSessionNegotiatorFactory(Timer timer) {
        this.timer = timer;
    }

    private static NetconfMessage loadHelloMessageTemplate() {
        try (InputStream is = NetconfClientSessionNegotiatorFactory.class.getResourceAsStream("/client_hello.xml")) {
            return new NetconfMessage(XmlUtil.readXmlToDocument(is));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to load hello message", e);
        }
    }

    @Override
    public SessionNegotiator getSessionNegotiator(SessionListenerFactory sessionListenerFactory, Channel channel,
            Promise promise) {
        // Hello message needs to be recreated every time
        NetconfSessionPreferences proposal = new NetconfSessionPreferences(loadHelloMessageTemplate());
        return new NetconfClientSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener());
    }

}
