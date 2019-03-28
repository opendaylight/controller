/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import java.net.URI;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * The NormalizedNodePruner removes all nodes from the input NormalizedNode that do not have a corresponding
 * schema element in the passed in SchemaContext.
 *
 * @deprecated Use {@link AbstractNormalizedNodePruner} instead.
 */
@Deprecated
public class NormalizedNodePruner extends AbstractNormalizedNodePruner {
    public static final URI BASE_NAMESPACE = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");

    public NormalizedNodePruner(final YangInstanceIdentifier nodePath, final SchemaContext schemaContext) {
        super(schemaContext);
        initialize(nodePath);
    }

    public NormalizedNode<?, ?> normalizedNode() {
        return normalizedNode;
    }
}
