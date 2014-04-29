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
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_TARGET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.ROLLBACK_ON_ERROR_OPTION;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.common.util.RpcErrors;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.FailedRpcResult;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

class NetconfDeviceTwoPhaseCommitTransaction implements DataCommitTransaction<InstanceIdentifier, CompositeNode> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfDeviceTwoPhaseCommitTransaction.class);

    private final DataModification<InstanceIdentifier, CompositeNode> modification;
    private final RpcImplementation device;
    private final boolean candidateSupported;
    private final boolean rollbackSupported;

    // TODO too many args
    public NetconfDeviceTwoPhaseCommitTransaction(final RpcImplementation rpc,
            final DataModification<InstanceIdentifier, CompositeNode> modification,
            final boolean candidateSupported, final boolean rollbackOnErrorSupported) {
        this.device = Preconditions.checkNotNull(rpc);
        this.modification = Preconditions.checkNotNull(modification);
        this.candidateSupported = candidateSupported;
        this.rollbackSupported = rollbackOnErrorSupported;
    }

    void prepare() throws InterruptedException, ExecutionException {
        for (final InstanceIdentifier toRemove : modification.getRemovedConfigurationData()) {
            sendDelete(toRemove);
        }
        for(final Entry<InstanceIdentifier, CompositeNode> toUpdate : modification.getUpdatedConfigurationData().entrySet()) {
            sendMerge(toUpdate.getKey(),toUpdate.getValue());
        }
    }

    private void sendMerge(final InstanceIdentifier key, final CompositeNode value) throws InterruptedException, ExecutionException {
        sendEditRpc(createEditStructure(key, Optional.<String>absent(), Optional.of(value)));
    }

    private void sendDelete(final InstanceIdentifier toDelete) throws InterruptedException, ExecutionException {
        sendEditRpc(createEditStructure(toDelete, Optional.of("delete"), Optional.<CompositeNode> absent()));
    }

    private void sendEditRpc(final CompositeNode editStructure) throws InterruptedException, ExecutionException {
        final ImmutableCompositeNode builder = createEditConfigRequest(editStructure);
        final RpcResult<CompositeNode> rpcResult = device.invokeRpc(NETCONF_EDIT_CONFIG_QNAME, builder).get();
        Preconditions.checkState(rpcResult.isSuccessful(),"Rpc Result was unsuccessful");
    }

    private ImmutableCompositeNode createEditConfigRequest(final CompositeNode editStructure) {
        final CompositeNodeBuilder<ImmutableCompositeNode> ret = ImmutableCompositeNode.builder();

        final Node<?> targetNode;
        if(candidateSupported) {
            targetNode = ImmutableCompositeNode.create(NETCONF_CANDIDATE_QNAME, ImmutableList.<Node<?>>of());
        } else {
            targetNode = ImmutableCompositeNode.create(NETCONF_RUNNING_QNAME, ImmutableList.<Node<?>>of());
        }

        final Node<?> targetWrapperNode = ImmutableCompositeNode.create(NETCONF_TARGET_QNAME, ImmutableList.<Node<?>>of(targetNode));
        ret.add(targetWrapperNode);

        if(rollbackSupported) {
            ret.addLeaf(NETCONF_ERROR_OPTION_QNAME, ROLLBACK_ON_ERROR_OPTION);
        }
        ret.setQName(NETCONF_EDIT_CONFIG_QNAME);
        ret.add(editStructure);
        return ret.toInstance();
    }

    private CompositeNode createEditStructure(final InstanceIdentifier dataPath, final Optional<String> operation,
            final Optional<CompositeNode> lastChildOverride) {
        // TODO refactor method
        final List<PathArgument> path = dataPath.getPath();
        final List<PathArgument> reversed = Lists.reverse(path);
        CompositeNode previous = null;
        boolean isLast = true;
        for (final PathArgument arg : reversed) {
            final CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
            builder.setQName(arg.getNodeType());
            Map<QName, Object> predicates = Collections.emptyMap();
            if (arg instanceof NodeIdentifierWithPredicates) {
                predicates = ((NodeIdentifierWithPredicates) arg).getKeyValues();
            }
            for (final Entry<QName, Object> entry : predicates.entrySet()) {
                builder.addLeaf(entry.getKey(), entry.getValue());
            }

            if (isLast) {
                if (operation.isPresent()) {
                    builder.setAttribute(NETCONF_OPERATION_QNAME, operation.get());
                }
                if (lastChildOverride.isPresent()) {
                    final List<Node<?>> children = lastChildOverride.get().getChildren();
                    for(final Node<?> child : children) {
                        if(!predicates.containsKey(child.getKey())) {
                            builder.add(child);
                        }
                    }

                }
            } else {
                builder.add(previous);
            }
            previous = builder.toInstance();
            isLast = false;
        }
        return ImmutableCompositeNode.create(NETCONF_CONFIG_QNAME, ImmutableList.<Node<?>>of(previous));
    }

    @Override
    public RpcResult<Void> finish() {
        try {
            final RpcResult<?> rpcResult = device.invokeRpc(NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME, getCommitRequest()).get();
            return new RpcResultVoidWrapper(rpcResult);
        } catch (final InterruptedException | ExecutionException e) {
            LOG.warn("Failed to finish operation", e);
            return new FailedRpcResult<>(RpcErrors.getRpcError(null, null, null, RpcError.ErrorSeverity.ERROR,
                    "Unexpected operation error", RpcError.ErrorType.APPLICATION, null));
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
