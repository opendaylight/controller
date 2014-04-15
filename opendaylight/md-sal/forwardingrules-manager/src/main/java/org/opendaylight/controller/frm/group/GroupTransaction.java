/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.group;

import org.opendaylight.controller.frm.AbstractTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
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

@Deprecated
/**
 * @deprecated please use {@link GroupDataChangeListener}
 */
public class GroupTransaction extends AbstractTransaction {

    private final SalGroupService groupService;

    public SalGroupService getGroupService() {
        return this.groupService;
    }

    @Deprecated
    /**
     * @deprecated please use {@link GroupDataChangeListener}
     */
    public GroupTransaction(
            final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,
            final SalGroupService groupService) {
        super(modification);
        this.groupService = groupService;
    }

    public void remove(final InstanceIdentifier<? extends Object> instanceId, final DataObject obj) {
        if ((obj instanceof Group)) {
            
            final Group group = ((Group) obj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final RemoveGroupInputBuilder builder = new RemoveGroupInputBuilder(group);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setGroupRef(new GroupRef(instanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            this.groupService.removeGroup((RemoveGroupInput) builder.build());
        }
    }

    public void update(final InstanceIdentifier<? extends Object> instanceId, final DataObject originalObj, final DataObject updatedObj) {
        if (originalObj instanceof Group && updatedObj instanceof Group) {
            
            final Group originalGroup = ((Group) originalObj);
            final Group updatedGroup = ((Group) updatedObj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final UpdateGroupInputBuilder builder = new UpdateGroupInputBuilder();
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setGroupRef(new GroupRef(instanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            
            builder.setUpdatedGroup((UpdatedGroup) (new UpdatedGroupBuilder(updatedGroup)).build());
            builder.setOriginalGroup((OriginalGroup) (new OriginalGroupBuilder(originalGroup)).build());
            
            this.groupService.updateGroup((UpdateGroupInput) builder.build());
        }
    }
    
    public void add(final InstanceIdentifier<? extends Object> instanceId, final DataObject obj) {
        if ((obj instanceof Group)) {
            final Group group = ((Group) obj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final AddGroupInputBuilder builder = new AddGroupInputBuilder(group);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setGroupRef(new GroupRef(instanceId));
            
            Uri _uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(_uri);
            this.groupService.addGroup((AddGroupInput) builder.build());
        }
    }
    
    public void validate() throws IllegalStateException {
        GroupTransactionValidator.validate(this);
    }
}
