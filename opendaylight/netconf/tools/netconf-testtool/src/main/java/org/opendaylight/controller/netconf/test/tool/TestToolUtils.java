/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

public class TestToolUtils {

    public static String getMac(long mac) {
        StringBuilder m = new StringBuilder(Long.toString(mac, 16));

        for (int i = m.length(); i < 12; i++) {
            m.insert(0, "0");
        }

        for (int j = m.length() - 2; j >= 2; j -= 2) {
            m.insert(j, ":");
        }

        return m.toString();
    }
}
