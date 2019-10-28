/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

abstract class ForwardingNormalizedNodeDataInput extends ForwardingDataInput implements NormalizedNodeDataInput {

    @Override
    abstract @NonNull NormalizedNodeDataInput delegate() throws IOException;

    @Override
    public final void streamNormalizedNode(final NormalizedNodeStreamWriter writer) throws IOException {
        delegate().streamNormalizedNode(writer);
    }

    @Override
    public final NormalizedNode<?, ?> readNormalizedNode() throws IOException {
        return delegate().readNormalizedNode();
    }

    @Override
    public final YangInstanceIdentifier readYangInstanceIdentifier() throws IOException {
        return delegate().readYangInstanceIdentifier();
    }

    @Override
    public final PathArgument readPathArgument() throws IOException {
        return delegate().readPathArgument();
    }

    @Override
    public final SchemaPath readSchemaPath() throws IOException {
        return delegate().readSchemaPath();
    }

    @Override
    public final NormalizedNodeStreamVersion getVersion() throws IOException {
        return delegate().getVersion();
    }
}
