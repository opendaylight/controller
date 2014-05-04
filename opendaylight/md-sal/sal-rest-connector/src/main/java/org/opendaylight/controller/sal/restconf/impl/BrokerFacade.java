/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl;

import com.google.common.base.Objects;

import java.util.Map;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response.Status;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataReader;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.api.data.DataChangeListener;
import org.opendaylight.controller.sal.core.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.core.api.mount.MountInstance;
import org.opendaylight.controller.sal.rest.impl.RestconfProvider;
import org.opendaylight.controller.sal.restconf.impl.ResponseException;
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerFacade implements DataReader<InstanceIdentifier, CompositeNode> {
    private final static Logger LOG = LoggerFactory.getLogger( BrokerFacade.class );

    private final static BrokerFacade INSTANCE = new BrokerFacade();

    private volatile DataBrokerService dataService;
    private volatile ConsumerSession context;

    private BrokerFacade() {
    }

    public void setContext( final ConsumerSession context ) {
        this.context = context;
    }

    public void setDataService( final DataBrokerService dataService ) {
        this.dataService = dataService;
    }

    public static BrokerFacade getInstance() {
        return BrokerFacade.INSTANCE;
    }

    private void checkPreconditions() {
        if( context == null || dataService == null ) {
            ResponseException _responseException = new ResponseException( Status.SERVICE_UNAVAILABLE,
                    RestconfProvider.NOT_INITALIZED_MSG );
            throw _responseException;
        }
    }

    @Override
    public CompositeNode readConfigurationData( final InstanceIdentifier path ) {
        this.checkPreconditions();

        LOG.trace( "Read Configuration via Restconf: {}", path );

        return dataService.readConfigurationData( path );
    }

    public CompositeNode readConfigurationDataBehindMountPoint( final MountInstance mountPoint,
                                                                final InstanceIdentifier path ) {
        this.checkPreconditions();

        LOG.trace( "Read Configuration via Restconf: {}", path );

        return mountPoint.readConfigurationData( path );
    }

    @Override
    public CompositeNode readOperationalData( final InstanceIdentifier path ) {
        this.checkPreconditions();

        BrokerFacade.LOG.trace( "Read Operational via Restconf: {}", path );

        return dataService.readOperationalData( path );
    }

    public CompositeNode readOperationalDataBehindMountPoint( final MountInstance mountPoint,
                                                              final InstanceIdentifier path ) {
        this.checkPreconditions();

        BrokerFacade.LOG.trace( "Read Operational via Restconf: {}", path );

        return mountPoint.readOperationalData( path );
    }

    public RpcResult<CompositeNode> invokeRpc( final QName type, final CompositeNode payload ) {
        this.checkPreconditions();

        final Future<RpcResult<CompositeNode>> future = context.rpc( type, payload );

        try {
            return future.get();
        }
        catch( Exception e ) {
            throw new ResponseException( e, "Error invoking RPC " + type );
        }
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPut( final InstanceIdentifier path,
                                                                            final CompositeNode payload ) {
        this.checkPreconditions();

        final DataModificationTransaction transaction = dataService.beginTransaction();
        BrokerFacade.LOG.trace( "Put Configuration via Restconf: {}", path );
        transaction.putConfigurationData( path, payload );
        return transaction.commit();
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPutBehindMountPoint(
            final MountInstance mountPoint, final InstanceIdentifier path, final CompositeNode payload ) {
        this.checkPreconditions();

        final DataModificationTransaction transaction = mountPoint.beginTransaction();
        BrokerFacade.LOG.trace( "Put Configuration via Restconf: {}", path );
        transaction.putConfigurationData( path, payload );
        return transaction.commit();
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPost( final InstanceIdentifier path,
                                                                             final CompositeNode payload ) {
        this.checkPreconditions();

        final DataModificationTransaction transaction = dataService.beginTransaction();
        transaction.putConfigurationData( path, payload );
        Map<InstanceIdentifier, CompositeNode> createdConfigurationData =
                                                           transaction.getCreatedConfigurationData();
        CompositeNode createdNode = createdConfigurationData.get( path );

        if( Objects.equal( payload, createdNode ) ) {
            LOG.trace( "Post Configuration via Restconf: {}", path );
            return transaction.commit();
        }

        LOG.trace( "Post Configuration via Restconf was not executed because data already exists: {}",
                   path );
        return null;
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataPostBehindMountPoint(
            final MountInstance mountPoint, final InstanceIdentifier path, final CompositeNode payload ) {
        this.checkPreconditions();

        final DataModificationTransaction transaction = mountPoint.beginTransaction();
        transaction.putConfigurationData( path, payload );
        Map<InstanceIdentifier, CompositeNode> createdConfigurationData =
                                                               transaction.getCreatedConfigurationData();
        CompositeNode createdNode = createdConfigurationData.get( path );

        if( Objects.equal( payload, createdNode ) ) {
            LOG.trace( "Post Configuration via Restconf: {}", path );
            return transaction.commit();
        }

        LOG.trace( "Post Configuration via Restconf was not executed because data already exists: {}",
                    path );
        return null;
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataDelete( final InstanceIdentifier path ) {
        this.checkPreconditions();

        final DataModificationTransaction transaction = dataService.beginTransaction();
        LOG.info( "Delete Configuration via Restconf: {}", path );
        transaction.removeConfigurationData( path );
        return transaction.commit();
    }

    public Future<RpcResult<TransactionStatus>> commitConfigurationDataDeleteBehindMountPoint(
                                          final MountInstance mountPoint, final InstanceIdentifier path ) {
        this.checkPreconditions();

        final DataModificationTransaction transaction = mountPoint.beginTransaction();
        LOG.info( "Delete Configuration via Restconf: {}", path );
        transaction.removeConfigurationData( path );
        return transaction.commit();
    }

    public void registerToListenDataChanges( final ListenerAdapter listener ) {
        this.checkPreconditions();

        if( listener.isListening() ) {
            return;
        }

        InstanceIdentifier path = listener.getPath();
        final ListenerRegistration<DataChangeListener> registration =
                                             dataService.registerDataChangeListener( path, listener );

        listener.setRegistration( registration );
    }
}
