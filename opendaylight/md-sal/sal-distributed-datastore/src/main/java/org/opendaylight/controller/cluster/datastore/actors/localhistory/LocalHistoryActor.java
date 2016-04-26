/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

import akka.actor.Props;
import akka.actor.UntypedActor;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LocalHistoryActor extends UntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(LocalHistoryActor.class);
    private Behavior behavior;

    private LocalHistoryActor(final LocalHistoryIdentifier historyId, final DataTree dataTree) {
        this.behavior = new IdleBehavior(new LocalHistoryContext(getSelf(), historyId, dataTree));
    }

    public static Props props(final LocalHistoryIdentifier historyId) {
        return Props.create(LocalHistoryActor.class, Preconditions.checkNotNull(historyId));
    }

    @Override
    public void onReceive(final Object command) {
        final Behavior nextBehavior = behavior.handleCommand(command);
        if (!behavior.equals(nextBehavior)) {
            LOG.trace("{} switched behavior from {} to {}", getSelf(), behavior, nextBehavior);
            behavior = nextBehavior;
        }

        if (nextBehavior == null) {
            LOG.debug("No more behaviors, stopping {}", getSelf());
            getContext().stop(getSelf());
        }
    }
}
