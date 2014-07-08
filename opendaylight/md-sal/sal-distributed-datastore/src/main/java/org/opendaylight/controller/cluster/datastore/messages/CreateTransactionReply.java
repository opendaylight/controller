/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import akka.actor.ActorPath;

/**
 * This is being deprecated to use sal-protocolbuff-encoding ShardTransactionMessages.CreateTransactionReply
 * This classes will be removed once complete integration of distribute datastore with
 * sal-protocolbuff-encoding is done.
 */

@Deprecated
public class CreateTransactionReply {
    private final ActorPath transactionPath;
    private final String transactionId;

    public CreateTransactionReply(ActorPath transactionPath,
        String transactionId) {
        this.transactionPath = transactionPath;
        this.transactionId = transactionId;
    }

    public ActorPath getTransactionPath() {
        return transactionPath;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
