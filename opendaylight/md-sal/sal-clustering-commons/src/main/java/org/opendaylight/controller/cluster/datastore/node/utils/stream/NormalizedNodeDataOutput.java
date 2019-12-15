/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.annotations.Beta;
import java.io.DataOutput;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Interface for emitting {@link NormalizedNode}s, {@link YangInstanceIdentifier}s, {@link PathArgument}s
 * and {@link SchemaPath}s.
 *
 * @deprecated Used {@link org.opendaylight.yangtools.yang.data.codec.binfmt.NormalizedNodeDataOutput} instead.
 */
@Deprecated(forRemoval = true)
@Beta
public interface NormalizedNodeDataOutput extends AutoCloseable, DataOutput {
    void writeQName(@NonNull QName qname) throws IOException;

    void writeNormalizedNode(@NonNull NormalizedNode<?, ?> normalizedNode) throws IOException;

    void writePathArgument(PathArgument pathArgument) throws IOException;

    void writeYangInstanceIdentifier(YangInstanceIdentifier identifier) throws IOException;

    void writeSchemaPath(SchemaPath path) throws IOException;

    @Override
    void close() throws IOException;

    default void writeOptionalNormalizedNode(final @Nullable NormalizedNode<?, ?> normalizedNode) throws IOException {
        if (normalizedNode != null) {
            writeBoolean(true);
            writeNormalizedNode(normalizedNode);
        } else {
            writeBoolean(false);
        }
    }
}
