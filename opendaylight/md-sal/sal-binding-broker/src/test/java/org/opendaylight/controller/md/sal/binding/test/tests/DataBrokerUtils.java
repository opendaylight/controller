/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.test.tests;

import com.google.common.base.Optional;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Helper methods for {@link DataBrokerr}.
 *
 * TODO Code like this exists about 1000 times all over ODL.  Where to best put this?  Or where does the same already exist?
 *
 * @author Michael Vorburger
 */
public class DataBrokerUtils {

    private final DataBroker dataBroker;

    // TODO @Inject
    public DataBrokerUtils(DataBroker dataBroker) {
        super();
        this.dataBroker = dataBroker;
    }

    public <T extends ChildOf<? extends DataRoot>> T read(LogicalDatastoreType store, final Class<T> container)
            throws ReadFailedException {

        InstanceIdentifier<T> containerInstanceIdentifier = InstanceIdentifier.builder(container).build();
        try (ReadOnlyTransaction tx = dataBroker.newReadOnlyTransaction()) {
            Optional<T> optional = tx.read(store, containerInstanceIdentifier).checkedGet();
            if (optional.isPresent()) {
                return optional.get();
            } else {
                throw new ReadFailedException(
                        "Nothing in the " + store + " datastore at: " + containerInstanceIdentifier);
            }
        }
    }

    public <T extends DataObject> void write(LogicalDatastoreType store, InstanceIdentifier<T> path, T data) throws TransactionCommitFailedException {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(store, path, data, true);
        tx.submit().checkedGet();
    }

    public <T extends DataObject> void write(LogicalDatastoreType store, Entry<InstanceIdentifier<T>, T> entry) throws TransactionCommitFailedException {
        write(store, entry.getKey(), entry.getValue());
    }

}
