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
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;

public final class Util {
    private static final MessageDigest messageDigestTemplate = getDigestInstance();

    private static MessageDigest getDigestInstance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to get MD5 instance");
        }
    }

    public static String md5String(final String inputString) {

        try {
            MessageDigest md = (MessageDigest)messageDigestTemplate.clone();
            md.update(inputString.getBytes("UTF-8"), 0, inputString.length());
            return new BigInteger(1, md.digest()).toString(16);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get MD5 instance");
        }
    }

    public static <T> Future<RpcResult<T>> resultFor(final T output) {
        RpcResult<T> result = Rpcs.getRpcResult(true, output, Collections.<RpcError>emptyList());
        return Futures.immediateFuture(result);
    }

    /**
     * Extracts affected node from data change event.
     * @param event
     * @return
     */
    public static Node getAffectedNode(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        // TODO: expect listener method to be called even when change impact node
        // TODO: test with change.getCreatedData()
        for (Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getUpdatedData().entrySet()) {
            if (isNode(changeEntry)) {
                return (Node) changeEntry.getValue();
            }
        }

        return null;
    }

    private static boolean isNode(final Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry )  {
        return Node.class.equals(changeEntry.getKey().getTargetType());
    }

    /**
     * Method filters qnames based on wildcard strings
     *
     * @param availableQnames
     * @param patterh matching pattern
     * @return list of filtered qnames
     */
    public static List<QName> expandQname(final List<QName> availableQnames, final Pattern pattern) {
        List<QName> matchingQnames = new ArrayList<>();

        for (QName qname : availableQnames) {
            String namespace = qname.getNamespace().toString();
            if (pattern.matcher(namespace).matches()) {
                matchingQnames.add(qname);
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
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (char c : wildcard.toCharArray()) {
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
