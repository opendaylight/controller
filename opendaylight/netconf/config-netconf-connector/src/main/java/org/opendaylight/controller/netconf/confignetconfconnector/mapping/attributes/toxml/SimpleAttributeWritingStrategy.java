/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import com.google.common.base.Optional;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleAttributeWritingStrategy implements AttributeWritingStrategy {

    private final Document document;
    private final String key;

    /**
     * @param document
     * @param key
     */
    public SimpleAttributeWritingStrategy(Document document, String key) {
        this.document = document;
        this.key = key;
    }

    @Override
    public void writeElement(Element parentElement, String namespace, Object value) {
        value = preprocess(value);
        NetconfUtil.checkType(value, String.class);
        Element innerNode = createElement(document, key, (String) value, Optional.of(namespace));
        parentElement.appendChild(innerNode);
    }

    protected Element createElement(Document document, String key, String value, Optional<String> namespace) {
        Element typeElement = XmlUtil.createElement(document, key, namespace);

        typeElement.appendChild(document.createTextNode(value));
        return typeElement;
    }
    protected Object preprocess(Object value) {
        return value;
    }


}
