/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.persisted.FrontendShardDataTreeSnapshotMetadata;

public class TestShard extends Shard {
    // Message to request FrontendMetadata
    public static final class RequestFrontendMetadata {

    }

    protected TestShard(AbstractBuilder<?, ?> builder) {
        super(builder);
    }

    @Override
    protected void handleNonRaftCommand(Object message) {
        if (message instanceof  RequestFrontendMetadata) {
            FrontendShardDataTreeSnapshotMetadata metadataSnapshot = frontendMetadata.toSnapshot();
            sender().tell(metadataSnapshot, self());
        } else {
            super.handleNonRaftCommand(message);
        }
    }

    public static Shard.Builder builder() {
        return new TestShard.Builder();
    }

    public static class Builder extends Shard.Builder {
        Builder() {
            super(TestShard.class);
        }
    }
}

