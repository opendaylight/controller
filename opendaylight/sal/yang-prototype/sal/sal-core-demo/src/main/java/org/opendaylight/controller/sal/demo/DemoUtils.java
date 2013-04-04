/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.demo;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.util.Nodes;


public class DemoUtils {

    public static final URI namespace = uri("urn:cisco:prototype:sal:demo");
    public static final Date revision = new Date();

    public static final QName alertNotification = qName("alert");
    public static final QName changeNotification = qName("change");

    public static final QName contentNodeName = qName("content");

    public static URI uri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static QName qName(String str) {
        return new QName(namespace, revision, str);
    }

    public static Node<?> contentNode(String content) {
        return Nodes.leafNode(contentNodeName, content);
    }
}
