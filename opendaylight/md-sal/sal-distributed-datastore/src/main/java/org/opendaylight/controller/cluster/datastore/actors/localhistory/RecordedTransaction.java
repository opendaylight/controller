/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.actors.localhistory;

final class RecordedTransaction {
    static final RecordedTransaction ZERO = new RecordedTransaction(0);

    final long lastRequest;

    RecordedTransaction(final long lastRequest) {
        this.lastRequest = lastRequest;
    }

    long getLastRequest() {
        return lastRequest;
    }
}
