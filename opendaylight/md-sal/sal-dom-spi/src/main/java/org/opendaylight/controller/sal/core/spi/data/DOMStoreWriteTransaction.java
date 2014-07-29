/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

public interface DOMStoreWriteTransaction extends DOMStoreTransaction {

    /**
     * Store a provided data at specified path. This acts as a add / replace
     * operation, which is to say that whole subtree will be replaced by
     * specified path.
     *
     * If you need add or merge of current object with specified use
     * {@link #merge(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType, org.opendaylight.yangtools.concepts.Path, Object)}
     *
     *
     * @param path
     * @param data
     *            Data object to be written
     *
     * @throws IllegalStateException
     *             if the client code already sealed transaction and invoked
     *             {@link #ready()}
     */
    void write(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     * Store a provided data at specified path. This acts as a add / replace
     * operation, which is to say that whole subtree will be replaced by
     * specified path.
     *
     * If you need add or merge of current object with specified use
     * {@link #merge(org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType, org.opendaylight.yangtools.concepts.Path, Object)}
     *
     *
     * @param path
     * @param data
     *            Data object to be written
     *
     * @throws IllegalStateException
     *             if the client code already sealed transaction and invoked
     *             {@link #ready()}
     */
    void merge(YangInstanceIdentifier path, NormalizedNode<?, ?> data);

    /**
     *
     * Deletes data and whole subtree located at provided path.
     *
     * @param path
     *            Path to delete
     * @throws IllegalStateException
     *             if the client code already sealed transaction and invoked
     *             {@link #ready()}
     */
    void delete(YangInstanceIdentifier path);

    /**
     *
     * Seals transaction, and returns three-phase commit cohort associated
     * with this transaction and DOM Store to be coordinated by coordinator.
     *
     * @return Three Phase Commit Cohort instance for this transaction.
     */
    DOMStoreThreePhaseCommitCohort ready();

}
