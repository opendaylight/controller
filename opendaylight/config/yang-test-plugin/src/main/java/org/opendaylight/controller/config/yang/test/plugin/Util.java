/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.test.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;

public class Util {

    public static String replaceDots(String path) {
        path = path.replace(".", Matcher.quoteReplacement(File.separator));
        return path;
    }

    public static String loadHeader() throws IOException {
        StringBuffer header = new StringBuffer();
        InputStream headIn = Util.class.getClassLoader().getResourceAsStream("Header.txt");
        BufferedReader headBuf = new BufferedReader(new InputStreamReader(headIn));
        String line = null;
        while ((line = headBuf.readLine()) != null) {
            header.append(line).append(System.lineSeparator());
        }
        headBuf.close();
        return header.toString();
    }

    public static String loadStubFile(String fileName) throws IOException {
        InputStream stubIn = new FileInputStream(fileName);
        BufferedReader stubBuf = new BufferedReader(new InputStreamReader(stubIn));

        StringBuffer stubLines = new StringBuffer();
        String stubLine = null;
        while ((stubLine = stubBuf.readLine()) != null) {
            stubLines.append(stubLine).append(System.lineSeparator());
        }
        stubBuf.close();
        return stubLines.toString();
    }
}
