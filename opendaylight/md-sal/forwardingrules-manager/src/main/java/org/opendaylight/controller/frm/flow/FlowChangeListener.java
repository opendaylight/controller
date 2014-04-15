/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.flow;

import org.opendaylight.controller.frm.AbstractChangeListener;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class FlowChangeListener extends AbstractChangeListener {

    private final static Logger LOG = LoggerFactory.getLogger(FlowChangeListener.class);

    private final SalFlowService salFlowService;

    public SalFlowService getSalFlowService() {
        return this.salFlowService;
    }
    
    public FlowChangeListener(final SalFlowService manager) {
        this.salFlowService = manager;
    }

    @Override
    protected void validate() throws IllegalStateException {
        FlowTransactionValidator.validate(this);
    }

    @Override
    protected void remove(InstanceIdentifier<? extends DataObject> identifier, DataObject removeDataObj) {
        if ((removeDataObj instanceof Flow)) {
            
            final Flow flow = ((Flow) removeDataObj);
            final InstanceIdentifier<Table> tableInstanceId = identifier.<Table> firstIdentifierOf(Table.class);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(flow);
            
            builder.setFlowRef(new FlowRef(identifier));
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            this.salFlowService.removeFlow((RemoveFlowInput) builder.build());
            LOG.debug("Transaction {} - Removed Flow has removed flow: {}", new Object[]{uri, removeDataObj});
        }
    }

    @Override
    protected void update(InstanceIdentifier<? extends DataObject> identifier, DataObject original, DataObject update) {
        if (original instanceof Flow && update instanceof Flow) {
            
            final Flow originalFlow = ((Flow) original);
            final Flow updatedFlow = ((Flow) update);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node>firstIdentifierOf(Node.class);
            final UpdateFlowInputBuilder builder = new UpdateFlowInputBuilder();
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(identifier));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            
            builder.setUpdatedFlow((UpdatedFlow) (new UpdatedFlowBuilder(updatedFlow)).build());
            builder.setOriginalFlow((OriginalFlow) (new OriginalFlowBuilder(originalFlow)).build());
            
            this.salFlowService.updateFlow((UpdateFlowInput) builder.build());
            LOG.debug("Transaction {} - Update Flow has updated flow {} with {}", new Object[]{uri, original, update});
      }
    }

    @Override
    protected void add(InstanceIdentifier<? extends DataObject> identifier, DataObject addDataObj) {
        if ((addDataObj instanceof Flow)) {
            
            final Flow flow = ((Flow) addDataObj);
            final InstanceIdentifier<Table> tableInstanceId = identifier.<Table> firstIdentifierOf(Table.class);
            final InstanceIdentifier<Node> nodeInstanceId = identifier.<Node> firstIdentifierOf(Node.class);
            final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);
            
            builder.setNode(new NodeRef(nodeInstanceId));
            builder.setFlowRef(new FlowRef(identifier));
            builder.setFlowTable(new FlowTableRef(tableInstanceId));
            
            Uri uri = new Uri(this.getTransactionId());
            builder.setTransactionUri(uri);
            this.salFlowService.addFlow((AddFlowInput) builder.build());
            LOG.debug("Transaction {} - Add Flow has added flow: {}", new Object[]{uri, addDataObj});
        }
    }
}
