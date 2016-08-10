/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.ShardedDOMAdapter;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;

final class MdsalDelegatingDataBrokerUtils {

    private MdsalDelegatingDataBrokerUtils() {
        throw new AssertionError("Util class should not be instantiated");
    }

    public static org.opendaylight.mdsal.common.api.LogicalDatastoreType translateDataStoreType(final LogicalDatastoreType store) {
        return store.equals(LogicalDatastoreType.CONFIGURATION) ?
                org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION :
                org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;
    }
}
