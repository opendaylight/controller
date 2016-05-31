/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.WritableObject;
import org.opendaylight.yangtools.concepts.Identifier;

/**
 * {@link Request} transformer, invoked by the Client Actor before a Request is being resent to a particular target.
 *
 * @author Robert Varga
 */
@Beta
@FunctionalInterface
public interface RequestTransformer {
    static <T extends Identifier & WritableObject> Request<T, ?> toVersion(@Nonnull final Request<T, ?> original,
            @Nonnull final ShardLeaderInfo info) {
        return original.toVersion(info.getVersion());
    }

    static final RequestTransformer TO_VERSION = RequestTransformer::toVersion;

    /**
     * Given an original request and current {@link ShardLeaderInfo}, return the appropriate request to send
     *
     * @param original Original request
     * @param info Current information about shard leader
     * @return Request which should be sent to the leader
     */
    @Nonnull <T extends Identifier & WritableObject> Request<T, ?> transform(@Nonnull Request<T, ?> original,
            @Nonnull ShardLeaderInfo info);
}
