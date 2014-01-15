package org.opendaylight.controller.sal.connect.netconf;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.md.sal.common.api.data.DataCommitHandler.DataCommitTransaction;
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
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import static org.opendaylight.controller.sal.connect.netconf.NetconfMapping.*;

public class NetconfDeviceTwoPhaseCommitTransaction implements DataCommitTransaction<InstanceIdentifier, CompositeNode> {

    private NetconfDevice device;
    private final DataModification<InstanceIdentifier, CompositeNode> modification;
    private boolean candidateSupported = true;

    public NetconfDeviceTwoPhaseCommitTransaction(NetconfDevice device,
            DataModification<InstanceIdentifier, CompositeNode> modification) {
        super();
        this.device = device;
        this.modification = modification;
    }

    public void prepare() {
        for (InstanceIdentifier toRemove : modification.getRemovedConfigurationData()) {
            sendRemove(toRemove);
        }
        for(Entry<InstanceIdentifier, CompositeNode> toUpdate : modification.getUpdatedConfigurationData().entrySet()) {
            sendMerge(toUpdate.getKey(),toUpdate.getValue());
        }

    }

    private void sendMerge(InstanceIdentifier key, CompositeNode value) {
        sendEditRpc(createEditStructure(key, Optional.<String>absent(), Optional.of(value)));
    }

    private void sendRemove(InstanceIdentifier toRemove) {
        sendEditRpc(createEditStructure(toRemove, Optional.of("remove"), Optional.<CompositeNode> absent()));
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

    private CompositeNode createEditStructure(InstanceIdentifier dataPath, Optional<String> action,
            Optional<CompositeNode> lastChildOverride) {
        List<PathArgument> path = dataPath.getPath();
        List<PathArgument> reversed = Lists.reverse(path);
        CompositeNode previous = null;
        boolean isLast = true;
        for (PathArgument arg : reversed) {
            CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
            builder.setQName(arg.getNodeType());

            if (arg instanceof NodeIdentifierWithPredicates) {
                for (Entry<QName, Object> entry : ((NodeIdentifierWithPredicates) arg).getKeyValues().entrySet()) {
                    builder.addLeaf(entry.getKey(), entry.getValue());
                }
            }
            if (isLast) {
                if (action.isPresent()) {
                    builder.setAttribute(NETCONF_ACTION_QNAME, action.get());
                }
                if (lastChildOverride.isPresent()) {
                    List<Node<?>> children = lastChildOverride.get().getChildren();
                    builder.addAll(children);
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
    public RpcResult<Void> finish() throws IllegalStateException {
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
