/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;

/**
 * Client interface for interacting with the frontend actor. This interface is the primary access point through
 * which the DistributedDataStore frontend interacts with backend Shards.
 *
 * Keep this interface as clean as possible, as it needs to be implemented in thread-safe and highly-efficient manner.
 *
 * @author Robert Varga
 */
@Beta
public interface DistributedDataStoreClient extends Identifiable<ClientIdentifier>, AutoCloseable {
    @Override
    ClientIdentifier getIdentifier();

    @Override
    void close();

    /**
     * Create a new local history. This method initiates an asynchronous instantiation of a local history on the back
     * end. ClientLocalHistory represents the interface exposed to the client.
     *
     * @return Future client history handle
     */
    // FIXME:
    CompletionStage<ClientLocalHistory> createLocalHistory();

    // TODO: add methods required by DistributedDataStore

}
