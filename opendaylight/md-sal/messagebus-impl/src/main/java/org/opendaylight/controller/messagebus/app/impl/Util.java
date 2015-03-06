/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class Util {
    private static final MessageDigest messageDigestTemplate = getDigestInstance();

    private static MessageDigest getDigestInstance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to get MD5 instance");
        }
    }

    static String md5String(final String inputString) {

        try {
            final MessageDigest md = (MessageDigest)messageDigestTemplate.clone();
            md.update(inputString.getBytes("UTF-8"), 0, inputString.length());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (final Exception e) {
            throw new RuntimeException("Unable to get MD5 instance");
        }
    }

    static <T> Future<RpcResult<T>> resultFor(final T output) {
        final RpcResult<T> result = Rpcs.getRpcResult(true, output, Collections.<RpcError>emptyList());
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
    static String wildcardToRegex(final String wildcard){
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
