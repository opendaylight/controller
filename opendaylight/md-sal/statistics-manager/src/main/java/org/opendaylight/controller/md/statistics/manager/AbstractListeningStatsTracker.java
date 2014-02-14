/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.statistics.manager;

import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

abstract class AbstractListeningStatsTracker<I, K> extends AbstractStatsTracker<I, K> implements AutoCloseable, DataChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(AbstractListeningStatsTracker.class);
    private ListenerRegistration<?> reg;

    protected AbstractListeningStatsTracker(FlowCapableContext context, long lifetimeNanos) {
        super(context, lifetimeNanos);
    }

    protected abstract InstanceIdentifier<?> listenPath();
    protected abstract String statName();

    public void start(final DataBrokerService dbs) {
        Preconditions.checkState(reg == null);

        reg = dbs.registerDataChangeListener(listenPath(), this);
        logger.debug("{} Statistics tracker for node {} started", statName(), getNodeIdentifier());
    }

    @Override
    public final void close() {
        if (reg != null) {
            try {
                reg.close();
            } catch (Exception e) {
                logger.warn("Failed to stop {} Statistics tracker for node {}", statName(), getNodeIdentifier(), e);
            }
            reg = null;
        }
    }
}
