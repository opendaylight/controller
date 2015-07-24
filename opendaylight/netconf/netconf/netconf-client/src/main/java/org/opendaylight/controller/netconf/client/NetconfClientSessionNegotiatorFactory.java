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
import com.google.common.collect.ImmutableSet;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.util.Set;
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

    public static final Set<String> EXI_CLIENT_CAPABILITIES = ImmutableSet.of(
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0);

    public static final Set<String> LEGACY_EXI_CLIENT_CAPABILITIES = ImmutableSet.of(
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0);

    public static final Set<String> DEFAULT_CLIENT_CAPABILITIES = ImmutableSet.of(
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1);

    public static final Set<String> LEGACY_FRAMING_CLIENT_CAPABILITIES = ImmutableSet.of(
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0);

    private static final Logger LOG = LoggerFactory.getLogger(NetconfClientSessionNegotiatorFactory.class);
    private static final String START_EXI_MESSAGE_ID = "default-start-exi";
    private static final EXIOptions DEFAULT_OPTIONS;

    private final Optional<NetconfHelloMessageAdditionalHeader> additionalHeader;
    private final long connectionTimeoutMillis;
    private final Timer timer;
    private final EXIOptions options;

    static {
        final EXIOptions opts = new EXIOptions();
        try {
            opts.setPreserveDTD(true);
            opts.setPreserveNS(true);
            opts.setPreserveLexicalValues(true);
            opts.setAlignmentType(AlignmentType.byteAligned);
        } catch (EXIOptionsException e) {
            throw new ExceptionInInitializerError(e);
        }

        DEFAULT_OPTIONS = opts;
    }

    private final Set<String> clientCapabilities;

    public NetconfClientSessionNegotiatorFactory(final Timer timer,
                                                 final Optional<NetconfHelloMessageAdditionalHeader> additionalHeader,
                                                 final long connectionTimeoutMillis) {
        this(timer, additionalHeader, connectionTimeoutMillis, DEFAULT_OPTIONS);
    }

    public NetconfClientSessionNegotiatorFactory(final Timer timer,
                                                 final Optional<NetconfHelloMessageAdditionalHeader> additionalHeader,
                                                 final long connectionTimeoutMillis, final Set<String> capabilities) {
        this(timer, additionalHeader, connectionTimeoutMillis, DEFAULT_OPTIONS, capabilities);

    }

    public NetconfClientSessionNegotiatorFactory(final Timer timer,
                                                 final Optional<NetconfHelloMessageAdditionalHeader> additionalHeader,
                                                 final long connectionTimeoutMillis, final EXIOptions exiOptions) {
        this(timer, additionalHeader, connectionTimeoutMillis, exiOptions, EXI_CLIENT_CAPABILITIES);
    }

    public NetconfClientSessionNegotiatorFactory(final Timer timer,
                                                 final Optional<NetconfHelloMessageAdditionalHeader> additionalHeader,
                                                 final long connectionTimeoutMillis, final EXIOptions exiOptions, final Set<String> capabilities) {
        this.timer = Preconditions.checkNotNull(timer);
        this.additionalHeader = additionalHeader;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.options = exiOptions;
        this.clientCapabilities = capabilities;
    }

    @Override
    public SessionNegotiator<NetconfClientSession> getSessionNegotiator(final SessionListenerFactory<NetconfClientSessionListener> sessionListenerFactory,
                                                                        final Channel channel,
            final Promise<NetconfClientSession> promise) {

        NetconfMessage startExiMessage = NetconfStartExiMessage.create(options, START_EXI_MESSAGE_ID);
        NetconfHelloMessage helloMessage = null;
        try {
            helloMessage = NetconfHelloMessage.createClientHello(clientCapabilities, additionalHeader);
        } catch (NetconfDocumentedException e) {
            LOG.error("Unable to create client hello message with capabilities {} and additional handler {}", clientCapabilities,additionalHeader);
            throw new IllegalStateException(e);
        }

        NetconfClientSessionPreferences proposal = new NetconfClientSessionPreferences(helloMessage, startExiMessage);
        return new NetconfClientSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(),connectionTimeoutMillis);
    }
}
