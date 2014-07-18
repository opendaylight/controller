/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.sal;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DEFAULT_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_TARGET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.ROLLBACK_ON_ERROR_OPTION;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Remote transaction that delegates data change to remote device using netconf messages.
 */
final class NetconfDeviceTwoPhaseCommitTransaction implements DataCommitTransaction<InstanceIdentifier, CompositeNode> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceTwoPhaseCommitTransaction.class);

    private final DataModification<InstanceIdentifier, CompositeNode> modification;
    private final RpcImplementation rpc;
    private final boolean rollbackSupported;
    private final RemoteDeviceId id;
    private final CompositeNode targetNode;

    public NetconfDeviceTwoPhaseCommitTransaction(final RemoteDeviceId id, final RpcImplementation rpc,
            final DataModification<InstanceIdentifier, CompositeNode> modification,
            final boolean candidateSupported, final boolean rollbackOnErrorSupported) {
        this.id = id;
        this.rpc = Preconditions.checkNotNull(rpc);
        this.modification = Preconditions.checkNotNull(modification);
        this.targetNode = getTargetNode(candidateSupported);
        this.rollbackSupported = rollbackOnErrorSupported;
    }

    /**
     * Prepare phase, sends 1 or more netconf edit config operations to modify the data
     *
     * In case of failure or unexpected error response, ExecutionException is thrown
     */
    void prepare() throws InterruptedException, ExecutionException {
        for (final InstanceIdentifier toRemove : modification.getRemovedConfigurationData()) {
            sendDelete(toRemove);
        }
        for(final Entry<InstanceIdentifier, CompositeNode> toUpdate : modification.getUpdatedConfigurationData().entrySet()) {
            sendMerge(toUpdate.getKey(),toUpdate.getValue());
        }
    }

    private void sendMerge(final InstanceIdentifier key, final CompositeNode value) throws InterruptedException, ExecutionException {
        sendEditRpc(createEditConfigStructure(key, Optional.<ModifyAction>absent(), Optional.of(value)), Optional.<ModifyAction>absent());
    }

    private void sendDelete(final InstanceIdentifier toDelete) throws InterruptedException, ExecutionException {
        sendEditRpc(createEditConfigStructure(toDelete, Optional.of(ModifyAction.DELETE), Optional.<CompositeNode>absent()), Optional.of(ModifyAction.NONE));
    }

    private void sendEditRpc(final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation) throws InterruptedException, ExecutionException {
        final ImmutableCompositeNode editConfigRequest = createEditConfigRequest(editStructure, defaultOperation);
        final RpcResult<CompositeNode> rpcResult = rpc.invokeRpc(NETCONF_EDIT_CONFIG_QNAME, editConfigRequest).get();

        // Check result
        if(rpcResult.isSuccessful() == false) {
            throw new ExecutionException(
                    String.format("%s: Pre-commit rpc failed, request: %s, errors: %s", id, editConfigRequest, rpcResult.getErrors()), null);
        }
    }

    private ImmutableCompositeNode createEditConfigRequest(final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation) {
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

    private CompositeNode createEditConfigStructure(final InstanceIdentifier dataPath, final Optional<ModifyAction> operation,
            final Optional<CompositeNode> lastChildOverride) {
        Preconditions.checkArgument(Iterables.isEmpty(dataPath.getPathArguments()) == false, "Instance identifier with empty path %s", dataPath);

        List<PathArgument> reversedPath = Lists.reverse(dataPath.getPath());

        // Create deepest edit element with expected edit operation
        CompositeNode previous = getDeepestEditElement(reversedPath.get(0), operation, lastChildOverride);

        // Remove already processed deepest child
        reversedPath = Lists.newArrayList(reversedPath);
        reversedPath.remove(0);

        // Create edit structure in reversed order
        for (final PathArgument arg : reversedPath) {
            final CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
            builder.setQName(arg.getNodeType());

            addPredicatesToCompositeNodeBuilder(getPredicates(arg), builder);

            builder.add(previous);
            previous = builder.toInstance();
        }
        return ImmutableCompositeNode.create(NETCONF_CONFIG_QNAME, ImmutableList.<Node<?>>of(previous));
    }

    private void addPredicatesToCompositeNodeBuilder(final Map<QName, Object> predicates, final CompositeNodeBuilder<ImmutableCompositeNode> builder) {
        for (final Entry<QName, Object> entry : predicates.entrySet()) {
            builder.addLeaf(entry.getKey(), entry.getValue());
        }
    }

    private Map<QName, Object> getPredicates(final PathArgument arg) {
        Map<QName, Object> predicates = Collections.emptyMap();
        if (arg instanceof NodeIdentifierWithPredicates) {
            predicates = ((NodeIdentifierWithPredicates) arg).getKeyValues();
        }
        return predicates;
    }

    private CompositeNode getDeepestEditElement(final PathArgument arg, final Optional<ModifyAction> operation, final Optional<CompositeNode> lastChildOverride) {
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

    private String modifyOperationToXmlString(final ModifyAction operation) {
        return operation.name().toLowerCase();
    }

    /**
     * Send commit rpc to finish the transaction
     * In case of failure or unexpected error response, ExecutionException is thrown
     */
    @Override
    public RpcResult<Void> finish() {
        try {
            final RpcResult<?> rpcResult = rpc.invokeRpc(NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME, getCommitRequest()).get();
            return new RpcResultVoidWrapper(rpcResult);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(id + ": Interrupted while waiting for response", e);
        } catch (final ExecutionException e) {
            LOG.warn("{}: Failed to finish commit operation", id, e);
            return RpcResultBuilder.<Void>failed().withError( RpcError.ErrorType.APPLICATION,
                            id + ": Unexpected operation error during commit operation", e ).build();
        }
    }

    private ImmutableCompositeNode getCommitRequest() {
        final CompositeNodeBuilder<ImmutableCompositeNode> commitInput = ImmutableCompositeNode.builder();
        commitInput.setQName(NETCONF_COMMIT_QNAME);
        return commitInput.toInstance();
    }

    @Override
    public DataModification<InstanceIdentifier, CompositeNode> getModification() {
        return this.modification;
    }

    @Override
    public RpcResult<Void> rollback() throws IllegalStateException {
        // TODO BUG-732 implement rollback by sending discard changes
        return null;
    }

    public CompositeNode getTargetNode(final boolean candidateSupported) {
        if(candidateSupported) {
            return ImmutableCompositeNode.create(NETCONF_CANDIDATE_QNAME, ImmutableList.<Node<?>>of());
        } else {
            return ImmutableCompositeNode.create(NETCONF_RUNNING_QNAME, ImmutableList.<Node<?>>of());
        }
    }

    private static final class RpcResultVoidWrapper implements RpcResult<Void> {

        private final RpcResult<?> rpcResult;

        public RpcResultVoidWrapper(final RpcResult<?> rpcResult) {
            this.rpcResult = rpcResult;
        }

        @Override
        public boolean isSuccessful() {
            return rpcResult.isSuccessful();
        }

        @Override
        public Void getResult() {
            return null;
        }

        @Override
        public Collection<RpcError> getErrors() {
            return rpcResult.getErrors();
        }
    }
}
