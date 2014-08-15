/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.group;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcConsumerRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Group Provider registers the {@link GroupChangeListener} and it holds all needed
 * services for {@link GroupChangeListener}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class GroupProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(GroupProvider.class);

    private SalGroupService salGroupService;
    private DataBroker dataService;

    /* DataChangeListener */
    private DataChangeListener groupDataChangeListener;
    private ListenerRegistration<DataChangeListener> groupDataChangeListenerRegistration;

    /**
     * Provider Initialization Phase.
     *
     * @param DataProviderService dataService
     */
    public void init (final DataBroker dataService) {
        LOG.info("FRM Group Config Provider initialization.");
        this.dataService = Preconditions.checkNotNull(dataService, "DataService can not be null !");
    }

    /**
     * Listener Registration Phase
     *
     * @param RpcConsumerRegistry rpcRegistry
     */
    public void start(final RpcConsumerRegistry rpcRegistry) {
        Preconditions.checkArgument(rpcRegistry != null, "RpcConsumerRegistry can not be null !");

        this.salGroupService = Preconditions.checkNotNull(rpcRegistry.getRpcService(SalGroupService.class),
                "RPC SalGroupService not found.");

        /* Build Path */
        InstanceIdentifier<Group> groupIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).child(Group.class);

        /* DataChangeListener registration */
        this.groupDataChangeListener = new GroupChangeListener(GroupProvider.this);
        this.groupDataChangeListenerRegistration = this.dataService.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, groupIdentifier, groupDataChangeListener, DataChangeScope.SUBTREE);

        LOG.info("FRM Group Config Provider started.");
    }

    @Override
    public void close() {
        LOG.info("FRM Group Config Provider stopped.");
        if (groupDataChangeListenerRegistration != null) {
            try {
                groupDataChangeListenerRegistration.close();
            } catch (Exception e) {
                String errMsg = "Error by stop FRM Group Config Provider.";
                LOG.error(errMsg, e);
                throw new IllegalStateException(errMsg, e);
            } finally {
                groupDataChangeListenerRegistration = null;
            }
        }
    }

    public DataChangeListener getGroupDataChangeListener() {
        return groupDataChangeListener;
    }

    public SalGroupService getSalGroupService() {
        return salGroupService;
    }

    public DataBroker getDataService() {
        return dataService;
    }
}
