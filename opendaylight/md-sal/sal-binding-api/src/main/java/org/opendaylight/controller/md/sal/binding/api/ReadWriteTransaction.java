/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A transaction that enables combined read/write capabilities.
 * <p>
 * For more information on usage and examples, please see the documentation in {@link AsyncReadWriteTransaction}.
 */
public interface ReadWriteTransaction extends ReadTransaction, WriteTransaction, AsyncReadWriteTransaction<InstanceIdentifier<?>, DataObject> {

    @Override
    public void put(LogicalDatastoreType store, InstanceIdentifier<?> path,
            DataObject data);

    @Override
    public void merge(LogicalDatastoreType store, InstanceIdentifier<?> path,
            DataObject data);

    /**
     * Puts data and creates default parents if parent nodes are missing.
     *
     * <p>
     * This method behaves as {@link #put(LogicalDatastoreType, InstanceIdentifier, DataObject)}
     * but also ensures parents if possible: allows for deep writes,
     * and creates missing parents if required. Parents are created
     * using {@link #merge(LogicalDatastoreType, InstanceIdentifier, DataObject)}
     * method with only minimum necessary fields set (usually key only)
     * which are derived from provided keys in {@link InstanceIdentifier}
     * supplied in path.
     * <p>
     * This is equivalent of:
     * <pre>
     * Iterator&lt;PathArgument&gt; pathArg = path.getPathArguments().iterator();
     * InstanceIdentifier current = InstanceIdentifier.builder.build();
     * while(pathArg.hasNext()) {
     *     current = current.child(pathArg.next);
     *     if(false == tx.read(store,current).get().isPresent()) {
     *          tx.merge(current,defaultValueFor(current));
     *     }
     * }
     * </pre>
     * But has potential to be more efficient, since MD-SAL implementations tend
     * to optimise it.
     *
     *
     * @param store
     *            Logical data store which should be modified
     * @param path
     *            Data object path
     * @param data
     *            Data object to be written to specified path
     * @throws IllegalStateException
     */
    public void putAndEnsureParents(LogicalDatastoreType store, InstanceIdentifier<?> path,
            DataObject data);

    /**
     *
     * <p>
     * This method behaves as {@link #put(LogicalDatastoreType, InstanceIdentifier, DataObject)}
     * but also ensures parents if possible: allows for deep writes,
     * and creates missing parents if required. Parents are created
     * using {@link #merge(LogicalDatastoreType, InstanceIdentifier, DataObject)}
     * method with only minimum necessary fields set (usually key only)
     * which are derived from provided keys in {@link InstanceIdentifier}
     * supplied in path.
     * <p>
     * This is equivalent of:
     * <pre>
     * Iterator&lt;PathArgument&gt; pathArg = path.getPathArguments().iterator();
     * InstanceIdentifier current = InstanceIdentifier.builder.build();
     * while(pathArg.hasNext()) {
     *     current = current.child(pathArg.next);
     *     if(false == tx.read(store,current).get().isPresent()) {
     *          tx.merge(current,defaultValueFor(current));
     *     }
     * }
     * </pre>
     * But has potential to be more efficient, since MD-SAL implementations tend
     * to optimise it.
     *
     */
    public void mergeAndEnsureParents(LogicalDatastoreType store, InstanceIdentifier<?> path,
            DataObject data);

}
