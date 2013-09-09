/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility class providing common system utilities
 */

public class SysUtils {
    public static boolean amIroot() {
        try {
            Process p = Runtime.getRuntime().exec("id -u");
            InputStream in = p.getInputStream();
            InputStreamReader isr = new InputStreamReader(in);
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(isr);

            String read = br.readLine();
            while (read != null) {
                sb.append(read);
                read = br.readLine();
            }

            // "id -u" command should have returned 0 for root
            return (sb.toString().equals("0"));
        } catch (Exception e) {
            return false;
        }
    }
}
