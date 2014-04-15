/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.group;

import org.opendaylight.controller.frm.AbstractChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.RemoveGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.UpdateGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.OriginalGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.group.update.UpdatedGroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class GroupChangeListener extends AbstractChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(GroupChangeListener.class);

    private final SalGroupService salGroupService;

    public SalGroupService getSalGroupService() {
        return this.salGroupService;
    }
    
    public GroupChangeListener(final SalGroupService manager) {
        this.salGroupService = manager;
    }

    @Override
    protected void validate() throws IllegalStateException {
        GroupTransactionValidator.validate(this);
    }

    @Override
    protected void remove(InstanceIdentifier<? extends DataObject> identifier, DataObject removeDataObj) {
        if ((removeDataObj instanceof Group)) {
            
            final Group group = ((Group) removeDataObj);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final RemoveGroupInputBuilder builder = new RemoveGroupInputBuilder(group);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setGroupRef(new GroupRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            this.salGroupService.removeGroup((RemoveGroupInput) builder.build());
            LOG.debug("Transaction {} - Remove Group has removed group: {}", new Object[]{uri, removeDataObj});
        }
    }

    @Override
    protected void update(InstanceIdentifier<? extends DataObject> identifier, DataObject original, DataObject update) {
        if (original instanceof Group && update instanceof Group) {
            
            final Group originalGroup = ((Group) original);
            final Group updatedGroup = ((Group) update);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final UpdateGroupInputBuilder builder = new UpdateGroupInputBuilder();
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setGroupRef(new GroupRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            
            builder.setUpdatedGroup((UpdatedGroup) (new UpdatedGroupBuilder(updatedGroup)).build());
            builder.setOriginalGroup((OriginalGroup) (new OriginalGroupBuilder(originalGroup)).build());
            
            this.salGroupService.updateGroup((UpdateGroupInput) builder.build());
            LOG.debug("Transaction {} - Update Group has updated group {} with group {}", new Object[]{uri, original, update});
        }
    }

    @Override
    protected void add(InstanceIdentifier<? extends DataObject> identifier, DataObject addDataObj) {
        if ((addDataObj instanceof Group)) {
            final Group group = ((Group) addDataObj);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final AddGroupInputBuilder builder = new AddGroupInputBuilder(group);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setGroupRef(new GroupRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            this.salGroupService.addGroup((AddGroupInput) builder.build());
            LOG.debug("Transaction {} - Add Group has added group: {}", new Object[]{uri, addDataObj});
        }
    }
}
