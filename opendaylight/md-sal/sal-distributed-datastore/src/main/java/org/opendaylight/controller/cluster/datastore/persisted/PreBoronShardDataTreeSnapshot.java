/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Legacy data tree snapshot used in versions prior to Boron, which contains only the data.
 *
 * @author Robert Varga
 */
@Beta
public final class PreBoronShardDataTreeSnapshot extends ShardDataTreeSnapshot {
    private final NormalizedNode<?, ?> rootNode;

    @Deprecated
    public PreBoronShardDataTreeSnapshot(final @Nullable NormalizedNode<?, ?> rootNode) {
        this.rootNode = rootNode;
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getRootNode() {
        return Optional.ofNullable(rootNode);
    }

    @Override
    public byte[] serialize() {
        return SerializationUtils.serializeNormalizedNode(rootNode);
    }
}