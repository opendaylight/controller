/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.confignetconfconnector.operations;

import org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig.CandidateDatastoreQueryStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig.DatastoreQueryStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.operations.getconfig.RunningDatastoreQueryStrategy;
import org.opendaylight.controller.netconf.confignetconfconnector.transactions.TransactionProvider;

public enum Datastore {

    running, candidate;

    /**
     * @param source
     * @param transactionProvider
     * @return
     */
    public static DatastoreQueryStrategy getInstanceQueryStrategy(Datastore source,
            TransactionProvider transactionProvider) {
        switch (source) {
        case running:
            return new RunningDatastoreQueryStrategy();
        case candidate:
            return new CandidateDatastoreQueryStrategy(transactionProvider);
        default:
            throw new UnsupportedOperationException("Unimplemented datastore query strategy for " + source);
        }
    }
}
