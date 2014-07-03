/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore.messages;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public class DataChanged {
    private final AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>>
        change;

    public DataChanged(
        AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> change) {
        this.change = change;
    }

    public AsyncDataChangeEvent<InstanceIdentifier, NormalizedNode<?, ?>> getChange() {
        return change;
    }
}
