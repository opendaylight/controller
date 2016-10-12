/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.codec.TypeDefinitionAwareCodec;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeIdentifierWithPredicatesGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(NodeIdentifierWithPredicatesGenerator.class);
    private static final Pattern PATTERN = Pattern.compile("(.*)\\Q[{\\E(.*)\\Q}]\\E");

    private final String id;
    private final Matcher matcher;
    private final boolean doesMatch;
    private final ListSchemaNode listSchemaNode;

    public NodeIdentifierWithPredicatesGenerator(String id, DataSchemaNode schemaNode) {
        this.id = id;
        matcher = PATTERN.matcher(this.id);
        doesMatch = matcher.matches();

        if (schemaNode instanceof  ListSchemaNode) {
            this.listSchemaNode = (ListSchemaNode) schemaNode;
        } else {
            this.listSchemaNode = null;
        }
    }


    public boolean matches() {
        return doesMatch;
    }

    public YangInstanceIdentifier.NodeIdentifierWithPredicates getPathArgument() {
        final String group = matcher.group(2);
        final String[] keyValues = group.split(",");
        Map<QName, Object> nameValues = new HashMap<>();

        for (String keyValue : keyValues) {
            int eqIndex = keyValue.lastIndexOf('=');
            try {
                final QName key = QNameFactory
                    .create(keyValue.substring(0, eqIndex));
                nameValues.put(key, getValue(key, keyValue.substring(eqIndex + 1)));
            } catch (IllegalArgumentException e) {
                LOG.error("Error processing identifier {}", id, e);
                throw e;
            }
        }

        return new YangInstanceIdentifier.NodeIdentifierWithPredicates(
                QNameFactory.create(matcher.group(1)), nameValues);
    }


    private Object getValue(QName key, String value) {
        if (listSchemaNode != null) {
            for (DataSchemaNode node : listSchemaNode.getChildNodes()) {
                if (node instanceof LeafSchemaNode && node.getQName().equals(key)) {
                    return TypeDefinitionAwareCodec.from(LeafSchemaNode.class.cast(node).getType()).deserialize(value);
                }
            }
        }
        return value;
    }
}
