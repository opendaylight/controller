/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Optional;
import org.opendaylight.controller.cluster.datastore.utils.SerializationUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * An {@link AbstractVersionedShardDataTreeSnapshot} which contains additional metadata.
 *
 * @author Robert Varga
 */
@Beta
public final class BoronShardDataTreeSnapshot extends AbstractVersionedShardDataTreeSnapshot implements Serializable {
    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private NormalizedNode<?, ?> rootNode;

        public Proxy() {
            // For Externalizable
        }

        Proxy(final BoronShardDataTreeSnapshot snapshot) {
            this.rootNode = snapshot.getRootNode().get();
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            SerializationUtils.serializeNormalizedNode(rootNode, out);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            rootNode = Verify.verifyNotNull(SerializationUtils.deserializeNormalizedNode(in));
        }

        private Object readResolve() {
            return new BoronShardDataTreeSnapshot(rootNode);
        }
    }

    private static final long serialVersionUID = 1L;

    private final NormalizedNode<?, ?> rootNode;

    public BoronShardDataTreeSnapshot(final NormalizedNode<?, ?> rootNode) {
        this.rootNode = Preconditions.checkNotNull(rootNode);
    }

    @Override
    public Optional<NormalizedNode<?, ?>> getRootNode() {
        return Optional.of(rootNode);
    }

    @Override
    PayloadVersion getVersion() {
        return PayloadVersion.BORON;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }
}
