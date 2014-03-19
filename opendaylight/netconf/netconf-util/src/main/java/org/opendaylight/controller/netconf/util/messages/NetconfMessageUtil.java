/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.w3c.dom.Document;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public final class NetconfMessageUtil {

    private NetconfMessageUtil() {}

    public static boolean isOKMessage(NetconfMessage message) {
        return isOKMessage(message.getDocument());
    }

    public static boolean isOKMessage(Document document) {
        return isOKMessage(XmlElement.fromDomDocument(document));
    }

    public static boolean isOKMessage(XmlElement xmlElement) {
        if(xmlElement.getChildElements().size() != 1) {
            return false;
        }
        return xmlElement.getOnlyChildElement().getName().equals(XmlNetconfConstants.OK);
    }

    public static boolean isErrorMessage(NetconfMessage message) {
        return isErrorMessage(message.getDocument());
    }

    public static boolean isErrorMessage(Document document) {
        return isErrorMessage(XmlElement.fromDomDocument(document));
    }

    public static boolean isErrorMessage(XmlElement xmlElement) {
        if(xmlElement.getChildElements().size() != 1) {
            return false;
        }
        return xmlElement.getOnlyChildElement().getName().equals(XmlNetconfConstants.RPC_ERROR);
    }

    public static Collection<String> extractCapabilitiesFromHello(Document doc) {
        XmlElement responseElement = XmlElement.fromDomDocument(doc);
        XmlElement capabilitiesElement = responseElement
                .getOnlyChildElementWithSameNamespace(XmlNetconfConstants.CAPABILITIES);
        List<XmlElement> caps = capabilitiesElement.getChildElements(XmlNetconfConstants.CAPABILITY);
        return Collections2.transform(caps, new Function<XmlElement, String>() {

            @Nullable
            @Override
            public String apply(@Nullable XmlElement input) {
                // Trim possible leading/tailing whitespace
                return input.getTextContent().trim();
            }
        });

    }
}
