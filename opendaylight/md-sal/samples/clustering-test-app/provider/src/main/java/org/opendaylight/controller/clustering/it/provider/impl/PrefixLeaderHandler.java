/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.it.provider.impl;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.datastore.exceptions.TimeoutException;
import org.opendaylight.controller.cluster.dom.api.CDSDataTreeProducer;
import org.opendaylight.controller.cluster.dom.api.CDSShardAccess;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderInput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderOutput;
import org.opendaylight.yang.gen.v1.tag.opendaylight.org._2017.controller.yang.lowlevel.control.rev170215.BecomePrefixLeaderOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(forRemoval = true)
public class PrefixLeaderHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PrefixLeaderHandler.class);

    private final DOMDataTreeService domDataTreeService;
    private final BindingNormalizedNodeSerializer serializer;

    public PrefixLeaderHandler(final DOMDataTreeService domDataTreeService,
                               final BindingNormalizedNodeSerializer serializer) {
        this.domDataTreeService = domDataTreeService;
        this.serializer = serializer;
    }

    public ListenableFuture<RpcResult<BecomePrefixLeaderOutput>> makeLeaderLocal(final BecomePrefixLeaderInput input) {

        final YangInstanceIdentifier yid = serializer.toYangInstanceIdentifier(input.getPrefix());
        final DOMDataTreeIdentifier prefix = new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, yid);

        try (CDSDataTreeProducer producer =
                     (CDSDataTreeProducer) domDataTreeService.createProducer(Collections.singleton(prefix))) {

            final CDSShardAccess shardAccess = producer.getShardAccess(prefix);

            final CompletionStage<Void> completionStage = shardAccess.makeLeaderLocal();

            completionStage.exceptionally(throwable -> {
                LOG.error("Leader movement failed.", throwable);
                return null;
            });
        } catch (final DOMDataTreeProducerException e) {
            LOG.warn("Error while closing producer", e);
        } catch (final TimeoutException e) {
            LOG.warn("Timeout while on producer operation", e);
            Futures.immediateFuture(RpcResultBuilder.failed().withError(RpcError.ErrorType.RPC,
                    "resource-denied-transport", "Timeout while opening producer please retry.", "clustering-it",
                    "clustering-it", e));
        }

        return Futures.immediateFuture(RpcResultBuilder.success(new BecomePrefixLeaderOutputBuilder().build()).build());
    }
}
