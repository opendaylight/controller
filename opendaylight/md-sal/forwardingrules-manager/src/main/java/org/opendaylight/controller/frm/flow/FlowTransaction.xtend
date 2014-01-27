/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.flow

import org.opendaylight.controller.frm.AbstractTransaction
import org.opendaylight.controller.md.sal.common.api.data.DataModification
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.Flow
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri

class FlowTransaction extends AbstractTransaction {
    
    @Property
    val SalFlowService salFlowService;   
    
    
    new(DataModification<InstanceIdentifier<? extends DataObject>, DataObject> modification,SalFlowService salFlowService) {
        super(modification)        
        _salFlowService = salFlowService;
    }
    
    override remove(InstanceIdentifier<?> instanceId, DataObject obj) {
        if(obj instanceof Flow) {
            val flow = (obj as Flow)
            val tableInstanceId = instanceId.firstIdentifierOf(Table);
            val nodeInstanceId = instanceId.firstIdentifierOf(Node);
            val builder = new RemoveFlowInputBuilder(flow);
            builder.setFlowRef(new FlowRef(instanceId));            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            builder.setTransactionUri(new Uri(modification.getIdentifier() as String));
            _salFlowService.removeFlow(builder.build());            
        }
    }
    
    override update(InstanceIdentifier<?> instanceId, DataObject originalObj, DataObject updatedObj) {
        if(originalObj instanceof Flow && updatedObj instanceof Flow) {
            val originalFlow = (originalObj as Flow)
            val updatedFlow = (updatedObj as Flow)
            val nodeInstanceId = instanceId.firstIdentifierOf(Node);
            val builder = new UpdateFlowInputBuilder();
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(instanceId));
            val ufb = new UpdatedFlowBuilder(updatedFlow);
            builder.setUpdatedFlow((ufb.build()));
            builder.setTransactionUri(new Uri(modification.getIdentifier() as String));
            val ofb = new OriginalFlowBuilder(originalFlow);
            builder.setOriginalFlow(ofb.build());      
            _salFlowService.updateFlow(builder.build());
           
        }
    }
    
    override add(InstanceIdentifier<?> instanceId, DataObject obj) {
        if(obj instanceof Flow) {
            val flow = (obj as Flow)
            val tableInstanceId = instanceId.firstIdentifierOf(Table);
            val nodeInstanceId = instanceId.firstIdentifierOf(Node);
            val builder = new AddFlowInputBuilder(flow);
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setTransactionUri(new Uri(modification.getIdentifier() as String));
            builder.setFlowRef(new FlowRef(instanceId));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            _salFlowService.addFlow(builder.build());            
        }
    }
    
    override validate() throws IllegalStateException {
        FlowTransactionValidator.validate(this)
    }  
}
