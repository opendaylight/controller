/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A transaction that provides mutation capabilities on a data tree.
 * <p>
 * For more information on usage and examples, please see the documentation in {@link AsyncWriteTransaction}.
 */
public interface WriteTransaction extends AsyncWriteTransaction<InstanceIdentifier<?>, DataObject> {
    @Override
    void put(LogicalDatastoreType store, InstanceIdentifier<?> path, DataObject data);

    @Override
    void delete(LogicalDatastoreType store, InstanceIdentifier<?> path);
}
