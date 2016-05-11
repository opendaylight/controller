/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors;

import akka.actor.Props;
import com.google.common.base.Preconditions;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Client interface for interacting with the frontend actor. This interface is the primary access point through
 * which the DistributedDataStore frontend interacts with backend Shards.
 *
 * Keep this interface as clean as possible, as it needs to be implemented in thread-safe and highly-efficient manner.
 */
public interface DistributedDataStoreClient extends Identifiable<ClientIdentifier<DistributedDataStoreFrontend>> {
    static Props props(final @Nonnull MemberName memberName, @Nonnull final String storeName) {
        return props(memberName, storeName, null);
    }

    static Props props(final @Nonnull MemberName memberName, final @Nonnull String storeName,
            final @Nullable CompletableFuture<DistributedDataStoreClientBehavior> future) {
        if (future != null) {
            Preconditions.checkArgument(!future.isDone(), "Future should not be completed");
        }

        return Props.create(DistributedDataStoreClientActor.class,
            FrontendIdentifier.create(memberName, new DistributedDataStoreFrontend(storeName)), future);
    }

    @Override
    ClientIdentifier<DistributedDataStoreFrontend> getIdentifier();

    /**
     * Create a new local history. This method initiates an asynchronous instantiation of a local history on the back
     * end. ClientLocalHistory represents the interface exposed to the client.
     *
     * @return Future client history handle
     */
    CompletionStage<ClientLocalHistory> createLocalHistory();

    // TODO: add methods required by DistributedDataStore

}
