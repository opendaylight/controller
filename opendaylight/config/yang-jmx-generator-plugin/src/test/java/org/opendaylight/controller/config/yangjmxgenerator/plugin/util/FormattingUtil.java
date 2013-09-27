/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin.util;

import java.util.Scanner;

public class FormattingUtil {

    public static String cleanUpEmptyLinesAndIndent(String input) {
        StringBuffer output = new StringBuffer();
        Scanner scanner = new Scanner(input);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = line.replaceAll("\t", " ");
            while (line.contains("  ")) {
                line = line.replaceAll("  ", " ");
            }
            line = line.trim();
            if (line.length() > 0) {
                output.append(line);
                output.append("\n");
            }
        }

        return output.toString();
    }
}
