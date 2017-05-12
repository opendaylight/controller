/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.toxml;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.config.facade.xml.util.Util;
import org.opendaylight.controller.config.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CompositeAttributeWritingStrategy implements AttributeWritingStrategy {

    private final String key;
    private final Document document;
    private final Map<String, AttributeWritingStrategy> innerStrats;

    public CompositeAttributeWritingStrategy(final Document document, final String key,
            final Map<String, AttributeWritingStrategy> innerStrats) {
        this.document = document;
        this.key = key;
        this.innerStrats = innerStrats;
    }

    @Override
    public void writeElement(final Element parentElement, final String namespace, final Object value) {
        Util.checkType(value, Map.class);

        Element innerNode = XmlUtil.createElement(document, key, Optional.of(namespace));

        Map<?, ?> map = (Map<?, ?>) value;

        for (Entry<?, ?> innerObjectEntry : map.entrySet()) {
            Util.checkType(innerObjectEntry.getKey(), String.class);

            String innerKey = (String) innerObjectEntry.getKey();
            Object innerValue = innerObjectEntry.getValue();

            innerStrats.get(innerKey).writeElement(innerNode, namespace, innerValue);
        }
        parentElement.appendChild(innerNode);
    }

    public String getKey() {
        return key;
    }

    public Document getDocument() {
        return document;
    }

    public Map<String, AttributeWritingStrategy> getInnerStrats() {
        return innerStrats;
    }
}
