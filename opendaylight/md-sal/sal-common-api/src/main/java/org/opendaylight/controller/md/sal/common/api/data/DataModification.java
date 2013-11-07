/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.data;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
// FIXME: After 0.6 Release of YANGTools refactor to use Path marker interface for arguments.
// import org.opendaylight.yangtools.concepts.Path;
import org.opendaylight.yangtools.yang.common.RpcResult;

public interface DataModification<P/* extends Path<P> */, D> extends DataChange<P, D>, DataReader<P, D> {

    /**
     * Returns transaction identifier
     * 
     * @return Transaction identifier
     */
    Object getIdentifier();

    TransactionStatus getStatus();

    /**
     * 
     * Use {@link #putOperationalData(Object, Object)} instead.
     * 
     * @param path
     * @param data
     */
    void putRuntimeData(P path, D data);

    void putOperationalData(P path, D data);

    void putConfigurationData(P path, D data);

    /**
     * Use {@link #removeOperationalData(Object)}
     * 
     * @param path
     */
    void removeRuntimeData(P path);

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
