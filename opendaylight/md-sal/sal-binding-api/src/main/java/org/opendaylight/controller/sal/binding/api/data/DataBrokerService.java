/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api.data;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.DataChangePublisher;
import org.opendaylight.controller.md.sal.common.api.data.DataModificationTransactionFactory;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.binding.api.BindingAwareService;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * DataBrokerService provides unified access to the data stores available in the
 * system.
 * 
 * 
 * @see DataProviderService
 */
public interface DataBrokerService extends //
        BindingAwareService, //
        DataModificationTransactionFactory<InstanceIdentifier<? extends DataObject>, DataObject>, //
        DataReader<InstanceIdentifier<? extends DataObject>, DataObject>, //
        DataChangePublisher<InstanceIdentifier<? extends DataObject>, DataObject, DataChangeListener> {

    /**
     * Returns a data from specified Data Store.
     * 
     * Returns all the data visible to the consumer from specified Data Store.
     * 
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @return data visible to the consumer
     */
    @Deprecated
    <T extends DataRoot> T getData(DataStoreIdentifier store, Class<T> rootType);

    /**
     * Returns a filtered subset of data from specified Data Store.
     * 
     * <p>
     * The filter is modeled as an hierarchy of Java TOs starting with
     * implementation of {@link DataRoot} representing data root. The semantics
     * of the filter tree is the same as filter semantics defined in the NETCONF
     * protocol for rpc operations <code>get</code> and <code>get-config</code>
     * in Section 6 of RFC6241.
     * 
     * 
     * @see http://tools.ietf.org/html/rfc6241#section-6
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @param filter
     *            Data tree filter similar to the NETCONF filter
     * @return
     */
    @Deprecated
    <T extends DataRoot> T getData(DataStoreIdentifier store, T filter);

    /**
     * Returns a candidate data which are not yet commited.
     * 
     * 
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @return
     */
    @Deprecated
    <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, Class<T> rootType);

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
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @param filter
     *            A filter data root
     * @return
     */
    @Deprecated
    <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter);

    /**
     * 
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, in which will be the candidate data
     *            modified
     * @param changeSet
     *            Modification of data tree.
     * @return Result object containing the modified data tree if the operation
     *         was successful, otherwise list of the encountered errors.
     */
    @Deprecated
    RpcResult<DataRoot> editCandidateData(DataStoreIdentifier store, DataRoot changeSet);

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
    @Deprecated
    Future<RpcResult<Void>> commit(DataStoreIdentifier store);

    @Deprecated
    DataObject getData(InstanceIdentifier<? extends DataObject> data);

    @Deprecated
    DataObject getConfigurationData(InstanceIdentifier<?> data);
    /**
     * Creates a data modification transaction.
     * 
     * @return new blank data modification transaction.
     */
    DataModificationTransaction beginTransaction();

    @Deprecated
    public void registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener);

    @Deprecated
    public void unregisterChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener);

    /**
     * Reads data subtree from configurational store. 
     * (Store which is populated by consumer, which is usually used to 
     * inject state into providers. E.g. Flow configuration)-
     * 
     */
    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path);
    
    /**
     * Reads data subtree from operational store. 
     * (Store which is populated by providers, which is usually used to 
     * capture state of providers. E.g. Topology)
     * 
     */
    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path);
    
    /**
     * Register a data change listener for particular subtree. 
     * 
     * Callback is invoked each time data in subtree changes.
     * 
     */
    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(
            InstanceIdentifier<? extends DataObject> path, DataChangeListener listener);
}
