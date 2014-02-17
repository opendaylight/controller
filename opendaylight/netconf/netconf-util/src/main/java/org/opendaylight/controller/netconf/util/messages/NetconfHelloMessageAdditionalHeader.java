/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Additional header can be used with hello message to carry information about
 * session's connection. Provided information can be reported via netconf
 * monitoring.
 * <pre>
 * It has PATTERN "[username; host-address:port; transport; session-identifier;]"
 * username - name of account on a remote
 * host-address - client's IP address
 * port - port number
 * transport - tcp, ssh
 * session-identifier - persister, client
 * Session-identifier is optional, others mandatory.
 * </pre>
 * This header is inserted in front of a netconf hello message followed by a newline.
 */
public class NetconfHelloMessageAdditionalHeader {

    private static final String SC = ";";

    private final String userName;
    private final String hostAddress;
    private final String port;
    private final String transport;
    private final String sessionIdentifier;

    public NetconfHelloMessageAdditionalHeader(String userName, String hostAddress, String port, String transport, String sessionIdentifier) {
        this.userName = userName;
        this.hostAddress = hostAddress;
        this.port = port;
        this.transport = transport;
        this.sessionIdentifier = sessionIdentifier;
    }

    public String getUserName() {
        return userName;
    }

    public String getAddress() {
        return hostAddress;
    }

    public String getPort() {
        return port;
    }

    public String getTransport() {
        return transport;
    }

    public String getSessionIdentifier() {
        return sessionIdentifier;
    }

    /**
     * Format additional header into a string suitable as a prefix for netconf hello message
     */
    public String toFormattedString() {
        Preconditions.checkNotNull(userName);
        Preconditions.checkNotNull(hostAddress);
        Preconditions.checkNotNull(port);
        Preconditions.checkNotNull(transport);
        Preconditions.checkNotNull(sessionIdentifier);
        return "[" + userName + SC + hostAddress + ":" + port + SC + transport + SC + sessionIdentifier + SC + "]"
                + System.lineSeparator();
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("NetconfHelloMessageAdditionalHeader{");
        sb.append("userName='").append(userName).append('\'');
        sb.append(", hostAddress='").append(hostAddress).append('\'');
        sb.append(", port='").append(port).append('\'');
        sb.append(", transport='").append(transport).append('\'');
        sb.append(", sessionIdentifier='").append(sessionIdentifier).append('\'');
        sb.append('}');
        return sb.toString();
    }

    // TODO IPv6
    private static final Pattern PATTERN = Pattern
            .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+)[:/](?<port>[0-9]+);(?<transport>[a-z]+)[^\\]]+\\]");
    private static final Pattern CUSTOM_HEADER_PATTERN = Pattern
            .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+)[:/](?<port>[0-9]+);(?<transport>[a-z]+);(?<sessionIdentifier>[a-z]+)[^\\]]+\\]");

    /**
     * Parse additional header from a formatted string
     */
    public static NetconfHelloMessageAdditionalHeader fromString(String additionalHeader) {
        String additionalHeaderTrimmed = additionalHeader.trim();
        Matcher matcher = PATTERN.matcher(additionalHeaderTrimmed);
        Matcher matcher2 = CUSTOM_HEADER_PATTERN.matcher(additionalHeaderTrimmed);
        Preconditions.checkArgument(matcher.matches(), "Additional header in wrong format %s, expected %s",
                additionalHeaderTrimmed, PATTERN);

        String username = matcher.group("username");
        String address = matcher.group("address");
        String port = matcher.group("port");
        String transport = matcher.group("transport");
        String sessionIdentifier = "client";
        if (matcher2.matches()) {
            sessionIdentifier = matcher2.group("sessionIdentifier");
        }
        return new NetconfHelloMessageAdditionalHeader(username, address, port, transport, sessionIdentifier);
    }

}
