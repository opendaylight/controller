/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.group;

import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.controller.sal.binding.api.data.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.data.DataModificationTransaction;
import org.opendaylight.controller.sal.binding.api.data.DataProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GroupProvider.class);

    private SalGroupService salGroupService;
    private DataProviderService dataService;

    public void init (final DataProviderService dataService) {
        LOG.info("FRM Group Config Provider initialization.");
        if (dataService == null) {
            throw new IllegalArgumentException("DataService can not be null !");
        }
        this.dataService = dataService;
    }

    /* DataChangeListener */
    private GroupChangeListener groupDataChangeListener;
    ListenerRegistration<DataChangeListener> groupDataChangeListenerRegistration;

    public void start(final RpcConsumerRegistry rpcRegistry) {
        if (rpcRegistry == null) {
            throw new IllegalArgumentException("RpcConsumerRegistry can not be null !");
        }
        if (rpcRegistry.getRpcService(SalGroupService.class) == null) {
            throw new IllegalStateException("RPC SalGroupService not found.");
        }

        this.salGroupService = rpcRegistry.getRpcService(SalGroupService.class);
        /* Build Path */
        InstanceIdentifierBuilder<Nodes> nodesBuilder = InstanceIdentifier.builder(Nodes.class);
        InstanceIdentifierBuilder<Node> nodeChild = nodesBuilder.child(Node.class);
        InstanceIdentifierBuilder<FlowCapableNode> augmentFlowCapNode =
                nodeChild.augmentation(FlowCapableNode.class);
        InstanceIdentifierBuilder<Group> groupChild = augmentFlowCapNode.child(Group.class);
        final InstanceIdentifier<? extends DataObject> groupDataObjectPath = groupChild.toInstance();

        /* DataChangeListener registration */
        this.groupDataChangeListener = new GroupChangeListener(GroupProvider.this);
        this.groupDataChangeListenerRegistration =
                this.dataService.registerDataChangeListener(groupDataObjectPath, groupDataChangeListener);
        LOG.info("FRM Group Config Provider started.");
    }

    protected DataModificationTransaction startChange() {
        return this.dataService.beginTransaction();
    }

    @Override
    public void close() {
        LOG.info("FRM Group Config Provider stopped.");
        if(groupDataChangeListenerRegistration != null){
            try {
                groupDataChangeListenerRegistration.close();
            }
            catch (Exception e) {
                String errMsg = "Error by stop FRM Group Config Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            }
        }
    }

    public SalGroupService getSalGroupService() {
        return salGroupService;
    }

    public DataProviderService getDataService() {
        return dataService;
    }
}
