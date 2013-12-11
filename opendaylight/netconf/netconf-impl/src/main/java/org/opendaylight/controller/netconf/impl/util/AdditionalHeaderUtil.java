/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiator.AdditionalHeader;

import com.google.common.base.Preconditions;

public class AdditionalHeaderUtil {

    private static final Pattern pattern = Pattern
            .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+)[:/](?<port>[0-9]+);(?<transport>[a-z]+)[^\\]]+\\]");
    private static final Pattern customHeaderPattern = Pattern
            .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+)[:/](?<port>[0-9]+);(?<transport>[a-z]+);(?<sessionIdentifier>[a-z]+)[^\\]]+\\]");

    public static AdditionalHeader fromString(String additionalHeader) {
        additionalHeader = additionalHeader.trim();
        Matcher matcher = pattern.matcher(additionalHeader);
        Matcher matcher2 = customHeaderPattern.matcher(additionalHeader);
        Preconditions.checkArgument(matcher.matches(), "Additional header in wrong format %s, expected %s",
                additionalHeader, pattern);
        String username = matcher.group("username");
        String address = matcher.group("address");
        String transport = matcher.group("transport");
        String sessionIdentifier = "client";
        if (matcher2.matches()) {
            sessionIdentifier = matcher2.group("sessionIdentifier");
        }
        return new AdditionalHeader(username, address, transport, sessionIdentifier);
    }

}
