/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.w3c.dom.Document;

public final class NetconfMessageUtil {

    private NetconfMessageUtil() {}

    public static boolean isOKMessage(NetconfMessage message) throws NetconfDocumentedException {
        return isOKMessage(message.getDocument());
    }

    public static boolean isOKMessage(Document document) throws NetconfDocumentedException {
        return isOKMessage(XmlElement.fromDomDocument(document));
    }

    public static boolean isOKMessage(XmlElement xmlElement) throws NetconfDocumentedException {
        return xmlElement.getOnlyChildElement().getName().equals(XmlNetconfConstants.OK);
    }

    public static boolean isErrorMEssage(NetconfMessage message) throws NetconfDocumentedException {
        return isErrorMessage(message.getDocument());
    }

    public static boolean isErrorMessage(Document document) throws NetconfDocumentedException {
        return isErrorMessage(XmlElement.fromDomDocument(document));
    }

    public static boolean isErrorMessage(XmlElement xmlElement) throws NetconfDocumentedException {
        return xmlElement.getOnlyChildElement().getName().equals(XmlNetconfConstants.RPC_ERROR);

    }
}
