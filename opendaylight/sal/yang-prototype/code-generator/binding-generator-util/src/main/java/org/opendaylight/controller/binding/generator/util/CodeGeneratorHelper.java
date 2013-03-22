/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.binding.generator.util;

public class CodeGeneratorHelper {

    public static String parseToClassName(String token) {
        String correctStr = parseToCamelCase(token);

        // make first char upper-case
        char first = Character.toUpperCase(correctStr.charAt(0));
        correctStr = first + correctStr.substring(1);
        return correctStr;
    }

    public static String parseToParamName(String token) {
        String correctStr = parseToCamelCase(token);

        // make first char lower-case
        char first = Character.toLowerCase(correctStr.charAt(0));
        correctStr = first + correctStr.substring(1);
        return correctStr;
    }

    private static String parseToCamelCase(String token) {
        if (token == null) {
            throw new NullPointerException("Name can not be null");
        }

        String correctStr = token.trim();
        if (correctStr.length() == 0) {
            throw new IllegalArgumentException("Name can not be emty");
        }

        correctStr = replaceWithCamelCase(correctStr, ' ');
        correctStr = replaceWithCamelCase(correctStr, '-');
        correctStr = replaceWithCamelCase(correctStr, '_');
        return correctStr;
    }

    private static String replaceWithCamelCase(String text, char removalChar) {
        StringBuilder sb = new StringBuilder(text);
        String toBeRemoved = String.valueOf(removalChar);

        int toBeRemovedPos = sb.indexOf(toBeRemoved);
        while (toBeRemovedPos != -1) {
            sb.replace(toBeRemovedPos, toBeRemovedPos + 1, "");
            // check if 'toBeRemoved' character is not the only character in
            // 'text'
            if (sb.length() == 0) {
                throw new IllegalArgumentException("Name can not be '"
                        + toBeRemoved + "'");
            }
            String replacement = String.valueOf(sb.charAt(toBeRemovedPos))
                    .toUpperCase();
            sb.setCharAt(toBeRemovedPos, replacement.charAt(0));
            toBeRemovedPos = sb.indexOf(toBeRemoved);
        }
        return sb.toString();
    }

}
