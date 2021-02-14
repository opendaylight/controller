/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils;

import static java.util.Objects.requireNonNull;

import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MixinNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodeContainer;

/**
 * NormalizedNodeNavigator walks a {@link NormalizedNodeVisitor} through the NormalizedNode.
 */
public class NormalizedNodeNavigator {
    private final NormalizedNodeVisitor visitor;

    public NormalizedNodeNavigator(final NormalizedNodeVisitor visitor) {
        this.visitor = requireNonNull(visitor, "visitor should not be null");
    }

    public void navigate(String parentPath, final NormalizedNode normalizedNode) {
        if (parentPath == null) {
            parentPath = "";
        }
        navigateNormalizedNode(0, parentPath, normalizedNode);
    }

    private void navigateDataContainerNode(final int level, final String parentPath,
            final DataContainerNode dataContainerNode) {
        visitor.visitNode(level, parentPath, dataContainerNode);

        String newParentPath = parentPath + "/" + dataContainerNode.getIdentifier().toString();

        for (NormalizedNode node : dataContainerNode.body()) {
            if (node instanceof MixinNode && node instanceof NormalizedNodeContainer) {
                navigateNormalizedNodeContainerMixin(level, newParentPath, (NormalizedNodeContainer<?>) node);
            } else {
                navigateNormalizedNode(level, newParentPath, node);
            }
        }

    }

    private void navigateNormalizedNodeContainerMixin(final int level, final String parentPath,
            final NormalizedNodeContainer<?> node) {
        visitor.visitNode(level, parentPath, node);

        String newParentPath = parentPath + "/" + node.getIdentifier().toString();

        for (NormalizedNode normalizedNode : node.body()) {
            if (normalizedNode instanceof MixinNode && normalizedNode instanceof NormalizedNodeContainer) {
                navigateNormalizedNodeContainerMixin(level + 1, newParentPath,
                        (NormalizedNodeContainer<?>) normalizedNode);
            } else {
                navigateNormalizedNode(level, newParentPath, normalizedNode);
            }
        }

    }

    private void navigateNormalizedNode(final int level, final String parentPath, final NormalizedNode normalizedNode) {
        if (normalizedNode instanceof DataContainerNode) {
            navigateDataContainerNode(level + 1, parentPath, (DataContainerNode) normalizedNode);
        } else {
            visitor.visitNode(level + 1, parentPath, normalizedNode);
        }
    }
}
