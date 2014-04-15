/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.flow;

import org.opendaylight.controller.frm.AbstractTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@Deprecated
/**
 * @deprecated please use {@link FlowDataChangeListener}
 */
public class FlowTransaction extends AbstractTransaction {

    private final SalFlowService salFlowService;

    @Deprecated
    /**
     * @deprecated please use {@link FlowDataChangeListener}
     */
    public FlowTransaction(
            final DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,
            final SalFlowService salFlowService) {
        super(modification);
        this.salFlowService = salFlowService;
    }

    public void remove(final InstanceIdentifier<? extends Object> instanceId, final DataObject obj) {
        if ((obj instanceof Flow)) {
            
            final Flow flow = ((Flow) obj);
            final InstanceIdentifier<Table> tableInstanceId = instanceId.<Table> firstIdentifierOf(Table.class);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(flow);
            
            builder.setFlowRef(new FlowRef(instanceId));
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            this.salFlowService.removeFlow((RemoveFlowInput) builder.build());
        }
    }

    public void update(final InstanceIdentifier<? extends Object> instanceId, final DataObject originalObj, final DataObject updatedObj) {
        if (originalObj instanceof Flow && updatedObj instanceof Flow) {
            
            final Flow originalFlow = ((Flow) originalObj);
            final Flow updatedFlow = ((Flow) updatedObj);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node>firstIdentifierOf(Node.class);
            final UpdateFlowInputBuilder builder = new UpdateFlowInputBuilder();
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(instanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            
            builder.setUpdatedFlow((UpdatedFlow) (new UpdatedFlowBuilder(updatedFlow)).build());
            builder.setOriginalFlow((OriginalFlow) (new OriginalFlowBuilder(originalFlow)).build());
            
            this.salFlowService.updateFlow((UpdateFlowInput) builder.build());
      }
    }

    public void add(final InstanceIdentifier<? extends Object> instanceId, final DataObject obj) {
        if ((obj instanceof Flow)) {
            
            final Flow flow = ((Flow) obj);
            final InstanceIdentifier<Table> tableInstanceId = instanceId.<Table> firstIdentifierOf(Table.class);
            final InstanceIdentifier<Node> nodeInstanceId = instanceId.<Node> firstIdentifierOf(Node.class);
            final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(instanceId));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            
            Uri uri = new Uri(((String) this.getModification().getIdentifier()));
            builder.setTransactionUri(uri);
            this.salFlowService.addFlow((AddFlowInput) builder.build());
        }
    }
    
    public void validate() throws IllegalStateException {
        FlowTransactionValidator.validate(this);
    }
    
    public SalFlowService getSalFlowService() {
        return this.salFlowService;
    }
}
