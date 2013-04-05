/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api.data;

import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Consumer;
import org.opendaylight.controller.sal.core.api.Provider;
import org.opendaylight.controller.yang.common.RpcResult;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.CompositeNodeModification;
import org.opendaylight.controller.yang.data.api.Node;


/**
 * DataBrokerService provides unified access to the data stores available in the
 * system.
 * 
 * 
 * @see DataProviderService
 * 
 */
public interface DataBrokerService extends BrokerService {

    
    Set<DataStoreIdentifier> getDataStores();
    
    /**
     * Returns a data from specified Data Store.
     * 
     * Returns all the data visible to the consumer from specified Data Store.
     * 
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @return data visible to the consumer
     */
    CompositeNode getData(DataStoreIdentifier store);

    /**
     * Returns a filtered subset of data from specified Data Store.
     * 
     * <p>
     * The filter is modeled as an hierarchy of {@link Node} starting with
     * {@link CompositeNode} representing data root. The semantics of the filter
     * tree is the same as filter semantics defined in the NETCONF protocol for
     * rpc operations <code>get</code> and <code>get-config</code> in Section 6
     * of RFC6241.
     * 
     * 
     * @see http://tools.ietf.org/html/rfc6241#section-6
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @param filter
     *            Data tree filter similar to the NETCONF filter
     * @return
     */
    CompositeNode getData(DataStoreIdentifier store, CompositeNode filter);

    /**
     * Returns a candidate data which are not yet commited.
     * 
     * 
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @return
     */
    CompositeNode getCandidateData(DataStoreIdentifier store);

    /**
     * Returns a filtered subset of candidate data from specified Data Store.
     * 
     * <p>
     * The filter is modeled as an hierarchy of {@link Node} starting with
     * {@link CompositeNode} representing data root. The semantics of the filter
     * tree is the same as filter semantics defined in the NETCONF protocol for
     * rpc operations <code>get</code> and <code>get-config</code> in Section 6
     * of RFC6241.
     * 
     * 
     * @see http://tools.ietf.org/html/rfc6241#section-6
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @param filter
     *            A CompositeNode filter
     * @return
     */
    CompositeNode getCandidateData(DataStoreIdentifier store,
            CompositeNode filter);

    /**
     * 
     * @param store
     *            Identifier of the store, in which will be the candidate data
     *            modified
     * @param changeSet
     *            Modification of data tree.
     * @return Result object containing the modified data tree if the operation
     *         was successful, otherwise list of the encountered errors.
     */
    RpcResult<CompositeNode> editCandidateData(DataStoreIdentifier store,
            CompositeNodeModification changeSet);

    /**
     * Initiates a two-phase commit of candidate data.
     * 
     * <p>
     * The {@link Consumer} could initiate a commit of candidate data
     * 
     * <p>
     * The successful commit changes the state of the system and may affect
     * several components.
     * 
     * <p>
     * The effects of successful commit of data are described in the
     * specifications and YANG models describing the {@link Provider} components
     * of controller. It is assumed that {@link Consumer} has an understanding
     * of this changes.
     * 
     * 
     * @see DataCommitHandler for further information how two-phase commit is
     *      processed.
     * @param store
     *            Identifier of the store, where commit should occur.
     * @return Result of the commit, containing success information or list of
     *         encountered errors, if commit was not successful.
     */
    Future<RpcResult<Void>> commit(DataStoreIdentifier store);
}
