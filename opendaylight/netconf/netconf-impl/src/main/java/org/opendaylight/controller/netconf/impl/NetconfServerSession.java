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
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.DomainName;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Host;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfSsh;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.Transport;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.Session;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.SessionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.netconf.state.sessions.SessionKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.ZeroBasedCounter32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetconfServerSession extends NetconfSession implements NetconfManagementSession {

    private static final Logger logger = LoggerFactory.getLogger(NetconfServerSession.class);

    private final NetconfServerSessionNegotiator.AdditionalHeader header;

    private Date loginTime;
    private long inRpcSuccess, inRpcFail, outRpcError;

    public NetconfServerSession(SessionListener sessionListener, Channel channel, long sessionId,
            NetconfServerSessionNegotiator.AdditionalHeader header) {
        super(sessionListener, channel, sessionId);
        this.header = header;
        logger.debug("Session {} created", toString());
    }

    @Override
    protected void sessionUp() {
        super.sessionUp();
        Preconditions.checkState(loginTime == null, "Session is already up");
        this.loginTime = new Date();
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

    @Override
    public Session toManagementSession() {
        SessionBuilder builder = new SessionBuilder();

        builder.setSessionId(getSessionId());
        builder.setSourceHost(new Host(new DomainName(header.getAddress())));

        Preconditions.checkState(DateAndTime.PATTERN_CONSTANTS.size() == 1);
        String formattedDateTime = formatDateTime(loginTime);
        String pattern = DateAndTime.PATTERN_CONSTANTS.get(0);
        Matcher matcher = Pattern.compile(pattern).matcher(formattedDateTime);
        Preconditions.checkState(matcher.matches(), "Formatted datetime %s does not match pattern %s", formattedDateTime, pattern);
        builder.setLoginTime(new DateAndTime(formattedDateTime));

        builder.setInBadRpcs(new ZeroBasedCounter32(inRpcFail));
        builder.setInRpcs(new ZeroBasedCounter32(inRpcSuccess));
        builder.setOutRpcErrors(new ZeroBasedCounter32(outRpcError));

        builder.setUsername(header.getUsername());
        builder.setTransport(getTransportForString(header.getTransport()));

        builder.setOutNotifications(new ZeroBasedCounter32(0L));

        builder.setKey(new SessionKey(getSessionId()));
        return builder.build();
    }

    private Class<? extends Transport> getTransportForString(String transport) {
        switch(transport) {
            case "ssh" : return NetconfSsh.class;
            // TODO what about tcp
            case "tcp" : return NetconfSsh.class;
            default: throw new IllegalArgumentException("Unknown transport type " + transport);
        }
    }

    private String formatDateTime(Date loginTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_DATE_FORMAT);
        return dateFormat.format(loginTime);
    }

}
