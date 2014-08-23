/**ab
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.impl;

import org.opendaylight.controller.frm.ForwardingRulesManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Flow Change Listener
 *  add, update and remove {@link Flow} processing from {@link org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent}.
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 */
public class FlowChangeListener extends AbstractChangeListener<Flow> {

    private static final Logger LOG = LoggerFactory.getLogger(FlowChangeListener.class);

    private ListenerRegistration<DataChangeListener> listenerRegistration;

    public FlowChangeListener (final ForwardingRulesManager manager, final DataBroker db) {
        super(manager);
        Preconditions.checkNotNull(db, "DataBroker can not be null!");
        /* Build Path */
        InstanceIdentifier<Flow> flowWildCardIdentifier = InstanceIdentifier.create(Nodes.class)
                .child(Node.class).augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
        this.listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION,
                flowWildCardIdentifier, FlowChangeListener.this, DataChangeScope.SUBTREE);
    }

    @Override
    public void close() {
        if (listenerRegistration != null) {
            try {
                listenerRegistration.close();
            } catch (Exception e) {
                LOG.error("Error by stop FRM FlowChangeListener.", e);
            }
            listenerRegistration = null;
        }
    }

    @Override
    public void remove(final InstanceIdentifier<Flow> identifier,
                       final Flow removeDataObj,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final TableKey tableKey = identifier.firstKeyOf(Table.class, TableKey.class);
        if (tableIdValidationPrecondition(tableKey, removeDataObj)) {
            final RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(removeDataObj);
            builder.setFlowRef(new FlowRef(identifier));
            builder.setNode(new NodeRef(nodeIdent));
            builder.setFlowTable(new FlowTableRef(nodeIdent.child(Table.class, tableKey)));

            Uri uri = new Uri(provider.getNewTransactionId());
            builder.setTransactionUri(uri);
            this.provider.getSalFlowService().removeFlow(builder.build());
            LOG.debug("Transaction {} - Removed Flow has removed flow: {}", uri, removeDataObj);
        }
    }

    @Override
    public void update(final InstanceIdentifier<Flow> identifier,
                       final Flow original, final Flow update,
                       final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final TableKey tableKey = identifier.firstKeyOf(Table.class, TableKey.class);
        if (tableIdValidationPrecondition(tableKey, update)) {
            final UpdateFlowInputBuilder builder = new UpdateFlowInputBuilder();

            builder.setNode(new NodeRef(nodeIdent));
            builder.setFlowRef(new FlowRef(identifier));

            Uri uri = new Uri(provider.getNewTransactionId());
            builder.setTransactionUri(uri);

            builder.setUpdatedFlow((new UpdatedFlowBuilder(update)).build());
            builder.setOriginalFlow((new OriginalFlowBuilder(original)).build());

            this.provider.getSalFlowService().updateFlow(builder.build());
            LOG.debug("Transaction {} - Update Flow has updated flow {} with {}", uri, original, update);
        }
    }

    @Override
    public void add(final InstanceIdentifier<Flow> identifier,
                    final Flow addDataObj,
                    final InstanceIdentifier<FlowCapableNode> nodeIdent) {

        final TableKey tableKey = identifier.firstKeyOf(Table.class, TableKey.class);
        if (tableIdValidationPrecondition(tableKey, addDataObj)) {
            final AddFlowInputBuilder builder = new AddFlowInputBuilder(addDataObj);

            builder.setNode(new NodeRef(nodeIdent));
            builder.setFlowRef(new FlowRef(identifier));
            builder.setFlowTable(new FlowTableRef(nodeIdent.child(Table.class, tableKey)));

            Uri uri = new Uri(provider.getNewTransactionId());
            builder.setTransactionUri(uri);
            this.provider.getSalFlowService().addFlow(builder.build());
            LOG.debug("Transaction {} - Add Flow has added flow: {}", uri, addDataObj);
        }
    }

    static boolean tableIdValidationPrecondition (final TableKey tableKey, final Flow flow) {
        Preconditions.checkNotNull(tableKey, "TableKey can not be null or empty!");
        Preconditions.checkNotNull(flow, "Flow can not be null or empty!");
        if (flow.getTableId() != tableKey.getId()) {
            LOG.error("TableID in URI tableId={} and in palyload tableId={} is not same.",
                    flow.getTableId(), tableKey.getId());
            return false;
        }
        return true;
    }
}

