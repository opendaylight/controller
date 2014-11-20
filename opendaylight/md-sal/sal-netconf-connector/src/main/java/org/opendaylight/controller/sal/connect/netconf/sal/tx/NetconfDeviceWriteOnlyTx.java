/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.sal.tx;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.DISCARD_CHANGES_RPC_CONTENT;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DEFAULT_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DISCARD_CHANGES_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_TARGET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.ROLLBACK_ON_ERROR_OPTION;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.util.compat.DataNormalizer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfDeviceWriteOnlyTx implements DOMDataWriteTransaction, FutureCallback<RpcResult<TransactionStatus>> {

    private static final Logger LOG  = LoggerFactory.getLogger(NetconfDeviceWriteOnlyTx.class);

    private final RemoteDeviceId id;
    private final RpcImplementation rpc;
    private final DataNormalizer normalizer;

    private final boolean rollbackSupported;
    private final boolean candidateSupported;
    private final CompositeNode targetNode;

    // Allow commit to be called only once
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public NetconfDeviceWriteOnlyTx(final RemoteDeviceId id, final RpcImplementation rpc, final DataNormalizer normalizer, final boolean candidateSupported, final boolean rollbackOnErrorSupported) {
        this.id = id;
        this.rpc = rpc;
        this.normalizer = normalizer;

        this.candidateSupported = candidateSupported;
        this.targetNode = getTargetNode(this.candidateSupported);
        this.rollbackSupported = rollbackOnErrorSupported;
    }

    @Override
    public boolean cancel() {
        if(isFinished()) {
            return false;
        }

        return discardChanges();
    }

    private boolean isFinished() {
        return finished.get();
    }

    private boolean discardChanges() {
        finished.set(true);

        if(candidateSupported) {
            sendDiscardChanges();
        }
        return true;
    }

    // TODO should the edit operations be blocking ?
    // TODO should the discard-changes operations be blocking ?

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotFinished();
        Preconditions.checkArgument(store == LogicalDatastoreType.CONFIGURATION, "Can merge only configuration, not %s", store);

        try {
            final YangInstanceIdentifier legacyPath = NetconfDeviceReadOnlyTx.toLegacyPath(normalizer, path, id);
            final CompositeNode legacyData = normalizer.toLegacy(path, data);
            sendEditRpc(
                    createEditConfigStructure(legacyPath, Optional.of(ModifyAction.REPLACE), Optional.fromNullable(legacyData)), Optional.of(ModifyAction.NONE));
        } catch (final ExecutionException e) {
            LOG.warn("{}: Error putting data to {}, data: {}, discarding changes", id, path, data, e);
            discardChanges();
            throw new RuntimeException(id + ": Error while replacing " + path, e);
        }
    }

    private void checkNotFinished() {
        Preconditions.checkState(isFinished() == false, "%s: Transaction %s already finished", id, getIdentifier());
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotFinished();
        Preconditions.checkArgument(store == LogicalDatastoreType.CONFIGURATION, "%s: Can merge only configuration, not %s", id, store);

        try {
            final YangInstanceIdentifier legacyPath = NetconfDeviceReadOnlyTx.toLegacyPath(normalizer, path, id);
            final CompositeNode legacyData = normalizer.toLegacy(path, data);
            sendEditRpc(
                    createEditConfigStructure(legacyPath, Optional.<ModifyAction> absent(), Optional.fromNullable(legacyData)), Optional.<ModifyAction> absent());
        } catch (final ExecutionException e) {
            LOG.warn("{}: Error merging data to {}, data: {}, discarding changes", id, path, data, e);
            discardChanges();
            throw new RuntimeException(id + ": Error while merging " + path, e);
        }
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkNotFinished();
        Preconditions.checkArgument(store == LogicalDatastoreType.CONFIGURATION, "%s: Can merge only configuration, not %s", id, store);

        try {
            sendEditRpc(
                    createEditConfigStructure(NetconfDeviceReadOnlyTx.toLegacyPath(normalizer, path, id), Optional.of(ModifyAction.DELETE), Optional.<CompositeNode>absent()), Optional.of(ModifyAction.NONE));
        } catch (final ExecutionException e) {
            LOG.warn("{}: Error deleting data {}, discarding changes", id, path, e);
            discardChanges();
            throw new RuntimeException(id + ": Error while deleting " + path, e);
        }
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        final ListenableFuture<Void> commmitFutureAsVoid = Futures.transform(commit(), new Function<RpcResult<TransactionStatus>, Void>() {
            @Override
            public Void apply(final RpcResult<TransactionStatus> input) {
                return null;
            }
        });

        return Futures.makeChecked(commmitFutureAsVoid, new Function<Exception, TransactionCommitFailedException>() {
            @Override
            public TransactionCommitFailedException apply(final Exception input) {
                return new TransactionCommitFailedException("Submit of transaction " + getIdentifier() + " failed", input);
            }
        });
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        checkNotFinished();
        finished.set(true);

        if(candidateSupported == false) {
            return Futures.immediateFuture(RpcResultBuilder.success(TransactionStatus.COMMITED).build());
        }

        final ListenableFuture<RpcResult<CompositeNode>> rpcResult = rpc.invokeRpc(
                NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME, NetconfMessageTransformUtil.COMMIT_RPC_CONTENT);

        final ListenableFuture<RpcResult<TransactionStatus>> transformed = Futures.transform(rpcResult,
                new Function<RpcResult<CompositeNode>, RpcResult<TransactionStatus>>() {
                    @Override
                    public RpcResult<TransactionStatus> apply(final RpcResult<CompositeNode> input) {
                        if (input.isSuccessful()) {
                            return RpcResultBuilder.success(TransactionStatus.COMMITED).build();
                        } else {
                            final RpcResultBuilder<TransactionStatus> failed = RpcResultBuilder.failed();
                            for (final RpcError rpcError : input.getErrors()) {
                                failed.withError(rpcError.getErrorType(), rpcError.getTag(), rpcError.getMessage(),
                                        rpcError.getApplicationTag(), rpcError.getInfo(), rpcError.getCause());
                            }
                            return failed.build();
                        }
                    }
                });

        Futures.addCallback(transformed, this);
        return transformed;
    }

    @Override
    public void onSuccess(final RpcResult<TransactionStatus> result) {
        LOG.debug("{}: Write successful, transaction: {}", id, getIdentifier());
    }

    @Override
    public void onFailure(final Throwable t) {
        LOG.warn("{}: Write failed, transaction {}, discarding changes", id, getIdentifier(), t);
        discardChanges();
    }

    private void sendEditRpc(final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation) throws ExecutionException {
        final CompositeNode editConfigRequest = createEditConfigRequest(editStructure, defaultOperation);
        final RpcResult<CompositeNode> rpcResult;
        try {
            rpcResult = rpc.invokeRpc(NETCONF_EDIT_CONFIG_QNAME, editConfigRequest).get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(id + ": Interrupted while waiting for response", e);
        }

        // Check result
        if(rpcResult.isSuccessful() == false) {
            throw new ExecutionException(
                    String.format("%s: Pre-commit rpc failed, request: %s, errors: %s", id, editConfigRequest, rpcResult.getErrors()), null);
        }
    }

    private void sendDiscardChanges() {
        final ListenableFuture<RpcResult<CompositeNode>> discardFuture = rpc.invokeRpc(NETCONF_DISCARD_CHANGES_QNAME, DISCARD_CHANGES_RPC_CONTENT);
        Futures.addCallback(discardFuture, new FutureCallback<RpcResult<CompositeNode>>() {
            @Override
            public void onSuccess(final RpcResult<CompositeNode> result) {
                LOG.debug("{}: Discarding transaction: {}", id, getIdentifier());
            }

            @Override
            public void onFailure(final Throwable t) {
                LOG.error("{}: Discarding changes failed, transaction: {}. Device configuration might be corrupted", id, getIdentifier(), t);
                throw new RuntimeException(id + ": Discarding changes failed, transaction " + getIdentifier(), t);
            }
        });
    }

    private CompositeNode createEditConfigStructure(final YangInstanceIdentifier dataPath, final Optional<ModifyAction> operation,
                                                    final Optional<CompositeNode> lastChildOverride) {
        Preconditions.checkArgument(Iterables.isEmpty(dataPath.getPathArguments()) == false, "Instance identifier with empty path %s", dataPath);

        // Create deepest edit element with expected edit operation
        CompositeNode previous = getDeepestEditElement(dataPath.getLastPathArgument(), operation, lastChildOverride);

        Iterator<PathArgument> it = dataPath.getReversePathArguments().iterator();
        // Remove already processed deepest child
        it.next();

        // Create edit structure in reversed order
        while (it.hasNext()) {
            final YangInstanceIdentifier.PathArgument arg = it.next();
            final CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
            builder.setQName(arg.getNodeType());

            addPredicatesToCompositeNodeBuilder(getPredicates(arg), builder);

            builder.add(previous);
            previous = builder.toInstance();
        }
        return ImmutableCompositeNode.create(NETCONF_CONFIG_QNAME, ImmutableList.<Node<?>>of(previous));
    }

    private void addPredicatesToCompositeNodeBuilder(final Map<QName, Object> predicates, final CompositeNodeBuilder<ImmutableCompositeNode> builder) {
        for (final Map.Entry<QName, Object> entry : predicates.entrySet()) {
            builder.addLeaf(entry.getKey(), entry.getValue());
        }
    }

    private Map<QName, Object> getPredicates(final YangInstanceIdentifier.PathArgument arg) {
        Map<QName, Object> predicates = Collections.emptyMap();
        if (arg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            predicates = ((YangInstanceIdentifier.NodeIdentifierWithPredicates) arg).getKeyValues();
        }
        return predicates;
    }

    private CompositeNode getDeepestEditElement(final YangInstanceIdentifier.PathArgument arg, final Optional<ModifyAction> operation, final Optional<CompositeNode> lastChildOverride) {
        final CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(arg.getNodeType());

        final Map<QName, Object> predicates = getPredicates(arg);
        addPredicatesToCompositeNodeBuilder(predicates, builder);

        if (operation.isPresent()) {
            builder.setAttribute(NETCONF_OPERATION_QNAME, modifyOperationToXmlString(operation.get()));
        }
        if (lastChildOverride.isPresent()) {
            final List<Node<?>> children = lastChildOverride.get().getValue();
            for(final Node<?> child : children) {
                if(!predicates.containsKey(child.getKey())) {
                    builder.add(child);
                }
            }
        }

        return builder.toInstance();
    }

    private CompositeNode createEditConfigRequest(final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation) {
        final CompositeNodeBuilder<ImmutableCompositeNode> ret = ImmutableCompositeNode.builder();

        // Target
        final Node<?> targetWrapperNode = ImmutableCompositeNode.create(NETCONF_TARGET_QNAME, ImmutableList.<Node<?>>of(targetNode));
        ret.add(targetWrapperNode);

        // Default operation
        if(defaultOperation.isPresent()) {
            final SimpleNode<String> defOp = NodeFactory.createImmutableSimpleNode(NETCONF_DEFAULT_OPERATION_QNAME, null, modifyOperationToXmlString(defaultOperation.get()));
            ret.add(defOp);
        }

        // Error option
        if(rollbackSupported) {
            ret.addLeaf(NETCONF_ERROR_OPTION_QNAME, ROLLBACK_ON_ERROR_OPTION);
        }

        ret.setQName(NETCONF_EDIT_CONFIG_QNAME);
        // Edit content
        ret.add(editStructure);
        return ret.toInstance();
    }

    private String modifyOperationToXmlString(final ModifyAction operation) {
        return operation.name().toLowerCase();
    }

    public CompositeNode getTargetNode(final boolean candidateSupported) {
        if(candidateSupported) {
            return ImmutableCompositeNode.create(NETCONF_CANDIDATE_QNAME, ImmutableList.<Node<?>>of());
        } else {
            return ImmutableCompositeNode.create(NETCONF_RUNNING_QNAME, ImmutableList.<Node<?>>of());
        }
    }

    @Override
    public Object getIdentifier() {
        return this;
    }
}
