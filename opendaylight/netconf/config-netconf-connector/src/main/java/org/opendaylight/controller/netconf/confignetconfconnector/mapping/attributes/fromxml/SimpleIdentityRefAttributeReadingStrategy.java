/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

public class SimpleIdentityRefAttributeReadingStrategy extends SimpleAttributeReadingStrategy {

    private final String key;

    public SimpleIdentityRefAttributeReadingStrategy(String nullableDefault, String key) {
        super(nullableDefault);
        this.key = key;
    }

    @Override
    protected String readElementContent(XmlElement xmlElement) {
        // TODO test
        Map.Entry<String, String> namespaceOfTextContent = xmlElement.findNamespaceOfTextContent();
        String content = xmlElement.getTextContent();
        return content.replace(namespaceOfTextContent.getKey(), namespaceOfTextContent.getValue());
    }

    @Override
    protected Object postprocessParsedValue(String textContent) {
        HashMap<String,String> map = Maps.newHashMap();
        map.put(key, textContent);
        return map;
    }

    @Override
    protected Object postprocessNullableDefault(String nullableDefault) {
        return nullableDefault == null ? null : postprocessParsedValue(nullableDefault);
    }
}
