/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.actor.UntypedActor;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransaction;
import org.opendaylight.controller.cluster.datastore.messages.AbortTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CanCommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CommitTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransaction;
import org.opendaylight.controller.cluster.datastore.messages.PreCommitTransactionReply;

public class NoOpCohort extends UntypedActor {

    @Override public void onReceive(Object message) throws Exception {
        if (message.getClass().equals(CanCommitTransaction.SERIALIZABLE_CLASS)) {
            getSender().tell(new CanCommitTransactionReply(false).toSerializable(), getSelf());
        } else if (message.getClass().equals(PreCommitTransaction.SERIALIZABLE_CLASS)) {
            getSender().tell(
                new PreCommitTransactionReply().toSerializable(),
                getSelf());
        } else if (message.getClass().equals(CommitTransaction.SERIALIZABLE_CLASS)) {
            getSender().tell(new CommitTransactionReply().toSerializable(), getSelf());
        } else if (message.getClass().equals(AbortTransaction.SERIALIZABLE_CLASS)) {
            getSender().tell(new AbortTransactionReply().toSerializable(), getSelf());
        } else {
            throw new Exception ("Not recognized message received,message="+message);
        }

    }
}

