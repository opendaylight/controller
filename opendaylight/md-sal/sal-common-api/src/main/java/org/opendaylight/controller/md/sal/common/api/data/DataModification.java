/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;
/**
 *
 * @deprecated Replaced by {@link AsyncWriteTransaction}
 */
@Deprecated
public interface DataModification<P extends Path<P>, D> extends DataChange<P, D>, DataReader<P, D> {
    /**
     * Returns transaction identifier
     *
     * @return Transaction identifier
     */
    Object getIdentifier();

    TransactionStatus getStatus();

    /**
     * Store a piece of data at specified path. This acts as a merge operation,
     * which is to say that any pre-existing data which is not explicitly
     * overwritten will be preserved. This means that if you store a container,
     * its child lists will be merged. Performing the following put operations:
     *
     * 1) container { list [ a ] }
     * 2) container { list [ b ] }
     *
     * will result in the following data being present:
     *
     * container { list [ a, b ] }
     *
     * This also means that storing the container will preserve any augmentations
     * which have been attached to it.
     *
     * If you require an explicit replace operation, perform
     * {@link removeOperationalData} first.
     */
    void putOperationalData(P path, D data);

    /**
     * Store a piece of data at specified path. This acts as a merge operation,
     * which is to say that any pre-existing data which is not explicitly
     * overwritten will be preserved. This means that if you store a container,
     * its child lists will be merged. Performing the following put operations:
     *
     * 1) container { list [ a ] }
     * 2) container { list [ b ] }
     *
     * will result in the following data being present:
     *
     * container { list [ a, b ] }
     *
     * This also means that storing the container will preserve any augmentations
     * which have been attached to it.
     *
     * If you require an explicit replace operation, perform
     * {@link removeConfigurationData} first.
     */
    void putConfigurationData(P path, D data);

    void removeOperationalData(P path);

    void removeConfigurationData(P path);

    /**
     * Initiates a two-phase commit of modification.
     *
     * <p>
     * The successful commit changes the state of the system and may affect
     * several components.
     *
     * <p>
     * The effects of successful commit of data are described in the
     * specifications and YANG models describing the Provider components of
     * controller. It is assumed that Consumer has an understanding of this
     * changes.
     *
     *
     * @see DataCommitHandler for further information how two-phase commit is
     *      processed.
     * @param store
     *            Identifier of the store, where commit should occur.
     * @return Result of the Commit, containing success information or list of
     *         encountered errors, if commit was not successful. The Future
     *         blocks until {@link TransactionStatus#COMMITED} or
     *         {@link TransactionStatus#FAILED} is reached.
     */
    Future<RpcResult<TransactionStatus>> commit();
}
