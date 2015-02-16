/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */
package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DATA_QNAME;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizationException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfBaseOps;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ReadOnlyTx implements DOMDataReadOnlyTransaction {

    private static final Logger LOG  = LoggerFactory.getLogger(ReadOnlyTx.class);

    private final NetconfBaseOps netconfOps;
    private final DataNormalizer normalizer;
    private final RemoteDeviceId id;
    private final FutureCallback<RpcResult<CompositeNode>> loggingCallback;

    public ReadOnlyTx(final NetconfBaseOps netconfOps, final DataNormalizer normalizer, final RemoteDeviceId id) {
        this.netconfOps = netconfOps;
        this.normalizer = normalizer;
        this.id = id;
        // Simple logging callback to log result of read operation
        loggingCallback = new FutureCallback<RpcResult<CompositeNode>>() {
            @Override
            public void onSuccess(final RpcResult<CompositeNode> result) {
                if(result.isSuccessful()) {
                    LOG.trace("{}: Reading data successful", id);
                } else {
                    LOG.warn("{}: Reading data unsuccessful: {}", id, result.getErrors());
                }

            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.warn("{}: Reading data failed", id, t);
            }
        };
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readConfigurationData(
            final YangInstanceIdentifier path) {
        final ListenableFuture<RpcResult<CompositeNode>> configRunning = netconfOps.getConfigRunning(loggingCallback, Optional.fromNullable(path));
        // Find data node and normalize its content
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> transformedFuture = Futures.transform(configRunning, new Function<RpcResult<CompositeNode>, Optional<NormalizedNode<?, ?>>>() {
            @Override
            public Optional<NormalizedNode<?, ?>> apply(final RpcResult<CompositeNode> result) {
                checkReadSuccess(result, path);

                final CompositeNode data = result.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
                final CompositeNode node = (CompositeNode) NetconfMessageTransformUtil.findNode(data, path);

                return data == null ?
                        Optional.<NormalizedNode<?, ?>>absent() :
                        transform(path, node);
            }
        });

        return MappingCheckedFuture.create(transformedFuture, ReadFailedException.MAPPER);
    }

    private void checkReadSuccess(final RpcResult<CompositeNode> result, final YangInstanceIdentifier path) {
        try {
            Preconditions.checkArgument(result.isSuccessful(), "%s: Unable to read data: %s, errors: %s", id, path, result.getErrors());
        } catch (final IllegalArgumentException e) {
            LOG.warn("{}: Unable to read data: {}, errors: {}", id, path, result.getErrors());
            throw e;
        }
    }

    private Optional<NormalizedNode<?, ?>> transform(final YangInstanceIdentifier path, final CompositeNode node) {
        if(node == null) {
            return Optional.absent();
        }
        try {
            return Optional.<NormalizedNode<?, ?>>of(normalizer.toNormalized(path, node).getValue());
        } catch (final Exception e) {
            LOG.error("{}: Unable to normalize data for {}, data: {}", id, path, node, e);
            throw e;
        }
    }

    private CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readOperationalData(
            final YangInstanceIdentifier path) {
        final ListenableFuture<RpcResult<CompositeNode>> configCandidate = netconfOps.getRunning(loggingCallback, Optional.fromNullable(path));

        // Find data node and normalize its content
        final ListenableFuture<Optional<NormalizedNode<?, ?>>> transformedFuture = Futures.transform(configCandidate, new Function<RpcResult<CompositeNode>, Optional<NormalizedNode<?, ?>>>() {
            @Override
            public Optional<NormalizedNode<?, ?>> apply(final RpcResult<CompositeNode> result) {
                checkReadSuccess(result, path);

                final CompositeNode data = result.getResult().getFirstCompositeByName(NETCONF_DATA_QNAME);
                final CompositeNode node = (CompositeNode) NetconfMessageTransformUtil.findNode(data, path);

                return data == null ?
                        Optional.<NormalizedNode<?, ?>>absent() :
                        transform(path, node);
            }
        });

        return MappingCheckedFuture.create(transformedFuture, ReadFailedException.MAPPER);
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        final YangInstanceIdentifier legacyPath = toLegacyPath(normalizer, path, id);

        switch (store) {
            case CONFIGURATION : {
                return readConfigurationData(legacyPath);
            }
            case OPERATIONAL : {
                return readOperationalData(legacyPath);
            }
        }

        throw new IllegalArgumentException(String.format("%s, Cannot read data %s for %s datastore, unknown datastore type", id, path, store));
    }

    @Override
    public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException>
            data = read(store, path);

        try {
            return Futures.immediateCheckedFuture(data.get().isPresent());
        } catch (InterruptedException | ExecutionException e) {
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Exists failed",e));
        }
    }

    static YangInstanceIdentifier toLegacyPath(final DataNormalizer normalizer, final YangInstanceIdentifier path, final RemoteDeviceId id) {
        try {
            return normalizer.toLegacy(path);
        } catch (final DataNormalizationException e) {
            throw new IllegalArgumentException(id + ": Cannot normalize path " + path, e);
        }
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
