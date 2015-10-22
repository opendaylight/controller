/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.topology;

import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Aggregate different node states into a single state
 */
@Beta
public interface StateAggregator {

    ListenableFuture<Node> combineCreateAttempts(final List<ListenableFuture<Node>> stateFutures);

    ListenableFuture<Node> combineUpdateAttempts(final List<ListenableFuture<Node>> stateFutures);

    ListenableFuture<Void> combineDeleteAttempts(final List<ListenableFuture<Void>> stateFutures);

}

