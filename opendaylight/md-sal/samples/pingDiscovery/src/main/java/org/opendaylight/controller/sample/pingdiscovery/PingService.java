/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * Tests the PingableDeviceHandler class
 * 
 * @author Devin Avery
 * @author Greg Hall
 */

package org.opendaylight.controller.sample.pingdiscovery;

import java.util.regex.Pattern;

/**
 * @author jameshall
 * 
 */
public interface PingService {
    double NOT_FOUND = -1;
    Pattern VALID_PING_RESPONSE = Pattern
            .compile("[0-9]+ bytes from .*\\: icmp_seq=[0-9]+ ttl=[0-9]+ time=([0-9]+\\.[0-9]+) ms");

    double ping(String host);

    double ping(String host, int count, int timeoutSeconds);
}
