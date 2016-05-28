/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import java.util.Map.Entry;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.datastore.actors.client.ShardLeaderResolver;

final class ModuleShardLeaderResolver extends ShardLeaderResolver {


    CompletionStage<Entry<ActorRef, ABIVersion>> getLeaderForCookie(final Long cookie) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
