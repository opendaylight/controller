/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.compat;

import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.japi.Creator;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An actor to maintain backwards compatibility for the base Helium version where the 3-phase commit
 * messages don't contain the transactionId. This actor just forwards a new message containing the
 * transactionId to the parent Shard.
 *
 * @author Thomas Pantelis
 */
public class BackwardsCompatibleThreePhaseCommitCohort extends AbstractUntypedActor {

    private static final Logger LOG = LoggerFactory.getLogger(BackwardsCompatibleThreePhaseCommitCohort.class);

    private final String transactionId;

    private BackwardsCompatibleThreePhaseCommitCohort(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public void handleReceive(Object message) throws Exception {
        if(message.getClass().equals(CanCommitTransaction.SERIALIZABLE_CLASS)) {
            LOG.debug("BackwardsCompatibleThreePhaseCommitCohort CanCommitTransaction");

            getContext().parent().forward(new CanCommitTransaction(transactionId).toSerializable(),
                    getContext());
        } else if(message.getClass().equals(PreCommitTransaction.SERIALIZABLE_CLASS)) {
            LOG.debug("BackwardsCompatibleThreePhaseCommitCohort PreCommitTransaction");

            // The Shard doesn't need the PreCommitTransaction message so just return the reply here.
            getSender().tell(new PreCommitTransactionReply().toSerializable(), self());
        } else if(message.getClass().equals(CommitTransaction.SERIALIZABLE_CLASS)) {
            LOG.debug("BackwardsCompatibleThreePhaseCommitCohort CommitTransaction");

            getContext().parent().forward(new CommitTransaction(transactionId).toSerializable(),
                    getContext());

            // We're done now - we can self-destruct
            self().tell(PoisonPill.getInstance(), self());
        } else if(message.getClass().equals(AbortTransaction.SERIALIZABLE_CLASS)) {
            LOG.debug("BackwardsCompatibleThreePhaseCommitCohort AbortTransaction");

            getContext().parent().forward(new AbortTransaction(transactionId).toSerializable(),
                    getContext());
            self().tell(PoisonPill.getInstance(), self());
        }
    }

    public static Props props(String transactionId) {
        return Props.create(new BackwardsCompatibleThreePhaseCommitCohortCreator(transactionId));
    }

    private static class BackwardsCompatibleThreePhaseCommitCohortCreator
                                  implements Creator<BackwardsCompatibleThreePhaseCommitCohort> {
        private static final long serialVersionUID = 1L;

        private final String transactionId;

        BackwardsCompatibleThreePhaseCommitCohortCreator(String transactionId) {
            this.transactionId = transactionId;
        }

        @Override
        public BackwardsCompatibleThreePhaseCommitCohort create() throws Exception {
            return new BackwardsCompatibleThreePhaseCommitCohort(transactionId);
        }
    }
}
