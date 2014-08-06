/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.statistics.manager.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.statistics.manager.StatisticsManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.GetFlowStatisticsFromFlowTableInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionAware;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.transaction.rev131103.TransactionId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;

/**
 * statistics-manager
 * org.opendaylight.controller.md.statistics.manager.impl
 *
 * ignore update Listening
 *
 * @author <a href="mailto:vdemcak@cisco.com">Vaclav Demcak</a>
 *
 * Created: Aug 27, 2014
 */
public class StatFlowListeningCommiter extends StatAbstractListeningCommiter<Flow> {

    private static final Logger LOG = LoggerFactory.getLogger(StatFlowListeningCommiter.class);


    public StatFlowListeningCommiter (final StatisticsManager manager, final DataBroker db){
        super(manager, db, Flow.class);
    }

    @Override
    protected InstanceIdentifier<Flow> getWildCardedRegistrationPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class)
                .augmentation(FlowCapableNode.class).child(Table.class).child(Flow.class);
    }

    @Override
    public void createStat(final InstanceIdentifier<Flow> keyIdent, final Flow data) {
        final GetFlowStatisticsFromFlowTableInputBuilder inputBuilder = new GetFlowStatisticsFromFlowTableInputBuilder(data);

        try {
            final RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput> response = flowStatReq.get();
            final GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput result = response.getResult();
//            result.get
        } catch (final Exception e) {
            LOG.error("", e);
        }

    }

    @Override
    public void removeStat(final InstanceIdentifier<Flow> keyIdent) {
        final WriteTransaction trans = manager.getWriteTransaction();
        trans.delete(LogicalDatastoreType.OPERATIONAL, keyIdent);
        trans.submit();
    }

    private final FutureCallback<RpcResult<? extends TransactionAware>> callback =
            new FutureCallback<RpcResult<? extends TransactionAware>>() {
        @Override
        public void onSuccess(final RpcResult<? extends TransactionAware> result) {
            if (result.isSuccessful()) {
                final TransactionId id = result.getResult().getTransactionId();
                if (id == null) {
                    final Throwable t = new UnsupportedOperationException("No protocol support");
                    t.fillInStackTrace();
                    onFailure(t);
                } else {
                    context.registerTransaction(id);
                }
            } else {
                LOG.debug("Statistics request failed: {}", result.getErrors());

                final Throwable t = new RPCFailedException("Failed to send statistics request", result.getErrors());
                t.fillInStackTrace();
                onFailure(t);
            }
        }

        @Override
        public void onFailure(final Throwable t) {
            LOG.debug("Failed to send statistics request", t);
        }
    };
}

class FlowCallback implements FutureCallback<RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput>> {

    public FlowCallback(final InstanceIdentifier<Flow> keyIdent, final Flow data,
            final InstanceIdentifier<FlowCapableNode> nodeIdent) {

    }

    @Override
    public void onSuccess(final RpcResult<GetAggregateFlowStatisticsFromFlowTableForGivenMatchOutput> result) {
        if (result.isSuccessful()) {
            final String id = result.getResult().getTransactionId();
        }
    }

    @Override
    public void onFailure(final Throwable t) {
        // TODO Auto-generated method stub

    }

}


