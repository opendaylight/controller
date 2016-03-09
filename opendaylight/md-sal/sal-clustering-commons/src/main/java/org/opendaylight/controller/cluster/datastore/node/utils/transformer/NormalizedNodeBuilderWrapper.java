/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.transformer;

import com.google.common.base.Optional;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeContainerBuilder;
import org.opendaylight.yangtools.yang.data.util.DataSchemaContextNode;

public class NormalizedNodeBuilderWrapper {
    private final NormalizedNodeContainerBuilder<?,?,?,?> builder;
    private final YangInstanceIdentifier.PathArgument identifier;
    private final Optional<DataSchemaContextNode<?>> schemaNode;

    NormalizedNodeBuilderWrapper(NormalizedNodeContainerBuilder<?,?,?,?> builder,
            YangInstanceIdentifier.PathArgument identifier, Optional<DataSchemaContextNode<?>> schemaNode) {
        this.builder = builder;
        this.identifier = identifier;
        this.schemaNode = schemaNode;
    }

    public NormalizedNodeContainerBuilder builder(){
        return builder;
    }

    public QName nodeType(){
        return identifier.getNodeType();
    }

    public YangInstanceIdentifier.PathArgument identifier(){
        return identifier;
    }

    public Optional<DataSchemaContextNode<?>> getSchema() {
        return schemaNode;
    }
}
