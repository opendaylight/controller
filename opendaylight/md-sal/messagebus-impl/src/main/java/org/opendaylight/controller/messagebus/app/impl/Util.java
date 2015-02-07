/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.messagebus.app.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.sal.common.util.Futures;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * TRASH/STAGING class. All functionalities implemented here are subject to move to more appropriate classes
 */
public class Util {
    public static String md5String(String inputString) {
        String md5String;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(inputString.getBytes());
            byte[] dig = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte aDig : dig) {
                sb.append(Integer.toString((aDig & 0xff) + 0x100, 16).substring(1));
            }
            md5String = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md5String;
    }

    public static <T> Future<RpcResult<T>> resultFor(T output) {
        RpcResult<T> result = Rpcs.getRpcResult(true, output, Collections.EMPTY_LIST);
        return Futures.immediateFuture(result);
    }


    public static Node getAffectedNode(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> event) {
        Node node = null;

        // TODO: expect listener method to be called even when change impact node
        // TODO: test with change.getCreatedData()
        for (Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry : event.getUpdatedData().entrySet()) {
            if ( isNode(changeEntry) ) {
                node = (Node) changeEntry.getValue();
                break;
            }
        }

        return node;
    }

    private static boolean isNode(Map.Entry<InstanceIdentifier<?>, DataObject> changeEntry )  {
        return Node.class.equals(changeEntry.getKey().getTargetType());
    }

    // ---------------------------
    // ----- QNAME expansion -----
    // ---------------------------
    public static List<QName> expandQname(List<QName> availableQnames, String wildcard) {
        List<QName> matchingQnames = new ArrayList<>();

        for (QName qname : availableQnames) {
            if (wildcardMatch(qname.getNamespace().toString(), wildcard)) {
                matchingQnames.add(qname);
            }
        }

        return matchingQnames;
    }

    private static boolean wildcardMatch(String stringToMatch, String wildcard) {
        return Pattern.matches(wildcardToRegex(wildcard), stringToMatch);
    }

    /**
     * CREDIT to http://www.rgagnon.com/javadetails/java-0515.html
     * @param wildcard
     * @return
     */
    private static String wildcardToRegex(String wildcard){
        StringBuffer s = new StringBuffer(wildcard.length());
        s.append('^');
        for (int i = 0, is = wildcard.length(); i < is; i++) {
            char c = wildcard.charAt(i);
            switch(c) {
                case '*':
                    s.append(".*");
                    break;
                case '?':
                    s.append(".");
                    break;
                // escape special regexp-characters
                case '(': case ')': case '[': case ']': case '$':
                case '^': case '.': case '{': case '}': case '|':
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
        return(s.toString());
    }
}
