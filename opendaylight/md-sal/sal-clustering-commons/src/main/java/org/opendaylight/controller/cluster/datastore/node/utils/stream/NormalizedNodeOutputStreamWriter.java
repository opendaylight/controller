/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.node.utils.stream;

import com.google.common.base.Preconditions;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;

/**
 * NormalizedNodeOutputStreamWriter will be used by distributed datastore to send normalized node in
 * a stream.
 * A stream writer wrapper around this class will write node objects to stream in recursive manner.
 * for example - If you have a ContainerNode which has a two LeafNode as children, then
 * you will first call {@link #startContainerNode(YangInstanceIdentifier.NodeIdentifier, int)}, then will call
 * {@link #leafNode(YangInstanceIdentifier.NodeIdentifier, Object)} twice and then, {@link #endNode()} to end
 * container node.
 *
 * Based on the each node, the node type is also written to the stream, that helps in reconstructing the object,
 * while reading.
 */
public class NormalizedNodeOutputStreamWriter extends AbstractNormalizedNodeDataOutput implements NormalizedNodeStreamWriter {
    /**
     * @deprecated Use {@link #NormalizedNodeOutputStreamWriter(DataOutput)} instead.
     */
    @Deprecated
    public NormalizedNodeOutputStreamWriter(final OutputStream stream) throws IOException {
        this((DataOutput) new DataOutputStream(Preconditions.checkNotNull(stream)));
    }

    /**
     * @deprecated Use {@link NormalizedNodeInputOutput#newDataOutput(DataOutput)} instead.
     */
    @Deprecated
    public NormalizedNodeOutputStreamWriter(final DataOutput output) {
        super(output, new NormalizedNodeOutputDictionary());
    }

    @Override
    protected final short streamVersion() {
        return TokenTypes.LITHIUM_VERSION;
    }
}
