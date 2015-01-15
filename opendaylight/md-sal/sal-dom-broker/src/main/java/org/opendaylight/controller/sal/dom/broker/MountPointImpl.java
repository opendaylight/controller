/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker;

import org.opendaylight.controller.md.sal.dom.broker.compat.hydrogen.DataBrokerImpl;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
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
import org.opendaylight.controller.sal.dom.broker.spi.NotificationRouter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextProvider;

@Deprecated
public class MountPointImpl implements MountProvisionInstance, SchemaContextProvider {

    private final SchemaAwareRpcBroker rpcs;
    private final DataBrokerImpl dataReader;
    private final NotificationRouter notificationRouter;
    private final DataReader<YangInstanceIdentifier,CompositeNode> readWrapper;


    private final YangInstanceIdentifier mountPath;

    private SchemaContext schemaContext;

    public MountPointImpl(final YangInstanceIdentifier path) {
        this.mountPath = path;
        rpcs = new SchemaAwareRpcBroker(path.toString(),this);
        dataReader = new DataBrokerImpl();
        notificationRouter = new NotificationRouterImpl();
        readWrapper = new ReadWrapper();
    }

    public YangInstanceIdentifier getMountPath() {
        return mountPath;
    }

    public DataReader<YangInstanceIdentifier, CompositeNode> getReadWrapper() {
        return readWrapper;
    }

    @Override
    public void publish(final CompositeNode notification) {
        notificationRouter.publish(notification);
    }

    @Override
    public ListenerRegistration<NotificationListener> addNotificationListener(final QName notification, final NotificationListener listener) {
        return notificationRouter.addNotificationListener(notification, listener);
    }

    @Override
    public CompositeNode readConfigurationData(final YangInstanceIdentifier path) {
        return dataReader.readConfigurationData(path);
    }

    @Override
    public CompositeNode readOperationalData(final YangInstanceIdentifier path) {
        return dataReader.readOperationalData(path);
    }

    @Override
    public Registration registerOperationalReader(
            final YangInstanceIdentifier path, final DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        return dataReader.registerOperationalReader(path, reader);
    }

    @Override
    public Registration registerConfigurationReader(
            final YangInstanceIdentifier path, final DataReader<YangInstanceIdentifier, CompositeNode> reader) {
        return dataReader.registerConfigurationReader(path, reader);
    }

    @Override
    public RoutedRpcRegistration addRoutedRpcImplementation(final QName rpcType, final RpcImplementation implementation) {
        return rpcs.addRoutedRpcImplementation(rpcType, implementation);
    }

    @Override
    public void setRoutedRpcDefaultDelegate(final RoutedRpcDefaultImplementation defaultImplementation) {
        rpcs.setRoutedRpcDefaultDelegate(defaultImplementation);
    }

    @Override
    public RpcRegistration addRpcImplementation(final QName rpcType, final RpcImplementation implementation)
            throws IllegalArgumentException {
        return rpcs.addRpcImplementation(rpcType, implementation);
    }

    @Override
    public Set<QName> getSupportedRpcs() {
        return rpcs.getSupportedRpcs();
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> invokeRpc(final QName rpc, final CompositeNode input) {
        return rpcs.invokeRpc(rpc, input);
    }

    @Override
    public ListenerRegistration<RpcRegistrationListener> addRpcRegistrationListener(final RpcRegistrationListener listener) {
        return rpcs.addRpcRegistrationListener(listener);
    }

    @Override
    public ListenableFuture<RpcResult<CompositeNode>> rpc(final QName type, final CompositeNode input) {
        return rpcs.invokeRpc( type, input );
    }

    @Override
    public DataModificationTransaction beginTransaction() {
        return dataReader.beginTransaction();
    }

    @Override
    public ListenerRegistration<DataChangeListener> registerDataChangeListener(final YangInstanceIdentifier path,
            final DataChangeListener listener) {
        return dataReader.registerDataChangeListener(path, listener);
    }

    @Override
    public Registration registerCommitHandler(
            final YangInstanceIdentifier path, final DataCommitHandler<YangInstanceIdentifier, CompositeNode> commitHandler) {
        return dataReader.registerCommitHandler(path, commitHandler);
    }

    @Override
    public void removeRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        // NOOP
    }

    @Override
    public void addRefresher(final DataStoreIdentifier store, final DataRefresher refresher) {
        // NOOP
    }

    @Override
    public void addValidator(final DataStoreIdentifier store, final DataValidator validator) {
        // NOOP
    }
    @Override
    public void removeValidator(final DataStoreIdentifier store, final DataValidator validator) {
        // NOOP
    }

    @Override
    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    @Override
    public void setSchemaContext(final SchemaContext schemaContext) {
        this.schemaContext = schemaContext;
    }

    class ReadWrapper implements DataReader<YangInstanceIdentifier, CompositeNode> {
        private YangInstanceIdentifier shortenPath(final YangInstanceIdentifier path) {
            if (!mountPath.contains(path)) {
                return null;
            }

            return YangInstanceIdentifier.create(Iterables.skip(path.getPathArguments(), Iterables.size(mountPath.getPathArguments())));
        }

        @Override
        public CompositeNode readConfigurationData(final YangInstanceIdentifier path) {
            YangInstanceIdentifier newPath = shortenPath(path);
            if(newPath == null) {
                return null;
            }
            return MountPointImpl.this.readConfigurationData(newPath);
        }

        @Override
        public CompositeNode readOperationalData(final YangInstanceIdentifier path) {
            YangInstanceIdentifier newPath = shortenPath(path);
            if(newPath == null) {
                return null;
            }
            return MountPointImpl.this.readOperationalData(newPath);
        }
    }

    @Override
    public ListenerRegistration<RegistrationListener<DataCommitHandlerRegistration<YangInstanceIdentifier, CompositeNode>>> registerCommitHandlerListener(
            final RegistrationListener<DataCommitHandlerRegistration<YangInstanceIdentifier, CompositeNode>> commitHandlerListener) {
        return dataReader.registerCommitHandlerListener(commitHandlerListener);
    }

    @Override
    public <L extends RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>> ListenerRegistration<L> registerRouteChangeListener(
            final L listener) {
        return rpcs.registerRouteChangeListener(listener);
    }
}
