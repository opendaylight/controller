/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GroupForwarder
 * It implements {@link org.opendaylight.controller.md.sal.binding.api.DataChangeListener}}
 * for WildCardedPath to {@link Group} and ForwardingRulesCommiter interface for methods:
 *  add, update and remove {@link Group} processing for
 *  {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class GroupForwarder extends AbstractListeningCommiter<Group> {

    private static final Logger LOG = LoggerFactory.getLogger(GroupForwarder.class);

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    public GroupForwarder (final ForwardingRulesManager manager, final DataBroker db) {
        super(manager, Group.class);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        this.listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                getWildCardPath(), GroupForwarder.this, DataChangeScope.SUBTREE);
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Error by stop FRM GroupChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    protected InstanceIdentifier<Group> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Group.class);
    }

    @Override
    public void remove(final InstanceIdentifier<Group> identifier, final Group removeDataObj,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final Group group = (removeDataObj);
        final RemoveGroupInputBuilder builder = new RemoveGroupInputBuilder(group);

        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setGroupRef(new GroupRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        this.provider.getSalGroupService().removeGroup(builder.build());
    }

    @Override
    public void update(final InstanceIdentifier<Group> identifier,
                       final Group original, final Group update,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final Group originalGroup = (original);
        final Group updatedGroup = (update);
        final UpdateGroupInputBuilder builder = new UpdateGroupInputBuilder();

        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setGroupRef(new GroupRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        builder.setUpdatedGroup((new UpdatedGroupBuilder(updatedGroup)).build());
        builder.setOriginalGroup((new OriginalGroupBuilder(originalGroup)).build());

        this.provider.getSalGroupService().updateGroup(builder.build());
    }

    @Override
    public void add(final InstanceIdentifier<Group> identifier, final Group addDataObj,
                    final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final Group group = (addDataObj);
        final AddGroupInputBuilder builder = new AddGroupInputBuilder(group);

        builder.setNode(new NodeRef(nodeIdent.firstIdentifierOf(Node.class)));
        builder.setGroupRef(new GroupRef(identifier));
        builder.setTransactionUri(new Uri(provider.getNewTransactionId()));
        this.provider.getSalGroupService().addGroup(builder.build());
    }
}

