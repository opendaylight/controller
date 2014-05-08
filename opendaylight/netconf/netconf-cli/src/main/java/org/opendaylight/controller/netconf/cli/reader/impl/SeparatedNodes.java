/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.reader.impl;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;

class SeparatedNodes {
    private final Set<DataSchemaNode> keyNodes;
    private final Set<DataSchemaNode> mandatoryNotKey;
    private final Set<DataSchemaNode> otherNodes;

    public SeparatedNodes(final Set<DataSchemaNode> keyNodes, final Set<DataSchemaNode> mandatoryNotKey,
            final Set<DataSchemaNode> otherNodes) {
        this.keyNodes = keyNodes;
        this.mandatoryNotKey = mandatoryNotKey;
        this.otherNodes = otherNodes;
    }

    public Set<DataSchemaNode> getKeyNodes() {
        return keyNodes;
    }

    public Set<DataSchemaNode> getMandatoryNotKey() {
        return mandatoryNotKey;
    }

    public Set<DataSchemaNode> getOthers() {
        return otherNodes;
    }

    static SeparatedNodes separateNodes(final DataNodeContainer dataNodeContainer) {
        return separateNodes(dataNodeContainer, false);
    }

    static SeparatedNodes separateNodes(final DataNodeContainer dataNodeContainer, final boolean removeConfigFalseNodes) {
        final Set<DataSchemaNode> keys = new HashSet<>();
        final Set<DataSchemaNode> mandatoryNotKeys = new HashSet<>();
        final Set<DataSchemaNode> others = new HashSet<>();

        List<QName> keyQNames = Collections.emptyList();
        if (dataNodeContainer instanceof ListSchemaNode) {
            keyQNames = ((ListSchemaNode) dataNodeContainer).getKeyDefinition();
        }

        for (final DataSchemaNode dataSchemaNode : dataNodeContainer.getChildNodes()) {
            if (removeConfigFalseNodes) {
                if (!dataSchemaNode.isConfiguration()) {
                    continue;
                }
            }
            if (keyQNames.contains(dataSchemaNode.getQName())) {
                Preconditions.checkArgument(dataSchemaNode instanceof LeafSchemaNode);
                keys.add(dataSchemaNode);
            } else if (dataSchemaNode.getConstraints().isMandatory()) {
                mandatoryNotKeys.add(dataSchemaNode);
            } else {
                others.add(dataSchemaNode);
            }
        }

        return new SeparatedNodes(keys, mandatoryNotKeys, others);
    }
}