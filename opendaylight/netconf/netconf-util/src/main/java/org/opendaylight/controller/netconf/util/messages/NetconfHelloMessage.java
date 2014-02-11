/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;

import com.google.common.base.Optional;

/**
 * NetconfMessage that can carry additional header with session metadata. See {@link org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader}
 */
public final class NetconfHelloMessage extends NetconfMessage {

    public static final String HELLO_TAG = "hello";

    private final NetconfHelloMessageAdditionalHeader additionalHeader;

    public NetconfHelloMessage(Document doc, NetconfHelloMessageAdditionalHeader additionalHeader) {
        super(doc);
        checkHelloMessage(doc);
        this.additionalHeader = additionalHeader;
    }

    public NetconfHelloMessage(Document doc) {
        this(doc, null);
    }

    public Optional<NetconfHelloMessageAdditionalHeader> getAdditionalHeader() {
        return additionalHeader== null ? Optional.<NetconfHelloMessageAdditionalHeader>absent() : Optional.of(additionalHeader);
    }

    private static void checkHelloMessage(Document doc) {
        try {
            XmlElement.fromDomElementWithExpected(doc.getDocumentElement(), HELLO_TAG,
                    XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new IllegalArgumentException(String.format(
                    "Hello message invalid format, should contain %s tag from namespace %s, but is: %s", HELLO_TAG,
                    XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0, XmlUtil.toString(doc)), e);
        }
    }
}
