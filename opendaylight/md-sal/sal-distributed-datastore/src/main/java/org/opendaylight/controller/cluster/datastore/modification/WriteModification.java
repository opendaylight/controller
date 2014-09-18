/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.modification;

import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Decoded;
import org.opendaylight.controller.cluster.datastore.node.NormalizedNodeToNodeCodec.Encoded;
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * WriteModification stores all the parameters required to write data to the specified path
 */
public class WriteModification extends AbstractModification {

    protected final NormalizedNode data;
    private final SchemaContext schemaContext;

    public WriteModification(YangInstanceIdentifier path, NormalizedNode data, SchemaContext schemaContext) {
        super(path);
        this.data = data;
        this.schemaContext = schemaContext;
    }

    @Override
    public void apply(DOMStoreWriteTransaction transaction) {
        transaction.write(path, data);
    }

    public NormalizedNode getData() {
        return data;
    }

    @Override
    public Object toSerializable() {
        Encoded encoded = new NormalizedNodeToNodeCodec(schemaContext).encode(path, data);

        return PersistentMessages.Modification.newBuilder()
                .setType(this.getClass().toString())
                .setPath(encoded.getEncodedPath())
                .setData(encoded.getEncodedNode().getNormalizedNode())
                .build();
    }

    public static WriteModification fromSerializable(Object serializable, SchemaContext schemaContext) {
        PersistentMessages.Modification o = (PersistentMessages.Modification) serializable;
        Decoded decoded = new NormalizedNodeToNodeCodec(schemaContext).decode(o.getPath(), o.getData());
        return new WriteModification(decoded.getDecodedPath(), decoded.getDecodedNode(), schemaContext);
    }
}
