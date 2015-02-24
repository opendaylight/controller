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
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_QNAME;
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
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toId;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.DataContainerNodeAttrBuilder;

/**
 * Provides base operations for netconf e.g. get, get-config, edit-config, (un)lock, commit etc.
 * According to RFC-6241
 */
public final class NetconfBaseOps {

    private final DOMRpcService rpc;

    public NetconfBaseOps(final DOMRpcService rpc) {
        this.rpc = rpc;
    }

    public ListenableFuture<DOMRpcResult> lock(final FutureCallback<DOMRpcResult> callback, final QName datastore) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_LOCK_QNAME), getLockContent(datastore));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> lockCandidate(final FutureCallback<DOMRpcResult> callback) {
        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_LOCK_QNAME), getLockContent(NETCONF_CANDIDATE_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }


    public ListenableFuture<DOMRpcResult> lockRunning(final FutureCallback<DOMRpcResult> callback) {
        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_LOCK_QNAME), getLockContent(NETCONF_RUNNING_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> unlock(final FutureCallback<DOMRpcResult> callback, final QName datastore) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_UNLOCK_QNAME), getUnLockContent(datastore));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> unlockRunning(final FutureCallback<DOMRpcResult> callback) {
        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_UNLOCK_QNAME), getUnLockContent(NETCONF_RUNNING_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> unlockCandidate(final FutureCallback<DOMRpcResult> callback) {
        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_UNLOCK_QNAME), getUnLockContent(NETCONF_CANDIDATE_QNAME));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> discardChanges(final FutureCallback<DOMRpcResult> callback) {
        Preconditions.checkNotNull(callback);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_DISCARD_CHANGES_QNAME), DISCARD_CHANGES_RPC_CONTENT);
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> commit(final FutureCallback<DOMRpcResult> callback) {
        Preconditions.checkNotNull(callback);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_COMMIT_QNAME), NetconfMessageTransformUtil.COMMIT_RPC_CONTENT);
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> validate(final FutureCallback<DOMRpcResult> callback, final QName datastore) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_VALIDATE_QNAME), getValidateContent(datastore));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> validateCandidate(final FutureCallback<DOMRpcResult> callback) {
        return validate(callback, NETCONF_CANDIDATE_QNAME);
    }


    public ListenableFuture<DOMRpcResult> validateRunning(final FutureCallback<DOMRpcResult> callback) {
        return validate(callback, NETCONF_RUNNING_QNAME);
    }

    public ListenableFuture<DOMRpcResult> copyConfig(final FutureCallback<DOMRpcResult> callback, final QName source, final QName target) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(target);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NetconfMessageTransformUtil.NETCONF_COPY_CONFIG_QNAME), getCopyConfigContent(source, target));
        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> copyRunningToCandidate(final FutureCallback<DOMRpcResult> callback) {
        return copyConfig(callback, NETCONF_RUNNING_QNAME, NETCONF_CANDIDATE_QNAME);
    }

    public ListenableFuture<DOMRpcResult> getConfig(final FutureCallback<DOMRpcResult> callback, final QName datastore, final Optional<YangInstanceIdentifier> filterPath) {
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<DOMRpcResult> future;
        if (filterPath.isPresent()) {
            // FIXME the source node has to be wrapped in a choice
            final DataContainerChild<?, ?> node = toFilterStructure(filterPath.get());
            future = rpc.invokeRpc(toPath(NETCONF_GET_CONFIG_QNAME),
                            NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, getSourceNode(datastore), node));
        } else {
            future = rpc.invokeRpc(toPath(NETCONF_GET_CONFIG_QNAME),
                            NetconfMessageTransformUtil.wrap(NETCONF_GET_CONFIG_QNAME, getSourceNode(datastore)));
        }

        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> getConfigRunning(final FutureCallback<DOMRpcResult> callback, final Optional<YangInstanceIdentifier> filterPath) {
        return getConfig(callback, NETCONF_RUNNING_QNAME, filterPath);
    }

    public ListenableFuture<DOMRpcResult> getConfigCandidate(final FutureCallback<DOMRpcResult> callback, final Optional<YangInstanceIdentifier> filterPath) {
        return getConfig(callback, NETCONF_CANDIDATE_QNAME, filterPath);
    }

    public ListenableFuture<DOMRpcResult> get(final FutureCallback<DOMRpcResult> callback, final Optional<YangInstanceIdentifier> filterPath) {
        Preconditions.checkNotNull(callback);

        final ListenableFuture<DOMRpcResult> future;
        final DataContainerChild<?, ?> node =
                filterPath.isPresent() ? toFilterStructure(filterPath.get()) : NetconfMessageTransformUtil.GET_RPC_CONTENT;
        future = rpc.invokeRpc(toPath(NETCONF_GET_QNAME), NetconfMessageTransformUtil.wrap(NETCONF_GET_QNAME, node));

        Futures.addCallback(future, callback);
        return future;
    }

    public ListenableFuture<DOMRpcResult> editConfigCandidate(final FutureCallback<? super DOMRpcResult> callback, final DataContainerChild<?, ?> editStructure, final ModifyAction modifyAction, final boolean rollback) {
        return editConfig(callback, NETCONF_CANDIDATE_QNAME, editStructure, Optional.of(modifyAction), rollback);
    }

    public ListenableFuture<DOMRpcResult> editConfigCandidate(final FutureCallback<? super DOMRpcResult> callback, final DataContainerChild<?, ?> editStructure, final boolean rollback) {
        return editConfig(callback, NETCONF_CANDIDATE_QNAME, editStructure, Optional.<ModifyAction>absent(), rollback);
    }

    public ListenableFuture<DOMRpcResult> editConfigRunning(final FutureCallback<? super DOMRpcResult> callback, final DataContainerChild<?, ?> editStructure, final ModifyAction modifyAction, final boolean rollback) {
        return editConfig(callback, NETCONF_RUNNING_QNAME, editStructure, Optional.of(modifyAction), rollback);
    }

    public ListenableFuture<DOMRpcResult> editConfigRunning(final FutureCallback<? super DOMRpcResult> callback, final DataContainerChild<?, ?> editStructure, final boolean rollback) {
        return editConfig(callback, NETCONF_RUNNING_QNAME, editStructure, Optional.<ModifyAction>absent(), rollback);
    }

    public ListenableFuture<DOMRpcResult> editConfig(final FutureCallback<? super DOMRpcResult> callback, final QName datastore, final DataContainerChild<?, ?> editStructure, final Optional<ModifyAction> modifyAction, final boolean rollback) {
        Preconditions.checkNotNull(editStructure);
        Preconditions.checkNotNull(callback);
        Preconditions.checkNotNull(datastore);

        final ListenableFuture<DOMRpcResult> future = rpc.invokeRpc(toPath(NETCONF_EDIT_CONFIG_QNAME), getEditConfigContent(datastore, editStructure, modifyAction, rollback));

        Futures.addCallback(future, callback);
        return future;
    }

    private ContainerNode getEditConfigContent(final QName datastore, final DataContainerChild<?, ?> editStructure, final Optional<ModifyAction> defaultOperation, final boolean rollback) {
        final DataContainerNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> editBuilder = Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_EDIT_CONFIG_QNAME));

        // Target
        editBuilder.withChild(getTargetNode(datastore));

        // Default operation
        if(defaultOperation.isPresent()) {
            editBuilder.withChild(Builders.leafBuilder().withNodeIdentifier(toId(NETCONF_DEFAULT_OPERATION_QNAME)).withValue(NetconfMessageTransformUtil.modifyOperationToXmlString(defaultOperation.get())).build());
        }

        // Error option
        if(rollback) {
            editBuilder.withChild(Builders.leafBuilder().withNodeIdentifier(toId(NETCONF_ERROR_OPTION_QNAME)).withValue(ROLLBACK_ON_ERROR_OPTION).build());
        }

        // Edit content
        editBuilder.withChild(editStructure);
        return editBuilder.build();
    }

    private static DataContainerChild<?, ?> getSourceNode(final QName datastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_SOURCE_QNAME))
                .withChild(Builders.leafBuilder().withNodeIdentifier(toId(datastore)).build()).build();
    }

    public static NormalizedNode<?, ?> getLockContent(final QName datastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_LOCK_QNAME))
                .withChild(getTargetNode(datastore)).build();
    }

    private static DataContainerChild<?, ?> getTargetNode(final QName datastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_TARGET_QNAME))
                .withChild(Builders.leafBuilder().withNodeIdentifier(toId(datastore)).build()).build();
    }

    public static NormalizedNode<?, ?> getCopyConfigContent(final QName source, final QName target) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_COPY_CONFIG_QNAME))
                .withChild(getTargetNode(target)).withChild(getSourceNode(source)).build();
    }

    public static NormalizedNode<?, ?> getValidateContent(final QName source) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_VALIDATE_QNAME))
                .withChild(getSourceNode(source)).build();
    }

    public static NormalizedNode<?, ?> getUnLockContent(final QName datastore) {
        return Builders.containerBuilder().withNodeIdentifier(toId(NETCONF_UNLOCK_QNAME))
                .withChild(getTargetNode(datastore)).build();
    }

}
