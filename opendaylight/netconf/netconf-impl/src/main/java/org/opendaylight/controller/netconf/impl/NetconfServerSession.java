/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.nettyutil.AbstractNetconfSession;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfMessageToXMLEncoder;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfXMLToMessageDecoder;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.NetconfTcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.Session1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.extension.rev131210.Session1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfSsh;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Transport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.SessionKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.ZeroBasedCounter32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetconfServerSession extends AbstractNetconfSession<NetconfServerSession, NetconfServerSessionListener> implements NetconfManagementSession {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfServerSession.class);

    private final NetconfHelloMessageAdditionalHeader header;

    private Date loginTime;
    private long inRpcSuccess, inRpcFail, outRpcError;

    public NetconfServerSession(final NetconfServerSessionListener sessionListener, final Channel channel, final long sessionId,
            final NetconfHelloMessageAdditionalHeader header) {
        super(sessionListener, channel, sessionId);
        this.header = header;
        LOG.debug("Session {} created", toString());
    }

    @Override
    protected void sessionUp() {
        Preconditions.checkState(loginTime == null, "Session is already up");
        this.loginTime = new Date();
        super.sessionUp();
    }

    public void onIncommingRpcSuccess() {
        inRpcSuccess++;
    }

    public void onIncommingRpcFail() {
        inRpcFail++;
    }

    public void onOutgoingRpcError() {
        outRpcError++;
    }

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    private static final String dateTimePatternString = DateAndTime.PATTERN_CONSTANTS.get(0);
    private static final Pattern dateTimePattern = Pattern.compile(dateTimePatternString);

    @Override
    public Session toManagementSession() {
        SessionBuilder builder = new SessionBuilder();

        builder.setSessionId(getSessionId());
        builder.setSourceHost(new Host(new DomainName(header.getAddress())));

        Preconditions.checkState(DateAndTime.PATTERN_CONSTANTS.size() == 1);
        String formattedDateTime = formatDateTime(loginTime);

        Matcher matcher = dateTimePattern.matcher(formattedDateTime);
        Preconditions.checkState(matcher.matches(), "Formatted datetime %s does not match pattern %s", formattedDateTime, dateTimePattern);
        builder.setLoginTime(new DateAndTime(formattedDateTime));

        builder.setInBadRpcs(new ZeroBasedCounter32(inRpcFail));
        builder.setInRpcs(new ZeroBasedCounter32(inRpcSuccess));
        builder.setOutRpcErrors(new ZeroBasedCounter32(outRpcError));

        builder.setUsername(header.getUserName());
        builder.setTransport(getTransportForString(header.getTransport()));

        builder.setOutNotifications(new ZeroBasedCounter32(0L));

        builder.setKey(new SessionKey(getSessionId()));

        Session1Builder builder1 = new Session1Builder();
        builder1.setSessionIdentifier(header.getSessionIdentifier());
        builder.addAugmentation(Session1.class, builder1.build());

        return builder.build();
    }

    private Class<? extends Transport> getTransportForString(final String transport) {
        switch(transport) {
        case "ssh" :
            return NetconfSsh.class;
        case "tcp" :
            return NetconfTcp.class;
        default:
            throw new IllegalArgumentException("Unknown transport type " + transport);
        }
    }

    private String formatDateTime(final Date loginTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_DATE_FORMAT);
        return dateFormat.format(loginTime);
    }

    @Override
    protected NetconfServerSession thisInstance() {
        return this;
    }

    @Override
    protected void addExiHandlers(final ByteToMessageDecoder decoder, final MessageToByteEncoder<NetconfMessage> encoder) {
        replaceMessageDecoder(decoder);
        replaceMessageEncoderAfterNextMessage(encoder);
    }

    @Override
    public void stopExiCommunication() {
        replaceMessageDecoder(new NetconfXMLToMessageDecoder());
        replaceMessageEncoderAfterNextMessage(new NetconfMessageToXMLEncoder());
    }
}
