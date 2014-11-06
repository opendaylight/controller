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
import org.opendaylight.controller.protobuff.messages.persistent.PersistentMessages;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * MergeModification stores all the parameters required to merge data into the specified path
 */
public class MergeModification extends WriteModification {
    private static final long serialVersionUID = 1L;

    public MergeModification(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data,
        final SchemaContext schemaContext) {
        super(path, data, schemaContext);
    }

    @Override
    public void apply(final DOMStoreWriteTransaction transaction) {
        transaction.merge(path, data);
    }

    public static MergeModification fromSerializable(final Object serializable, final SchemaContext schemaContext) {
        PersistentMessages.Modification o = (PersistentMessages.Modification) serializable;
        Decoded decoded = new NormalizedNodeToNodeCodec(schemaContext).decode(o.getPath(), o.getData());
        return new MergeModification(decoded.getDecodedPath(), decoded.getDecodedNode(), schemaContext);
    }
}
