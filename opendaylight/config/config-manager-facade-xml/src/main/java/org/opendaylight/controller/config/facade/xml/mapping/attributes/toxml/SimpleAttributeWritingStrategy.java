/*
 * Copyright (c) 2015, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Optional;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleAttributeWritingStrategy implements AttributeWritingStrategy {

    private final Document document;
    private final String key;

    public SimpleAttributeWritingStrategy(final Document document, final String key) {
        this.document = document;
        this.key = key;
    }

    @Override
    public void writeElement(final Element parentElement, final String namespace, Object value) {
        value = preprocess(value);
        Util.checkType(value, String.class);
        Element innerNode = createElement(document, key, (String) value, Optional.of(namespace));
        parentElement.appendChild(innerNode);
    }

    protected Element createElement(final Document document, final String key, final String value,
            final Optional<String> namespace) {
        Element typeElement = XmlUtil.createElement(document, key, namespace);

        typeElement.appendChild(document.createTextNode(value));
        return typeElement;
    }

    protected Object preprocess(final Object value) {
        return value;
    }
}
