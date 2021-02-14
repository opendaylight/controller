/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.dom.api;

import com.google.common.annotations.Beta;
import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;

/**
 * Unprivileged access interface to shard information. Provides read-only access to operational details about a CDS
 * shard.
 *
 * @author Robert Varga
 */
@Beta
@Deprecated(forRemoval = true)
public interface CDSShardAccess {
    /**
     * Return the shard identifier.
     *
     * @return Shard identifier.
     * @throws IllegalStateException if the {@link CDSDataTreeProducer} from which the associated
     *         {@link CDSDataTreeProducer} is no longer valid.
     */
    @NonNull DOMDataTreeIdentifier getShardIdentifier();

    /**
     * Return the shard leader location relative to the local node.
     *
     * @return Shard leader location.
     * @throws IllegalStateException if the {@link CDSDataTreeProducer} from which the associated
     *         {@link CDSDataTreeProducer} is no longer valid.
     */
    @NonNull LeaderLocation getLeaderLocation();

    /**
     * Request the shard leader to be moved to the local node. The request will be evaluated against shard state and
     * satisfied if leader movement is possible. If current shard policy or state prevents the movement from happening,
     * the returned {@link CompletionStage} will report an exception.
     *
     * <p>
     * This is a one-time operation, which does not prevent further movement happening in future. Even if this request
     * succeeds, there is no guarantee that the leader will remain local in face of failures, shutdown or any future
     * movement requests from other nodes.
     *
     * <p>
     * Note that due to asynchronous nature of CDS, the leader may no longer be local by the time the returned
     * {@link CompletionStage} reports success.
     *
     * @return A {@link CompletionStage} representing the request.
     * @throws IllegalStateException if the {@link CDSDataTreeProducer} from which the associated
     *         {@link CDSDataTreeProducer} is no longer valid.
     */
    @NonNull CompletionStage<Void> makeLeaderLocal();

    /**
     * Register a listener to shard location changes. Each listener object can be registered at most once.
     *
     * @param listener Listener object
     * @return A {@link LeaderLocationListenerRegistration} for the listener.
     * @throws IllegalArgumentException if the specified listener is already registered.
     * @throws IllegalStateException if the {@link CDSDataTreeProducer} from which the associated
     *         {@link CDSDataTreeProducer} is no longer valid.
     * @throws NullPointerException if listener is null.
     */
    @NonNull <L extends LeaderLocationListener> LeaderLocationListenerRegistration<L> registerLeaderLocationListener(
            @NonNull L listener);
}
