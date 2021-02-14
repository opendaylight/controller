/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.dom.api;

import com.google.common.annotations.Beta;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;

/**
 * An extension to {@link DOMDataTreeProducer}, which allows users access
 * to information about the backing shard.
 *
 * @author Robert Varga
 */
@Beta
@Deprecated(forRemoval = true)
public interface CDSDataTreeProducer extends DOMDataTreeProducer {
    /**
     * Return a {@link CDSShardAccess} handle. This handle will remain valid
     * as long as this producer is operational. Returned handle can be accessed
     * independently from this producer and is not subject to the usual access
     * restrictions imposed on DOMDataTreeProducer state.
     *
     * @param subtree One of the subtrees to which are currently under control of this producer
     * @return A shard access handle.
     * @throws NullPointerException when subtree is null
     * @throws IllegalArgumentException if the specified subtree is not controlled by this producer
     * @throws IllegalStateException if this producer is no longer operational
     * @throws IllegalThreadStateException if the access rules to this producer
     *         are violated, for example if this producer is bound and this thread
     *         is currently not executing from a listener context.
     */
    @NonNull CDSShardAccess getShardAccess(@NonNull DOMDataTreeIdentifier subtree);
}

