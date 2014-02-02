/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl

import javax.ws.rs.core.Response
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession
import org.opendaylight.controller.sal.core.api.data.DataBrokerService
import org.opendaylight.controller.sal.core.api.mount.MountInstance
import org.opendaylight.controller.sal.rest.impl.RestconfProvider
import org.opendaylight.controller.sal.streams.listeners.ListenerAdapter
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.slf4j.LoggerFactory

class BrokerFacade implements DataReader<InstanceIdentifier, CompositeNode> {


    val static LOG = LoggerFactory.getLogger(BrokerFacade)
    val static BrokerFacade INSTANCE = new BrokerFacade

    @Property
    private ConsumerSession context;

    @Property
    private DataBrokerService dataService;
    
    private new() {
        if (INSTANCE !== null) {
            throw new IllegalStateException("Already instantiated");
        }
    }

    def static BrokerFacade getInstance() {
        return INSTANCE
    }

    private def void checkPreconditions() {
        if (context === null || dataService === null) {
            throw new ResponseException(Response.Status.SERVICE_UNAVAILABLE, RestconfProvider::NOT_INITALIZED_MSG)
        }
    }

    override readConfigurationData(InstanceIdentifier path) {
        checkPreconditions
        LOG.trace("Read Configuration via Restconf: {}", path)
        return dataService.readConfigurationData(path);
    }
    
    def readConfigurationDataBehindMountPoint(MountInstance mountPoint, InstanceIdentifier path) {
        checkPreconditions
        LOG.trace("Read Configuration via Restconf: {}", path)
        return mountPoint.readConfigurationData(path);
    }

    override readOperationalData(InstanceIdentifier path) {
        checkPreconditions
        LOG.trace("Read Operational via Restconf: {}", path)
        return dataService.readOperationalData(path);
    }
    
    def readOperationalDataBehindMountPoint(MountInstance mountPoint, InstanceIdentifier path) {
        checkPreconditions
        LOG.trace("Read Operational via Restconf: {}", path)
        return mountPoint.readOperationalData(path);
    }

    def RpcResult<CompositeNode> invokeRpc(QName type, CompositeNode payload) {
        checkPreconditions
        val future = context.rpc(type, payload);
        return future.get;
    }

    def commitConfigurationDataPut(InstanceIdentifier path, CompositeNode payload) {
        checkPreconditions
        val transaction = dataService.beginTransaction;
        LOG.trace("Put Configuration via Restconf: {}", path)
        transaction.putConfigurationData(path, payload);
        return transaction.commit
    }
    
    def commitConfigurationDataPutBehindMountPoint(MountInstance mountPoint, InstanceIdentifier path, CompositeNode payload) {
        checkPreconditions
        val transaction = mountPoint.beginTransaction;
        LOG.trace("Put Configuration via Restconf: {}", path)
        transaction.putConfigurationData(path, payload);
        return transaction.commit
    }

    def commitConfigurationDataPost(InstanceIdentifier path, CompositeNode payload) {
        checkPreconditions
        val transaction = dataService.beginTransaction;
        transaction.putConfigurationData(path, payload);
        if (payload == transaction.createdConfigurationData.get(path)) {
            LOG.trace("Post Configuration via Restconf: {}", path)
            return transaction.commit
        }
        LOG.trace("Post Configuration via Restconf was not executed because data already exists: {}", path)
        return null;
    }
    
    def commitConfigurationDataPostBehindMountPoint(MountInstance mountPoint, InstanceIdentifier path, CompositeNode payload) {
        checkPreconditions
        val transaction = mountPoint.beginTransaction;
        transaction.putConfigurationData(path, payload);
        if (payload == transaction.createdConfigurationData.get(path)) {
            LOG.trace("Post Configuration via Restconf: {}", path)
            return transaction.commit
        }
        LOG.trace("Post Configuration via Restconf was not executed because data already exists: {}", path)
        return null;
    }

    def commitConfigurationDataDelete(InstanceIdentifier path) {
        checkPreconditions
        val transaction = dataService.beginTransaction;
        LOG.info("Delete Configuration via Restconf: {}", path)
        transaction.removeConfigurationData(path)
        return transaction.commit
    }
    
    def commitConfigurationDataDeleteBehindMountPoint(MountInstance mountPoint, InstanceIdentifier path) {
        checkPreconditions
        val transaction = mountPoint.beginTransaction;
        LOG.info("Delete Configuration via Restconf: {}", path)
        transaction.removeConfigurationData(path)
        return transaction.commit
    }

    def registerToListenDataChanges(ListenerAdapter listener) {
        checkPreconditions
        if (listener.listening) {
            return;
        }
        val registration = dataService.registerDataChangeListener(listener.path, listener)
        listener.setRegistration(registration)
    }

}
