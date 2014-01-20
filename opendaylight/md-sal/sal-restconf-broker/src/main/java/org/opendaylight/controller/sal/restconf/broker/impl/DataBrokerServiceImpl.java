/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.broker.impl;

import com.google.common.base.Optional;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.restconf.broker.listeners.RemoteDataChangeNotificationListener;
import org.opendaylight.controller.sal.restconf.broker.tools.RemoteStreamTools;
import org.opendaylight.controller.sal.restconf.broker.transactions.RemoteDataModificationTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.BeginTransactionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateDataChangeEventSubscriptionInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.CreateDataChangeEventSubscriptionOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.remote.rev140114.SalRemoteService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.restconf.client.api.RestconfClientContext;
import org.opendaylight.yangtools.restconf.client.api.event.EventStreamInfo;
import org.opendaylight.yangtools.restconf.client.api.event.ListenableEventStreamContext;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataBrokerServiceImpl implements DataBrokerService  {

    private static final Logger logger = LoggerFactory.getLogger(DataBrokerServiceImpl.class.toString());
    private RestconfClientContext restconfClientContext;
    private SalRemoteService salRemoteService;

    public DataBrokerServiceImpl(RestconfClientContext restconfClientContext) {
        this.restconfClientContext = restconfClientContext;
        this.salRemoteService =  this.restconfClientContext.getRpcServiceContext(SalRemoteService.class).getRpcService();
    }
    @Override
    public <T extends DataRoot> T getData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public <T extends DataRoot> T getData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, Class<T> rootType) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public <T extends DataRoot> T getCandidateData(DataStoreIdentifier store, T filter) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public RpcResult<DataRoot> editCandidateData(DataStoreIdentifier store, DataRoot changeSet) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public Future<RpcResult<Void>> commit(DataStoreIdentifier store) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public DataObject getData(InstanceIdentifier<? extends DataObject> data) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public DataObject getConfigurationData(InstanceIdentifier<?> data) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        Future<RpcResult<BeginTransactionOutput>> rpcResultFuture = this.salRemoteService.beginTransaction();
        //TODO finish yang model for proper remoteDataModificationTransaction setup
        RemoteDataModificationTransaction remoteDataModificationTransaction = new RemoteDataModificationTransaction();
        return remoteDataModificationTransaction;
    }

    @Override
    public void registerChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public void unregisterChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener changeListener) {
        throw new UnsupportedOperationException("Deprecated");
    }

    @Override
    public DataObject readConfigurationData(InstanceIdentifier<? extends DataObject> path) {
        try {
            Optional<DataObject> optDataObject = (Optional<DataObject>) this.restconfClientContext.getConfigurationDatastore().readData(path).get();
            if (optDataObject.isPresent()){
                return optDataObject.get();
            }
        } catch (InterruptedException e) {
            logger.trace("Reading configuration data interrupted {}",e);
        } catch (ExecutionException e) {
            logger.trace("Reading configuration execution exception {}",e);
        }
        throw new IllegalStateException("No data to return.");
    }

    @Override
    public DataObject readOperationalData(InstanceIdentifier<? extends DataObject> path) {
        try {
            Optional<DataObject> optDataObject = (Optional<DataObject>) this.restconfClientContext.getOperationalDatastore().readData(path).get();
            if (optDataObject.isPresent()){
                return optDataObject.get();
            }
        } catch (InterruptedException e) {
            logger.trace("Reading configuration data interrupted {}",e);
        } catch (ExecutionException e) {
            logger.trace("Reading configuration execution exception {}",e);
        }
        throw new IllegalStateException("No data to return.");
    }
    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(InstanceIdentifier<? extends DataObject> path, DataChangeListener listener) {
        CreateDataChangeEventSubscriptionInputBuilder inputBuilder = new CreateDataChangeEventSubscriptionInputBuilder();
        Future<RpcResult<CreateDataChangeEventSubscriptionOutput>> rpcResultFuture =  salRemoteService.createDataChangeEventSubscription(inputBuilder.setPath(path).build());
        String streamName = "";
        try {
            if (rpcResultFuture.get().isSuccessful()){
                streamName = rpcResultFuture.get().getResult().getStreamName();
            }
        } catch (InterruptedException e) {
            logger.trace("Interupted while getting rpc result due to {}",e);
        } catch (ExecutionException e) {
            logger.trace("Execution exception while getting rpc result due to {}",e);
        }
        final Map<String,EventStreamInfo> desiredEventStream = RemoteStreamTools.createEventStream(restconfClientContext,streamName);
        ListenableEventStreamContext restConfListenableEventStreamContext = restconfClientContext.getEventStreamContext(desiredEventStream.get(streamName));
        RemoteDataChangeNotificationListener remoteDataChangeNotificationListener = new RemoteDataChangeNotificationListener(listener);
        restConfListenableEventStreamContext.registerNotificationListener(remoteDataChangeNotificationListener);
        return new SalRemoteDataListenerRegistration(listener);
    }

    private class SalRemoteDataListenerRegistration implements ListenerRegistration<DataChangeListener> {
        private DataChangeListener dataChangeListener;
        public SalRemoteDataListenerRegistration(DataChangeListener dataChangeListener){
            this.dataChangeListener = dataChangeListener;
        }
        @Override
        public DataChangeListener getInstance() {
            return this.dataChangeListener;
        }
        @Override
        public void close() throws Exception {
            //noop
        }
    }
}
