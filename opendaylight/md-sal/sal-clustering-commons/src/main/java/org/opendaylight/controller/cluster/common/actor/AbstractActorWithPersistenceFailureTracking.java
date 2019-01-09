/*
 * Copyright (c) 2019 Lumina Networks, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.common.actor;

import akka.actor.PoisonPill;
import akka.persistence.AbstractPersistentActor;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;

/**
 * Actor with tracking of persistence failures. A back-off supervisor can be
 * used to supervise the actor which will cause it to be restarted according to
 * back-off algorithm when stopped due to persistence failure.
 */
public abstract class AbstractActorWithPersistenceFailureTracking extends AbstractPersistentActor {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractActorWithPersistenceFailureTracking.class);

    private final boolean backoffSupervised;
    private boolean persistFailed;

    protected AbstractActorWithPersistenceFailureTracking(final boolean backoffSupervised) {
        this.backoffSupervised = backoffSupervised;
    }

    protected boolean isBackoffSupervised() {
        return backoffSupervised;
    }

    @VisibleForTesting
    void setPersistFailed(final boolean persistFailed) {
        this.persistFailed = persistFailed;
    }

    @Override
    public void onRecoveryFailure(final Throwable cause, final Option<Object> event) {
        if (event.isEmpty()) {
            persistFailed = true;
        }
        super.onRecoveryFailure(cause, event);
    }

    @Override
    public void onPersistFailure(final Throwable cause, final Object event, final long seqNr) {
        persistFailed = true;
        super.onPersistFailure(cause, event, seqNr);
    }

    @Override
    public void postStop() {
        super.postStop();
        if (persistFailed) {
            LOG.warn("{}: stopped because of persistence failure", persistenceId());
        } else if (backoffSupervised) {
            LOG.debug("{}: sending PoisonPill to parent", persistenceId());
            getContext().parent().tell(PoisonPill.getInstance(), self());
        }
    }
}
