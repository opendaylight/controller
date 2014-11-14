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
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public final class NetconfMessageUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfMessageUtil.class);

    private NetconfMessageUtil() {}

    public static boolean isOKMessage(NetconfMessage message) throws NetconfDocumentedException {
        return isOKMessage(message.getDocument());
    }

    public static boolean isOKMessage(Document document) throws NetconfDocumentedException {
        return isOKMessage(XmlElement.fromDomDocument(document));
    }

    public static boolean isOKMessage(XmlElement xmlElement) throws NetconfDocumentedException {
        if(xmlElement.getChildElements().size() != 1) {
            return false;
        }
        return xmlElement.getOnlyChildElement().getName().equals(XmlNetconfConstants.OK);
    }

    public static boolean isErrorMessage(NetconfMessage message) throws NetconfDocumentedException {
        return isErrorMessage(message.getDocument());
    }

    public static boolean isErrorMessage(Document document) throws NetconfDocumentedException {
        return isErrorMessage(XmlElement.fromDomDocument(document));
    }

    public static boolean isErrorMessage(XmlElement xmlElement) throws NetconfDocumentedException {
        if(xmlElement.getChildElements().size() != 1) {
            return false;
        }
        return xmlElement.getOnlyChildElement().getName().equals(XmlNetconfConstants.RPC_ERROR);
    }

    public static Collection<String> extractCapabilitiesFromHello(Document doc) throws NetconfDocumentedException {
        XmlElement responseElement = XmlElement.fromDomDocument(doc);
        XmlElement capabilitiesElement = responseElement
                .getOnlyChildElementWithSameNamespace(XmlNetconfConstants.CAPABILITIES);
        List<XmlElement> caps = capabilitiesElement.getChildElements(XmlNetconfConstants.CAPABILITY);
        return Collections2.transform(caps, new Function<XmlElement, String>() {

            @Override
            public String apply(@Nonnull XmlElement input) {
                // Trim possible leading/tailing whitespace
                try {
                    return input.getTextContent().trim();
                } catch (NetconfDocumentedException e) {
                    LOG.trace("Error fetching input text content",e);
                    return null;
                }
            }
        });

    }
}
