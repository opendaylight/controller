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
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfClientSessionPreferences;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.nettyutil.handler.exi.NetconfStartExiMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfClientSessionNegotiatorFactory implements SessionNegotiatorFactory<NetconfMessage, NetconfClientSession, NetconfClientSessionListener> {

    public static final java.util.Set<String> CLIENT_CAPABILITIES = Sets.newHashSet(
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0);

    private static final String START_EXI_MESSAGE_ID = "default-start-exi";

    private final Optional<NetconfHelloMessageAdditionalHeader> additionalHeader;
    private final long connectionTimeoutMillis;
    private final Timer timer;
    private final EXIOptions options;
    private static final Logger LOG = LoggerFactory.getLogger(NetconfClientSessionNegotiatorFactory.class);

    public NetconfClientSessionNegotiatorFactory(Timer timer,
                                                 Optional<NetconfHelloMessageAdditionalHeader> additionalHeader,
                                                 long connectionTimeoutMillis) {
        this(timer, additionalHeader, connectionTimeoutMillis, DEFAULT_OPTIONS);
    }

    public NetconfClientSessionNegotiatorFactory(Timer timer,
                                                 Optional<NetconfHelloMessageAdditionalHeader> additionalHeader,
                                                 long connectionTimeoutMillis, EXIOptions exiOptions) {
        this.timer = Preconditions.checkNotNull(timer);
        this.additionalHeader = additionalHeader;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.options = exiOptions;
    }

    @Override
    public SessionNegotiator<NetconfClientSession> getSessionNegotiator(SessionListenerFactory<NetconfClientSessionListener> sessionListenerFactory,
                                                                        Channel channel,
            Promise<NetconfClientSession> promise) {

        NetconfMessage startExiMessage = NetconfStartExiMessage.create(options, START_EXI_MESSAGE_ID);
        NetconfHelloMessage helloMessage = null;
        try {
            helloMessage = NetconfHelloMessage.createClientHello(CLIENT_CAPABILITIES, additionalHeader);
        } catch (NetconfDocumentedException e) {
            LOG.error("Unable to create client hello message with capabilities {} and additional handler {}",CLIENT_CAPABILITIES,additionalHeader);
            throw new IllegalStateException(e);
        }

        NetconfClientSessionPreferences proposal = new NetconfClientSessionPreferences(helloMessage, startExiMessage);
        return new NetconfClientSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(),connectionTimeoutMillis);
    }

    private static final EXIOptions DEFAULT_OPTIONS = new EXIOptions();
    static {
        try {
            DEFAULT_OPTIONS.setPreserveDTD(true);
            DEFAULT_OPTIONS.setPreserveNS(true);
            DEFAULT_OPTIONS.setPreserveLexicalValues(true);
            DEFAULT_OPTIONS.setAlignmentType(AlignmentType.preCompress);
        } catch (EXIOptionsException e) {
            // Should not happen since DEFAULT_OPTIONS are still the same
            throw new IllegalStateException("Unable to create EXI DEFAULT_OPTIONS");
        }
    }
}
