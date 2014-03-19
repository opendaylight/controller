/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.util.concurrent.Future;
import org.opendaylight.controller.netconf.api.AbstractNetconfSession;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

public final class NetconfClientSession extends AbstractNetconfSession<NetconfClientSession, NetconfClientSessionListener> {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientSession.class);
    private final Collection<String> capabilities;
    private final String EXI_CAPABILITY_1_0 = "urn:ietf:params:netconf:capability:exi:1.0";


    public NetconfClientSession(NetconfClientSessionListener sessionListener, Channel channel, long sessionId,
            Collection<String> capabilities) {
        super(sessionListener, channel, sessionId);
        this.capabilities = capabilities;
        Iterator<String> capabilitiesIterator = capabilities.iterator();
        logger.info("Creating NetconfClientSession for sessionId:"+sessionId);
        while (capabilitiesIterator.hasNext()){
            String capability = capabilitiesIterator.next();
            if (capability.equals(EXI_CAPABILITY_1_0)){
                //TODO elaborate on this - this seems to be called more than once !!!
                Future<NetconfMessage> futureMessage = ((SimpleNetconfClientSessionListener)sessionListener).sendRequest(loadStartExiTemplate());
                if (futureMessage.isSuccess()){
                    futureMessage.getNow();
                }
                logger.info("Found exi capability - trying to switch session to exi.");
                break;
            }
        }
        logger.debug("Client Session {} created", toString());
    }

    public Collection<String> getServerCapabilities() {
        return capabilities;
    }


    @Override
    protected NetconfClientSession thisInstance() {
        return this;
    }
    private static NetconfMessage loadStartExiTemplate() {
        final String messagePath = "/startExi.xml";
        try (InputStream is = NetconfClientSessionNegotiatorFactory.class.getResourceAsStream(messagePath)) {
            Preconditions.checkState(is != null, "Input stream from %s was null", messagePath);
            return new NetconfMessage(XmlUtil.readXmlToDocument(is));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to load start-exi message", e);
        }
    }
    private static NetconfMessage loadStopExiTemplate() {
        final String messagePath = "/stopExi.xml";
        try (InputStream is = NetconfClientSessionNegotiatorFactory.class.getResourceAsStream(messagePath)) {
            Preconditions.checkState(is != null, "Input stream from %s was null", messagePath);
            return new NetconfMessage(XmlUtil.readXmlToDocument(is));
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Unable to load stop-exi message", e);
        }
    }

}
