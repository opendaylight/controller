/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSessionPreferences;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

public class NetconfClientSessionNegotiatorFactory implements SessionNegotiatorFactory {

    private final Timer timer;

    private final Optional<String> additionalHeader;
    private final long connectionTimeoutMillis;

    public NetconfClientSessionNegotiatorFactory(Timer timer, Optional<String> additionalHeader, long connectionTimeoutMillis) {
        this.timer = timer;
        this.additionalHeader = additionalHeader;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    private static NetconfMessage loadHelloMessageTemplate() {
        final String helloMessagePath = "/client_hello.xml";
        try (InputStream is = NetconfClientSessionNegotiatorFactory.class.getResourceAsStream(helloMessagePath)) {
            Preconditions.checkState(is != null, "Input stream from %s was null", helloMessagePath);
            return new NetconfMessage(XmlUtil.readXmlToDocument(is));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to load hello message", e);
        }
    }

    @Override
    public SessionNegotiator getSessionNegotiator(SessionListenerFactory sessionListenerFactory, Channel channel,
            Promise promise) {
        // Hello message needs to be recreated every time
        NetconfMessage helloMessage = loadHelloMessageTemplate();
        if(this.additionalHeader.isPresent()) {
            helloMessage = new NetconfMessage(helloMessage.getDocument(), additionalHeader.get());
        }
        NetconfSessionPreferences proposal = new NetconfSessionPreferences(helloMessage);
        return new NetconfClientSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(), connectionTimeoutMillis);
    }

}
