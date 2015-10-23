/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.controller.netconf.topology.StateAggregator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class SingleStateAggregatorImpl implements StateAggregator {

    @Override
    public ListenableFuture<Node> combineCreateAttempts(final List<ListenableFuture<Node>> stateFutures) {
        return getSingleFuture(stateFutures);
    }

    @Override
    public ListenableFuture<Node> combineUpdateAttempts(final List<ListenableFuture<Node>> stateFutures) {
        return getSingleFuture(stateFutures);
    }

    @Override
    public ListenableFuture<Void> combineDeleteAttempts(final List<ListenableFuture<Void>> stateFutures) {
        return getSingleFuture(stateFutures);
    }

    private <T> ListenableFuture<T> getSingleFuture(final List<ListenableFuture<T>> stateFutures) {
        Preconditions.checkArgument(stateFutures.size() == 1, "Recived multipe results, single resut is enforced here");
        return stateFutures.get(0);
    }

}
