/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.toxml;

import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.netconf.confignetconfconnector.util.Util;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CompositeAttributeWritingStrategy implements AttributeWritingStrategy {

    protected final String key;
    protected final Document document;
    protected final Map<String, AttributeWritingStrategy> innerStrats;

    public CompositeAttributeWritingStrategy(Document document, String key,
            Map<String, AttributeWritingStrategy> innerStrats) {
        this.document = document;
        this.key = key;
        this.innerStrats = innerStrats;
    }

    @Override
    public void writeElement(Element parentElement, String namespace, Object value) {
        Util.checkType(value, Map.class);

        Element innerNode = document.createElement(key);
        XmlUtil.addNamespaceAttr(innerNode, namespace);

        Map<?, ?> map = (Map<?, ?>) value;

        for (Entry<?, ?> innerObjectEntry : map.entrySet()) {
            Util.checkType(innerObjectEntry.getKey(), String.class);

            String innerKey = (String) innerObjectEntry.getKey();
            Object innerValue = innerObjectEntry.getValue();

            innerStrats.get(innerKey).writeElement(innerNode, namespace, innerValue);
        }
        parentElement.appendChild(innerNode);
    }
}
