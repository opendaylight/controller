/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import com.google.common.util.concurrent.Futures;

public final class Util {
    
    public static String getUUIDIdent(){
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static <T> Future<RpcResult<T>> resultRpcSuccessFor(final T output) {
        final RpcResult<T> result = RpcResultBuilder.success(output).build();
        return Futures.immediateFuture(result);
    }

    /**
     * Method filters qnames based on wildcard strings
     *
     * @param list
     * @param patterh matching pattern
     * @return list of filtered qnames
     */
    public static List<SchemaPath> expandQname(final List<SchemaPath> list, final Pattern pattern) {
        final List<SchemaPath> matchingQnames = new ArrayList<>();

        for (final SchemaPath notification : list) {
            final String namespace = notification.getLastComponent().getNamespace().toString();
            if (pattern.matcher(namespace).matches()) {
                matchingQnames.add(notification);
            }
        }
        return matchingQnames;
    }

    /**
     * CREDIT to http://www.rgagnon.com/javadetails/java-0515.html
     * @param wildcard
     * @return
     */
    public static String wildcardToRegex(final String wildcard){
        final StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (final char c : wildcard.toCharArray()) {
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append('.');
                    break;
                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    s.append("\\");
                    s.append(c);
                    break;
                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return s.toString();
    }
}
