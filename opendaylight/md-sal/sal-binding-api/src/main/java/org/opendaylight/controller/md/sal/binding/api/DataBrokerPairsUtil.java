/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import java.util.Map;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Utility around {@link WriteTransaction} for working with pairs and tuples of Type/Id/DataObject.
 *
 * <ul><li>{@link java.util.Map.Entry}s pairs of {@link InstanceIdentifier}s and {@link DataObject}s.
 * <li>Triples of
 * {@link LogicalDatastoreType}s/InstanceIdentifiers/DataObjects, with
 * <li>{@link DataTreeIdentifierIdentifiableBuilder},
 * <li>and {@link IdentifierIdentifiableBuilder}.
 * </ul>
 *
 * @author Michael Vorburger
 */
public class DataBrokerPairsUtil {
    // TODO rename to e.g. PairsWriteTransaction, or some other more clear name - which?

    private final WriteTransaction tx;

    @Inject
    public DataBrokerPairsUtil(WriteTransaction tx) {
        super();
        this.tx = tx;
    }

    /**
     * Put the pair of InstanceIdentifier and DataObject into the given LogicalDatastoreType Data Store.
     */
    public <T extends DataObject> void put(LogicalDatastoreType type, Map.Entry<InstanceIdentifier<T>, T> pair) {
        tx.put(type, pair.getKey(), pair.getValue());
    }

    /**
     * Put the pair of InstanceIdentifier and DataObject produced by the passed
     * Builder into the given LogicalDatastoreType Data Store.
     */
    public <T extends DataObject> void put(LogicalDatastoreType type, IdentifierIdentifiableBuilder<T> builder) {
        put(type, builder.build());
    }

    /**
     * Put the tuple of LogicalDatastoreType, InstanceIdentifier and DataObject into the Data Store.
     */
    public <T extends DataObject> void put(Map.Entry<DataTreeIdentifier<T>, T> pair) {
        tx.put(pair.getKey().getDatastoreType(), pair.getKey().getRootIdentifier(), pair.getValue());
    }

    /**
     * Put the tuple of LogicalDatastoreType, InstanceIdentifier and DataObject produced by the passed
     * Builder into the Data Store.
     */
    public <T extends DataObject> void put(DataTreeIdentifierIdentifiableBuilder<T> builder) {
        put(builder.build());
    }

}
