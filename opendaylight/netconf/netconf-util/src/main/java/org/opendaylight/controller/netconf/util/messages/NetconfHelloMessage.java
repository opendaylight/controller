/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util.messages;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedElementException;
import org.opendaylight.controller.netconf.util.exception.UnexpectedNamespaceException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.w3c.dom.Document;

/**
 * NetconfMessage that can carry additional header with session metadata. See {@link org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader}
 */
public final class NetconfHelloMessage extends NetconfMessage {

    public static final String HELLO_TAG = "hello";

    private final NetconfHelloMessageAdditionalHeader additionalHeader;

    public NetconfHelloMessage(Document doc, NetconfHelloMessageAdditionalHeader additionalHeader) throws NetconfDocumentedException {
        super(doc);
        checkHelloMessage(doc);
        this.additionalHeader = additionalHeader;
    }

    public NetconfHelloMessage(Document doc) throws NetconfDocumentedException {
        this(doc, null);
    }

    public Optional<NetconfHelloMessageAdditionalHeader> getAdditionalHeader() {
        return additionalHeader== null ? Optional.<NetconfHelloMessageAdditionalHeader>absent() : Optional.of(additionalHeader);
    }

    private static void checkHelloMessage(Document doc) throws NetconfDocumentedException {
        try {
            XmlElement.fromDomElementWithExpected(doc.getDocumentElement(), HELLO_TAG,
                    XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0);

        } catch (final MissingNameSpaceException | UnexpectedNamespaceException | UnexpectedElementException e) {
            throw NetconfDocumentedException.wrap(e);
        }
    }
}
