/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.mapping.attributes.fromxml;

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.editconfig.EditConfig;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.common.QName;


public class SimpleIdentityRefAttributeReadingStrategy extends SimpleAttributeReadingStrategy {

    private final String key;
    private final Map<String, Map<Date, EditConfig.IdentityMapping>> identityMap;

    public SimpleIdentityRefAttributeReadingStrategy(String nullableDefault, String key, Map<String, Map<Date,EditConfig.IdentityMapping>> identityMap) {
        super(nullableDefault);
        this.key = key;
        this.identityMap = identityMap;
    }

    @Override
    protected String readElementContent(XmlElement xmlElement) {
        // TODO test
        Map.Entry<String, String> namespaceOfTextContent = xmlElement.findNamespaceOfTextContent();
        String content = xmlElement.getTextContent();

        String prefix = namespaceOfTextContent.getKey() + ":";
        Preconditions.checkArgument(content.startsWith(prefix), "Identity ref should be prefixed");

        String localName = content.substring(prefix.length());
        String namespace = namespaceOfTextContent.getValue();

        Date revision = null;
        Map<Date, EditConfig.IdentityMapping> revisions = identityMap.get(namespace);
        if(revisions.keySet().size() > 1) {
            for (Date date : revisions.keySet()) {
                if(revisions.get(date).containsIdName(localName)) {
                    Preconditions.checkState(revision == null, "Duplicate identity %s, in namespace %s, with revisions: %s, %s detected. Cannot map attribute",
                            localName, namespace, revision, date);
                    revision = date;
                }
            }
        } else
            revision = revisions.keySet().iterator().next();


        return QName.create(URI.create(namespace), revision, localName).toString();
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
