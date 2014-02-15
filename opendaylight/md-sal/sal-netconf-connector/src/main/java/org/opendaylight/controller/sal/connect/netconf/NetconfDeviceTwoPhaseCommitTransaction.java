/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_CANDIDATE_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_COMMIT_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_EDIT_CONFIG_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_OPERATION_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_RUNNING_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.NETCONF_TARGET_QNAME;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class NetconfDeviceTwoPhaseCommitTransaction implements DataCommitTransaction<InstanceIdentifier, CompositeNode> {

    private final NetconfDevice device;
    private final DataModification<InstanceIdentifier, CompositeNode> modification;
    private final boolean candidateSupported = true;

    public NetconfDeviceTwoPhaseCommitTransaction(NetconfDevice device,
            DataModification<InstanceIdentifier, CompositeNode> modification) {
        super();
        this.device = device;
        this.modification = modification;
    }

    public void prepare() {
        for (InstanceIdentifier toRemove : modification.getRemovedConfigurationData()) {
            sendDelete(toRemove);
        }
        for(Entry<InstanceIdentifier, CompositeNode> toUpdate : modification.getUpdatedConfigurationData().entrySet()) {
            sendMerge(toUpdate.getKey(),toUpdate.getValue());
        }

    }

    private void sendMerge(InstanceIdentifier key, CompositeNode value) {
        sendEditRpc(createEditStructure(key, Optional.<String>absent(), Optional.of(value)));
    }

    private void sendDelete(InstanceIdentifier toDelete) {
        sendEditRpc(createEditStructure(toDelete, Optional.of("delete"), Optional.<CompositeNode> absent()));
    }

    private void sendEditRpc(CompositeNode editStructure) {
        CompositeNodeBuilder<ImmutableCompositeNode> builder = configurationRpcBuilder();
        builder.setQName(NETCONF_EDIT_CONFIG_QNAME);
        builder.add(editStructure);

        RpcResult<CompositeNode> rpcResult = device.invokeRpc(NETCONF_EDIT_CONFIG_QNAME, builder.toInstance());
        Preconditions.checkState(rpcResult.isSuccessful(),"Rpc Result was unsuccessful");

    }

    private CompositeNodeBuilder<ImmutableCompositeNode> configurationRpcBuilder() {
        CompositeNodeBuilder<ImmutableCompositeNode> ret = ImmutableCompositeNode.builder();

        Node<?> targetNode;
        if(candidateSupported) {
            targetNode = ImmutableCompositeNode.create(NETCONF_CANDIDATE_QNAME, ImmutableList.<Node<?>>of());
        } else {
            targetNode = ImmutableCompositeNode.create(NETCONF_RUNNING_QNAME, ImmutableList.<Node<?>>of());
        }
        Node<?> targetWrapperNode = ImmutableCompositeNode.create(NETCONF_TARGET_QNAME, ImmutableList.<Node<?>>of(targetNode));
        ret.add(targetWrapperNode);
        return ret;
    }

    private CompositeNode createEditStructure(InstanceIdentifier dataPath, Optional<String> operation,
            Optional<CompositeNode> lastChildOverride) {
        List<PathArgument> path = dataPath.getPath();
        List<PathArgument> reversed = Lists.reverse(path);
        CompositeNode previous = null;
        boolean isLast = true;
        for (PathArgument arg : reversed) {
            CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
            builder.setQName(arg.getNodeType());
            Map<QName, Object> predicates = Collections.emptyMap();
            if (arg instanceof NodeIdentifierWithPredicates) {
                predicates = ((NodeIdentifierWithPredicates) arg).getKeyValues();
            }
            for (Entry<QName, Object> entry : predicates.entrySet()) {
                builder.addLeaf(entry.getKey(), entry.getValue());
            }

            if (isLast) {
                if (operation.isPresent()) {
                    builder.setAttribute(NETCONF_OPERATION_QNAME, operation.get());
                }
                if (lastChildOverride.isPresent()) {
                    List<Node<?>> children = lastChildOverride.get().getChildren();
                    for(Node<?> child : children) {
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
        CompositeNodeBuilder<ImmutableCompositeNode> commitInput = ImmutableCompositeNode.builder();
        commitInput.setQName(NETCONF_COMMIT_QNAME);
        RpcResult<?> rpcResult = device.invokeRpc(NetconfMapping.NETCONF_COMMIT_QNAME, commitInput.toInstance());
        return (RpcResult<Void>) rpcResult;
    }

    @Override
    public DataModification<InstanceIdentifier, CompositeNode> getModification() {
        return this.modification;
    }

    @Override
    public RpcResult<Void> rollback() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }
}
