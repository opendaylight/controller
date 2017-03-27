/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.dom.api.CDSDataTreeProducer;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderInput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class PrefixLeaderHandler {

    private final DOMDataTreeService domDataTreeService;
    private final BindingNormalizedNodeSerializer serializer;

    public PrefixLeaderHandler(final DOMDataTreeService domDataTreeService,
                               final BindingNormalizedNodeSerializer serializer) {
        this.domDataTreeService = domDataTreeService;
        this.serializer = serializer;
    }

    public ListenableFuture<RpcResult<Void>> makeLeaderLocal(final BecomePrefixLeaderInput input) {

        final YangInstanceIdentifier yid = serializer.toYangInstanceIdentifier(input.getPrefix());
        final DOMDataTreeIdentifier prefix = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yid);

        final CDSDataTreeProducer producer =
                (CDSDataTreeProducer) domDataTreeService.createProducer(Collections.singleton(prefix));

        final CDSShardAccess shardAccess = producer.getShardAccess(prefix);

        final SettableFuture<RpcResult<Void>> future = SettableFuture.create();
        final CompletionStage<Void> completionStage = shardAccess.makeLeaderLocal();

        completionStage.thenRun(() -> future.set(RpcResultBuilder.<Void>success().build()));
        completionStage.exceptionally(throwable -> {
            final RpcError error = RpcResultBuilder.newError(RpcError.ErrorType.APPLICATION,
                    "make-leader-local-failed", "Make leader local failed",
                    "cluster-test-app", "", throwable);

            future.set(RpcResultBuilder.<Void>failed().withRpcError(error).build());
            return null;
        });

        return future;
    }
}
