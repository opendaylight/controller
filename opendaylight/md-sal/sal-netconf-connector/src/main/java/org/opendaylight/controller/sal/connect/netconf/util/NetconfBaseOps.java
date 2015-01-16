/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.opendaylight.controller.sal.connect.netconf.util;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.DISCARD_CHANGES_RPC_CONTENT;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DEFAULT_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_DISCARD_CHANGES_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_ERROR_OPTION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_GET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_LOCK_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_SOURCE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_TARGET_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_UNLOCK_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_VALIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.ROLLBACK_ON_ERROR_OPTION;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toFilterStructure;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import org.opendaylight.controller.sal.core.api.RpcImplementation;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;

/**
 * Provides base operations for netconf e.g. get, get-config, edit-config, (un)lock, commit etc.
 * According to RFC-6241
 */
public final class NetconfBaseOps {

    private final RpcImplementation rpc;

    public NetconfBaseOps(final RpcImplementation rpc) {
        this.rpc = rpc;
    }

    public ListenableFuture<RpcResult<CompositeNode>> lock(final FutureCallback<RpcResult<CompositeNode>> callback, final QName datastore) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_LOCK_QNAME, getLockContent(datastore));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> lockCandidate(final FutureCallback<RpcResult<CompositeNode>> callback) {
        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_LOCK_QNAME, getLockContent(NETCONF_CANDIDATE_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }


    public ListenableFuture<RpcResult<CompositeNode>> lockRunning(final FutureCallback<RpcResult<CompositeNode>> callback) {
        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_LOCK_QNAME, getLockContent(NETCONF_RUNNING_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> unlock(final FutureCallback<RpcResult<CompositeNode>> callback, final QName datastore) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_UNLOCK_QNAME, getUnLockContent(datastore));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> unlockRunning(final FutureCallback<RpcResult<CompositeNode>> callback) {
        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_UNLOCK_QNAME, getUnLockContent(NETCONF_RUNNING_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> unlockCandidate(final FutureCallback<RpcResult<CompositeNode>> callback) {
        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_UNLOCK_QNAME, getUnLockContent(NETCONF_CANDIDATE_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> discardChanges(final FutureCallback<RpcResult<CompositeNode>> callback) {
        Preconditions.checkNotNull(callback);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_DISCARD_CHANGES_QNAME, DISCARD_CHANGES_RPC_CONTENT);
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> commit(final FutureCallback<RpcResult<CompositeNode>> callback) {
        Preconditions.checkNotNull(callback);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME, NetconfMessageTransformUtil.COMMIT_RPC_CONTENT);
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> validate(final FutureCallback<RpcResult<CompositeNode>> callback, final QName datastore) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NetconfMessageTransformUtil.NETCONF_VALIDATE_QNAME, getValidateContent(datastore));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> validateCandidate(final FutureCallback<RpcResult<CompositeNode>> callback) {
        return validate(callback, NETCONF_CANDIDATE_QNAME);
    }


    public ListenableFuture<RpcResult<CompositeNode>> validateRunning(final FutureCallback<RpcResult<CompositeNode>> callback) {
        return validate(callback, NETCONF_RUNNING_QNAME);
    }

    public ListenableFuture<RpcResult<CompositeNode>> copyConfig(final FutureCallback<RpcResult<CompositeNode>> callback, final QName source, final QName target) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(target);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_QNAME, getCopyConfigContent(source, target));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> copyRunningToCandidate(final FutureCallback<RpcResult<CompositeNode>> callback) {
        return copyConfig(callback, NETCONF_RUNNING_QNAME, NETCONF_CANDIDATE_QNAME);
    }

    public ListenableFuture<RpcResult<CompositeNode>> getConfig(final FutureCallback<RpcResult<CompositeNode>> callback, final QName datastore, final Optional<YangInstanceIdentifier> filterPath) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<RpcResult<CompositeNode>> future;
        if (filterPath.isPresent()) {
            final Node<?> node = toFilterStructure(filterPath.get());
            future = rpc.invokeRpc(NETCONF_GET_CONFIG_QNAME,
                            NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, getSourceNode(datastore), node));
        } else {
            future = rpc.invokeRpc(NETCONF_GET_CONFIG_QNAME,
                            NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, getSourceNode(datastore)));
        }

        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> getConfigRunning(final FutureCallback<RpcResult<CompositeNode>> callback, final Optional<YangInstanceIdentifier> filterPath) {
        return getConfig(callback, NETCONF_RUNNING_QNAME, filterPath);
    }

    public ListenableFuture<RpcResult<CompositeNode>> getConfigCandidate(final FutureCallback<RpcResult<CompositeNode>> callback, final Optional<YangInstanceIdentifier> filterPath) {
        return getConfig(callback, NETCONF_CANDIDATE_QNAME, filterPath);
    }

    public ListenableFuture<RpcResult<CompositeNode>> get(final FutureCallback<RpcResult<CompositeNode>> callback, final QName datastore, final Optional<YangInstanceIdentifier> filterPath) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<RpcResult<CompositeNode>> future;
        if (filterPath.isPresent()) {
            final Node<?> node = toFilterStructure(filterPath.get());
            future = rpc.invokeRpc(NETCONF_GET_QNAME,
                            NetconfMessageTransformUtil.wrap(NETCONF_GET_QNAME, getSourceNode(datastore), node));
        } else {
            future = rpc.invokeRpc(NETCONF_GET_QNAME,
                            NetconfMessageTransformUtil.wrap(NETCONF_GET_QNAME, getSourceNode(datastore)));
        }

        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<RpcResult<CompositeNode>> getRunning(final FutureCallback<RpcResult<CompositeNode>> callback, final Optional<YangInstanceIdentifier> filterPath) {
        return get(callback, NETCONF_RUNNING_QNAME, filterPath);
    }

    public ListenableFuture<RpcResult<CompositeNode>> getCandidate(final FutureCallback<RpcResult<CompositeNode>> callback, final Optional<YangInstanceIdentifier> filterPath) {
        return get(callback, NETCONF_CANDIDATE_QNAME, filterPath);
    }


    public ListenableFuture<RpcResult<CompositeNode>> editConfigCandidate(final FutureCallback<? super RpcResult<CompositeNode>> callback, final CompositeNode editStructure, final ModifyAction modifyAction, final boolean rollback) {
        return editConfig(callback, NETCONF_CANDIDATE_QNAME, editStructure, Optional.of(modifyAction), rollback);
    }

    public ListenableFuture<RpcResult<CompositeNode>> editConfigCandidate(final FutureCallback<? super RpcResult<CompositeNode>> callback, final CompositeNode editStructure, final boolean rollback) {
        return editConfig(callback, NETCONF_CANDIDATE_QNAME, editStructure, Optional.<ModifyAction>absent(), rollback);
    }

    public ListenableFuture<RpcResult<CompositeNode>> editConfigRunning(final FutureCallback<? super RpcResult<CompositeNode>> callback, final CompositeNode editStructure, final ModifyAction modifyAction, final boolean rollback) {
        return editConfig(callback, NETCONF_RUNNING_QNAME, editStructure, Optional.of(modifyAction), rollback);
    }

    public ListenableFuture<RpcResult<CompositeNode>> editConfigRunning(final FutureCallback<? super RpcResult<CompositeNode>> callback, final CompositeNode editStructure, final boolean rollback) {
        return editConfig(callback, NETCONF_RUNNING_QNAME, editStructure, Optional.<ModifyAction>absent(), rollback);
    }

    public ListenableFuture<RpcResult<CompositeNode>> editConfig(final FutureCallback<? super RpcResult<CompositeNode>> callback, final QName datastore, final CompositeNode editStructure, final Optional<ModifyAction> modifyAction, final boolean rollback) {
        Preconditions.checkNotNull(editStructure);
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<RpcResult<CompositeNode>> future = rpc.invokeRpc(NETCONF_EDIT_CONFIG_QNAME, getEditConfigContent(datastore, editStructure, modifyAction, rollback));

        Futures.addCallback(future, callback);
        return future;
    }

    private CompositeNode getEditConfigContent(final QName datastore, final CompositeNode editStructure, final Optional<ModifyAction> defaultOperation, final boolean rollback) {
        final CompositeNodeBuilder<ImmutableCompositeNode> ret = ImmutableCompositeNode.builder();

        // Target
        ret.add(getTargetNode(datastore));

        // Default operation
        if(defaultOperation.isPresent()) {
            final SimpleNode<String> defOp = NodeFactory.createImmutableSimpleNode(NETCONF_DEFAULT_OPERATION_QNAME, null, NetconfMessageTransformUtil.modifyOperationToXmlString(defaultOperation.get()));
            ret.add(defOp);
        }

        // Error option
        if(rollback) {
            ret.addLeaf(NETCONF_ERROR_OPTION_QNAME, ROLLBACK_ON_ERROR_OPTION);
        }

        ret.setQName(NETCONF_EDIT_CONFIG_QNAME);
        // Edit content
        ret.add(editStructure);
        return ret.build();
    }

    private static CompositeNode getSourceNode(final QName datastore) {
        return NodeFactory.createImmutableCompositeNode(NETCONF_SOURCE_QNAME, null,
                Collections.<Node<?>> singletonList(new SimpleNodeTOImpl<>(datastore, null, null)));
    }


    public static CompositeNode getLockContent(final QName datastore) {
        return NodeFactory.createImmutableCompositeNode(NETCONF_LOCK_QNAME, null, Collections.<Node<?>>singletonList(
                getTargetNode(datastore)));
    }

    private static CompositeNode getTargetNode(final QName datastore) {
        return NodeFactory.createImmutableCompositeNode(NETCONF_TARGET_QNAME, null, Collections.<Node<?>>singletonList(
                NodeFactory.createImmutableSimpleNode(datastore, null, null)
        ));
    }

    public static CompositeNode getCopyConfigContent(final QName source, final QName target) {
        return NodeFactory.createImmutableCompositeNode(NETCONF_LOCK_QNAME, null,
                Lists.<Node<?>> newArrayList(getTargetNode(target), getSourceNode(source)));
    }

    public static CompositeNode getValidateContent(final QName source) {
        return NodeFactory.createImmutableCompositeNode(NETCONF_VALIDATE_QNAME, null, Lists.<Node<?>> newArrayList(getSourceNode(source)));
    }

    public static CompositeNode getUnLockContent(final QName preferedDatastore) {
        return NodeFactory.createImmutableCompositeNode(NETCONF_UNLOCK_QNAME, null, Collections.<Node<?>>singletonList(
                getTargetNode(preferedDatastore)));
    }

}
