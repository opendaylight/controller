/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import org.opendaylight.controller.md.sal.common.api.data.AsyncWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * A transaction that provides mutation capabilities on a data tree.
 * <p>
 * For more information on usage and examples, please see the documentation in {@link AsyncWriteTransaction}.
 */
public interface DOMDataWriteTransaction extends AsyncWriteTransaction<YangInstanceIdentifier, NormalizedNode<?, ?>> {

    /**
     * Stores a piece of data at the specified path. This acts as an add / replace
     * operation, which is to say that whole subtree will be replaced by the specified data.
     * <p>
     * For more information on usage and examples, please see the documentation in {@link AsyncWriteTransaction}.
     * <p>
     * If you need to make sure that a parent object exists but you do not want modify
     * its pre-existing state by using put, consider using {@link #merge} instead.
     *
     * @param store
     *            the logical data store which should be modified
     * @param path
     *            the data object path
     * @param data
     *            the data object to be written to the specified path
     * @throws IllegalStateException
     *             if the transaction has already been submitted
     */
    void put(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     * Merges a piece of data with the existing data at a specified path. Any pre-existing data
     * which is not explicitly overwritten will be preserved. This means that if you store a container,
     * its child lists will be merged.
     * <p>
     * For more information on usage and examples, please see the documentation in {@link AsyncWriteTransaction}.
     *<p>
     * If you require an explicit replace operation, use {@link #put} instead.
     *
     * @param store
     *            the logical data store which should be modified
     * @param path
     *            the data object path
     * @param data
     *            the data object to be merged to the specified path
     * @throws IllegalStateException
     *             if the transaction has already been submitted
     */
    void merge(LogicalDatastoreType store, YangInstanceIdentifier path, NormalizedNode<?, ?> data);
}
