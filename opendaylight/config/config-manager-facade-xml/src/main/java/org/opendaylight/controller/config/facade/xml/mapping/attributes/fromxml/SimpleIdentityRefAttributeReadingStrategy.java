/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.facade.xml.mapping.attributes.fromxml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.config.facade.xml.mapping.IdentityMapping;
import org.opendaylight.controller.config.util.xml.DocumentedException;
import org.opendaylight.controller.config.util.xml.XmlElement;
import org.opendaylight.yangtools.yang.common.QName;


public class SimpleIdentityRefAttributeReadingStrategy extends SimpleAttributeReadingStrategy {

    private final String key;
    private final Map<String, Map<Date, IdentityMapping>> identityMap;

    public SimpleIdentityRefAttributeReadingStrategy(String nullableDefault, String key, Map<String, Map<Date, IdentityMapping>> identityMap) {
        super(nullableDefault);
        this.key = key;
        this.identityMap = identityMap;
    }

    @Override
    protected String readElementContent(XmlElement xmlElement) throws DocumentedException {
        Map.Entry<String, String> namespaceOfTextContent = xmlElement.findNamespaceOfTextContent();
        String content = xmlElement.getTextContent();

        final String namespace;
        final String localName;
        if(namespaceOfTextContent.getKey().isEmpty()) {
            localName = content;
            namespace = xmlElement.getNamespace();
        } else {
            String prefix = namespaceOfTextContent.getKey() + ":";
            Preconditions.checkArgument(content.startsWith(prefix), "Identity ref should be prefixed with \"%s\"", prefix);
            localName = content.substring(prefix.length());
            namespace = namespaceOfTextContent.getValue();
        }

        Date revision = null;
        Map<Date, IdentityMapping> revisions = identityMap.get(namespace);
        if(revisions.keySet().size() > 1) {
            for (Map.Entry<Date, IdentityMapping> revisionToIdentityEntry : revisions.entrySet()) {
                if(revisionToIdentityEntry.getValue().containsIdName(localName)) {
                    Preconditions.checkState(revision == null, "Duplicate identity %s, in namespace %s, with revisions: %s, %s detected. Cannot map attribute",
                            localName, namespace, revision, revisionToIdentityEntry.getKey());
                    revision = revisionToIdentityEntry.getKey();
                }
            }
        } else {
            revision = revisions.keySet().iterator().next();
        }
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
