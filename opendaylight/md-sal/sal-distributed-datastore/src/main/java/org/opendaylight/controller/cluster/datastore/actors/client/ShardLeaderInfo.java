/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.client;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.ABIVersion;

/**
 * Information about a backend leader. This information is acquired by a particular client actor information and is
 * used by the baseline infrastructure to route requests to the appropriate destination.
 *
 * This class is not final to allow implementations to subclass it, adding additional information they need, notably
 * in their {@link RequestTransformer} implementation.
 *
 * @author Robert Varga
 */
@Beta
public class ShardLeaderInfo {
    private final ActorRef actor;
    private final ABIVersion version;

    protected ShardLeaderInfo(final ActorRef actor, final ABIVersion version) {
        this.actor = Preconditions.checkNotNull(actor);
        this.version = Preconditions.checkNotNull(version);
    }

    public final ActorRef getActor() {
        return actor;
    }

    public final ABIVersion getVersion() {
        return version;
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("actor", actor).add("version", version).toString();
    }
}
