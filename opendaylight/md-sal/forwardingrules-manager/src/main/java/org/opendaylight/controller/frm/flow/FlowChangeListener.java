/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.frm.flow;

import java.math.BigInteger;

import org.opendaylight.controller.frm.AbstractChangeListener;
import org.opendaylight.controller.frm.FlowCookieProducer;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.UpdateFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.OriginalFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.flow.update.UpdatedFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowCookie;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
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
public class FlowChangeListener extends AbstractChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(FlowChangeListener.class);

    private final FlowProvider provider;

    public FlowChangeListener (final FlowProvider provider) {
        this.provider = Preconditions.checkNotNull(provider, "FlowProvider can not be null !");
    }

    @Override
    protected void remove(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject removeDataObj) {

        final Flow flow = ((Flow) removeDataObj);
        final InstanceIdentifier<Table> tableIdent = identifier.firstIdentifierOf(Table.class);
        final InstanceIdentifier<Node> nodeIdent = identifier.firstIdentifierOf(Node.class);
        final RemoveFlowInputBuilder builder = new RemoveFlowInputBuilder(flow);

        // use empty cookie mask in order to delete flow even with generated cookie
        builder.setCookieMask(new FlowCookie(BigInteger.ZERO));

        builder.setFlowRef(new FlowRef(identifier));
        builder.setNode(new NodeRef(nodeIdent));
        builder.setFlowTable(new FlowTableRef(tableIdent));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalFlowService().removeFlow(builder.build());
        LOG.debug("Transaction {} - Removed Flow has removed flow: {}", new Object[]{uri, removeDataObj});
    }

    @Override
    protected void update(final InstanceIdentifier<? extends DataObject> identifier,
                          final DataObject original, final DataObject update) {

        final Flow originalFlow = ((Flow) original);
        final Flow updatedFlow = ((Flow) update);
        final InstanceIdentifier<Node> nodeIdent = identifier.firstIdentifierOf(Node.class);
        final UpdateFlowInputBuilder builder = new UpdateFlowInputBuilder();

        builder.setNode(new NodeRef(nodeIdent));
        builder.setFlowRef(new FlowRef(identifier));

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);

        builder.setUpdatedFlow((new UpdatedFlowBuilder(updatedFlow)).build());
        builder.setOriginalFlow((new OriginalFlowBuilder(originalFlow)).build());

        this.provider.getSalFlowService().updateFlow(builder.build());
        LOG.debug("Transaction {} - Update Flow has updated flow {} with {}", new Object[]{uri, original, update});
    }

    @Override
    protected void add(final InstanceIdentifier<? extends DataObject> identifier,
                       final DataObject addDataObj) {

        final Flow flow = ((Flow) addDataObj);
        final InstanceIdentifier<Table> tableIdent = identifier.firstIdentifierOf(Table.class);
        final NodeRef nodeRef = new NodeRef(identifier.firstIdentifierOf(Node.class));
        final FlowCookie flowCookie = new FlowCookie(FlowCookieProducer.INSTANCE.getNewCookie(tableIdent));
        final AddFlowInputBuilder builder = new AddFlowInputBuilder(flow);

        builder.setNode(nodeRef);
        builder.setFlowRef(new FlowRef(identifier));
        builder.setFlowTable(new FlowTableRef(tableIdent));
        builder.setCookie( flowCookie );

        Uri uri = new Uri(this.getTransactionId());
        builder.setTransactionUri(uri);
        this.provider.getSalFlowService().addFlow(builder.build());
        LOG.debug("Transaction {} - Add Flow has added flow: {}", new Object[]{uri, addDataObj});
    }

    @Override
    protected boolean preconditionForChange(final InstanceIdentifier<? extends DataObject> identifier,
            final DataObject dataObj, final DataObject update) {

        final ReadOnlyTransaction trans = this.provider.getDataService().newReadOnlyTransaction();
        return update != null
                ? (dataObj instanceof Flow && update instanceof Flow && isNodeAvailable(identifier, trans))
                : (dataObj instanceof Flow && isNodeAvailable(identifier, trans));
    }
}
