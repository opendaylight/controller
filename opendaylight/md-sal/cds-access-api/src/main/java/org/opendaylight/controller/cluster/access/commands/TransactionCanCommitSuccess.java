/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.commands;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Successful reply to a coordinated commit request. It contains a reference to the actor which is handling the commit
 * process.
 *
 * @author Robert Varga
 */
public final class TransactionCanCommitSuccess extends TransactionSuccess<TransactionCanCommitSuccess> {
    private static final long serialVersionUID = 1L;
    private final ActorRef cohort;

    public TransactionCanCommitSuccess(final TransactionIdentifier identifier, final ActorRef cohort) {
        super(identifier);
        this.cohort = Preconditions.checkNotNull(cohort);
    }

    public ActorRef getCohort() {
        return cohort;
    }

    @Override
    protected AbstractTransactionSuccessProxy<TransactionCanCommitSuccess> externalizableProxy(final ABIVersion version) {
        return new TransactionCanCommitSuccessProxyV1(this);
    }

    @Override
    protected TransactionCanCommitSuccess cloneAsVersion(final ABIVersion version) {
        return this;
    }
}
