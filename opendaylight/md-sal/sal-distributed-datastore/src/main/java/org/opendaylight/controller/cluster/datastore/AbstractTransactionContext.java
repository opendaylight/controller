/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTransactionContext implements TransactionContext {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionContext.class);

    private long modificationCount = 0;

    private final TransactionIdentifier identifier;

    protected AbstractTransactionContext(TransactionIdentifier identifier) {
        this.identifier = identifier;
    }

    protected final TransactionIdentifier getIdentifier() {
        return identifier;
    }

    protected void incrementModificationCount(){
        modificationCount++;
    }

    protected void logModificationCount(){
        LOG.debug("Total modifications on Tx {} = [ {} ]", identifier, modificationCount);
    }
}