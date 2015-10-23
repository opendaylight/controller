/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.util;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.netconf.topology.util.NodeWriter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SalNodeWriter implements NodeWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SalNodeWriter.class);

    private final DataBroker dataBroker;
    private final String topologyId;

    private final InstanceIdentifier<Topology> topologyListPath;

    public SalNodeWriter(final DataBroker dataBroker, final String topologyId) {
        this.dataBroker = dataBroker;
        this.topologyId = topologyId;
        this.topologyListPath = createTopologyId(this.topologyId);
    }

    //FIXME change to txChains
    @Override
    public void init(@Nonnull final NodeId id, @Nonnull final Node operationalDataNode) {
        // put into Datastore
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, createBindingPathForTopology(id), operationalDataNode);
        commitTransaction(wTx, id, "init");
    }

    @Override
    public void update(@Nonnull final NodeId id, @Nonnull final Node operationalDataNode) {
        // merge
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.put(LogicalDatastoreType.OPERATIONAL, createBindingPathForTopology(id), operationalDataNode);
        commitTransaction(wTx, id, "update");
    }

    @Override
    public void delete(@Nonnull final NodeId id) {
        // delete
        final WriteTransaction wTx = dataBroker.newWriteOnlyTransaction();
        wTx.delete(LogicalDatastoreType.OPERATIONAL, createBindingPathForTopology(id));
        commitTransaction(wTx, id, "delete");
    }

    private void commitTransaction(final WriteTransaction transaction, final NodeId id, final String txType) {
        LOG.debug("{}: Committing Transaction {}:{}", id.getValue(), txType, transaction.getIdentifier());
        final CheckedFuture<Void, TransactionCommitFailedException> result = transaction.submit();

        Futures.addCallback(result, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                LOG.debug("{}: Transaction({}) {} SUCCESSFUL", id.getValue(), txType, transaction.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("{}: Transaction({}) {} FAILED!", id.getValue(), txType, transaction.getIdentifier(), t);
                throw new IllegalStateException(id + "  Transaction(" + txType + ") not committed correctly", t);
            }
        });
    }

    private InstanceIdentifier<Node> createBindingPathForTopology(final NodeId id) {
        return topologyListPath.child(Node.class, new NodeKey(id));
    }

    private InstanceIdentifier<Topology> createTopologyId(final String topologyId) {
        final InstanceIdentifier<NetworkTopology> networkTopology = InstanceIdentifier.create(NetworkTopology.class);
        return networkTopology.child(Topology.class, new TopologyKey(new TopologyId(topologyId)));
    }
}
