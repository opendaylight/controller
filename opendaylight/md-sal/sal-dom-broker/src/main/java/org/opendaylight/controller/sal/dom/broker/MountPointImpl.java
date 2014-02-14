/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.RegistrationListener;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandlerRegistration;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.controller.sal.core.api.Broker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.core.api.Broker.RpcRegistration;
import org.opendaylight.controller.sal.core.api.RoutedRpcDefaultImplementation;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.controller.sal.core.api.RpcRegistrationListener;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.data.DataValidator;
import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.dom.broker.impl.NotificationRouterImpl;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaAwareRpcBroker;
import org.opendaylight.controller.sal.dom.broker.impl.SchemaContextProvider;
import org.opendaylight.controller.sal.dom.broker.spi.NotificationRouter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class MountPointImpl implements MountProvisionInstance, SchemaContextProvider {

    private final SchemaAwareRpcBroker rpcs;
    private final DataBrokerImpl dataReader;
    private final NotificationRouter notificationRouter;
    private final DataReader<InstanceIdentifier,CompositeNode> readWrapper;
    
    
    private final InstanceIdentifier mountPath;

    private SchemaContext schemaContext;

    public MountPointImpl(InstanceIdentifier path) {
        this.mountPath = path;
        rpcs = new SchemaAwareRpcBroker(path.toString(),this);
        dataReader = new DataBrokerImpl();
        notificationRouter = new NotificationRouterImpl();
        readWrapper = new ReadWrapper();
    }

    public InstanceIdentifier getMountPath() {
        return mountPath;
    }

    public DataReader<InstanceIdentifier, CompositeNode> getReadWrapper() {
        return readWrapper;
    }

    @Override
    public void publish(CompositeNode notification) {
        notificationRouter.publish(notification);
    }

    @Override
    public Registration<NotificationListener> addNotificationListener(QName notification, NotificationListener listener) {
        return notificationRouter.addNotificationListener(notification, listener);
    }

    @Override
    public CompositeNode readConfigurationData(InstanceIdentifier path) {
        return dataReader.readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(InstanceIdentifier path) {
        return dataReader.readOperationalData(path);
    }

    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerOperationalReader(
            InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        return dataReader.registerOperationalReader(path, reader);
    }

    public Registration<DataReader<InstanceIdentifier, CompositeNode>> registerConfigurationReader(
            InstanceIdentifier path, DataReader<InstanceIdentifier, CompositeNode> reader) {
        return dataReader.registerConfigurationReader(path, reader);
    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(QName rpcType, RpcImplementation implementation) {
        return rpcs.addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public void setRoutedRpcDefaultDelegate(RoutedRpcDefaultImplementation defaultImplementation) {
      rpcs.setRoutedRpcDefaultDelegate(defaultImplementation);
    }

  @Override
    public RpcRegistration addRpcImplementation(QName rpcType, RpcImplementation implementation)
            throws IllegalArgumentException {
        return rpcs.addRpcImplementation(rpcType, implementation);
    }

    public Set<QName> getSupportedRpcs() {
        return rpcs.getSupportedRpcs();
    }

    
    public RpcResult<CompositeNode> invokeRpc(QName rpc, CompositeNode input) {
        return rpcs.invokeRpc(rpc, input);
    }

    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(RpcRegistrationListener listener) {
        return rpcs.addRpcRegistrationListener(listener);
    }


    @Override
    public Future<RpcResult<CompositeNode>> rpc(QName type, CompositeNode input) {
        return null;
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return dataReader.beginTransaction();
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(InstanceIdentifier path,
            DataChangeListener listener) {
        return dataReader.registerDataChangeListener(path, listener);
    }

    @Override
    public void sendNotification(CompositeNode notification) {
        publish(notification);
    }
    
    @Override
    public Registration<DataCommitHandler<InstanceIdentifier, CompositeNode>> registerCommitHandler(
            InstanceIdentifier path, DataCommitHandler<InstanceIdentifier, CompositeNode> commitHandler) {
        return dataReader.registerCommitHandler(path, commitHandler);
    }
    
    @Override
    public void removeRefresher(DataStoreIdentifier store, DataRefresher refresher) {
     // NOOP
    }
    
    @Override
    public void addRefresher(DataStoreIdentifier store, DataRefresher refresher) {
     // NOOP
    }
    
    @Override
    public void addValidator(DataStoreIdentifier store, DataValidator validator) {
     // NOOP
    }
    @Override
    public void removeValidator(DataStoreIdentifier store, DataValidator validator) {
        // NOOP
    }
    
    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    public void setSchemaContext(SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    class ReadWrapper implements DataReader<InstanceIdentifier, CompositeNode> {
        
        
        private InstanceIdentifier shortenPath(InstanceIdentifier path) {
            InstanceIdentifier ret = null;
            if(mountPath.contains(path)) {
                List<PathArgument> newArgs = path.getPath().subList(mountPath.getPath().size(), path.getPath().size());
                ret = new InstanceIdentifier(newArgs);
            }
            return ret;
        }
        
        @Override
        public CompositeNode readConfigurationData(InstanceIdentifier path) {
            InstanceIdentifier newPath = shortenPath(path);
            if(newPath == null) {
                return null;
            }
            return MountPointImpl.this.readConfigurationData(newPath);
        }
        
        @Override
        public CompositeNode readOperationalData(InstanceIdentifier path) {
            InstanceIdentifier newPath = shortenPath(path);
            if(newPath == null) {
                return null;
            }
            return MountPointImpl.this.readOperationalData(newPath);
        }
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            RegistrationListener<DataCommitHandlerRegistration<InstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return dataReader.registerCommitHandlerListener(commitHandlerListener);
    }

    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, InstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            L listener) {
        return rpcs.registerRouteChangeListener(listener);
    }


}
