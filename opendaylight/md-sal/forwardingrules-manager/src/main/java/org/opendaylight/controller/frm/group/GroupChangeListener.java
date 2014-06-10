/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.group;

import org.opendaylight.controller.frm.AbstractChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Group Change Listener
 *  add, update and remove {@link Group} processing from {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class GroupChangeListener extends AbstractChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(GroupChangeListener.class);

    private final GroupProvider provider;

    public GroupChangeListener(final GroupProvider provider) {
        this.provider = Preconditions.checkNotNull(provider, "GroupProvider can not be null !");
    }

    @Override
    protected void remove(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject removeDataObj) {

        final Group group = ((Group) removeDataObj);
        final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
        final RemoveGroupInputBuilder builder = new RemoveGroupInputBuilder(group);

        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setGroupRef(new GroupRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalGroupService().removeGroup(builder.build());
        LOG.debug("Transaction {} - Remove Group has removed group: {}", new Object[]{uri, removeDataObj});
    }

    @Override
    protected void update(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject original, final DataObject update) {

        final Group originalGroup = ((Group) original);
        final Group updatedGroup = ((Group) update);
        final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
        final UpdateGroupInputBuilder builder = new UpdateGroupInputBuilder();

        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setGroupRef(new GroupRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);

        builder.setUpdatedGroup((new UpdatedGroupBuilder(updatedGroup)).build());
        builder.setOriginalGroup((new OriginalGroupBuilder(originalGroup)).build());

        this.provider.getSalGroupService().updateGroup(builder.build());
        LOG.debug("Transaction {} - Update Group has updated group {} with group {}", new Object[]{uri, original, update});
    }

    @Override
    protected void add(final InstanceIdentifier<? extends DataObject> identifier,
                       final DataObject addDataObj) {

        final Group group = ((Group) addDataObj);
        final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
        final AddGroupInputBuilder builder = new AddGroupInputBuilder(group);

        builder.setNode(new NodeRef(nodeInstanceId));
        builder.setGroupRef(new GroupRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalGroupService().addGroup(builder.build());
        LOG.debug("Transaction {} - Add Group has added group: {}", new Object[]{uri, addDataObj});
    }

    @Override
    protected boolean preconditionForChange(final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject dataObj, final DataObject update) {

        final ReadOnlyTransaction trans = this.provider.getDataService().newReadOnlyTransaction();
        return update != null
                ? (dataObj instanceof Group && update instanceof Group && isNodeAvaliable(identifier, trans))
                : (dataObj instanceof Group && isNodeAvaliable(identifier, trans));
    }
}
