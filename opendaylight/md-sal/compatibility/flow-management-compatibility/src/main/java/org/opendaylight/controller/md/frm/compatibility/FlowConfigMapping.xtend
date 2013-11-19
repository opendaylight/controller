package org.opendaylight.controller.md.frm.compatibility

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowBuilder

import static extension org.opendaylight.controller.sal.compatibility.NodeMapping.*
import static org.opendaylight.controller.sal.compatibility.MDFlowMapping.*
import static org.opendaylight.controller.sal.compatibility.ToSalConversionsUtils.*

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.FlowKey
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.config.rev130819.flows.Flow
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem
import org.opendaylight.yangtools.yang.binding.Identifiable

class FlowConfigMapping {

    static def toConfigurationFlow(FlowConfig sourceCfg) {
        val source = flowAdded(sourceCfg.flow);
        val it = new FlowBuilder();
        instructions = source.instructions;
        cookie = source.cookie;
        hardTimeout = source.hardTimeout
        idleTimeout = source.idleTimeout
        match = source.match
        node = source.node
        key = new FlowKey(Long.parseLong(sourceCfg.name),node);
        return it.build();
    }

    static def toFlowConfig(Flow sourceCfg) {
        val flow = toFlow(sourceCfg);
        val it = new FlowConfig;
        name = String.valueOf(sourceCfg.id);
        node = sourceCfg.node.toADNode();

        return it
    }

    static def toFlowConfig(InstanceIdentifier<?> identifier) {
        val it = new FlowConfig()
        val FlowKey key = ((identifier.path.get(2) as IdentifiableItem<Flow,FlowKey>).key)
        name = String.valueOf(key.id);
        node = key.node.toADNode();

        return it;
    }

    static def boolean isFlowPath(InstanceIdentifier<?> path) {
        if(path.path.size < 2) return false;
        if (path.path.get(2) instanceof IdentifiableItem<?,?>) {
            val IdentifiableItem<?,? extends Identifiable<?>> item = path.path.get(2) as IdentifiableItem<?,? extends Identifiable<?>>;
            val Identifiable<?> key = item.key;
            if (key instanceof FlowKey) {
                return true;
            }
        }
        return false;
    }
}
