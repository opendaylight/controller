package org.opendaylight.controller.sal.connect.netconf

import org.opendaylight.controller.sal.core.api.mount.MountProvisionInstance
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.controller.md.sal.common.api.data.DataReader
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.controller.netconf.client.NetconfClient
import org.opendaylight.controller.sal.core.api.RpcImplementation
import static extension org.opendaylight.controller.sal.connect.netconf.NetconfMapping.*
import java.net.InetSocketAddress
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.api.SimpleNode
import org.opendaylight.yangtools.yang.common.QName
import java.util.Collections
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher
import org.opendaylight.yangtools.concepts.Registration

class NetconfDevice implements DataReader<InstanceIdentifier, CompositeNode>, RpcImplementation {

    var NetconfClient client;

    @Property
    var InetSocketAddress socketAddress;

    @Property
    val MountProvisionInstance mountInstance;

    @Property
    val InstanceIdentifier path;
    
    Registration<DataReader<InstanceIdentifier,CompositeNode>> operReaderReg
    
    Registration<DataReader<InstanceIdentifier,CompositeNode>> confReaderReg
    
    public new(MountProvisionInstance mount,InstanceIdentifier path) {
        _mountInstance = mount;
        _path = path;
    }

    def start(NetconfClientDispatcher dispatcher) {
        client = new NetconfClient("sal-netconf-connector", socketAddress, dispatcher);
        
        confReaderReg = mountInstance.registerConfigurationReader(path,this);
        operReaderReg = mountInstance.registerOperationalReader(path,this);
    }

    override readConfigurationData(InstanceIdentifier path) {
        val result = invokeRpc(NETCONF_GET_CONFIG_QNAME, wrap(NETCONF_GET_CONFIG_QNAME, path.toFilterStructure()));
        val data = result.result.getFirstCompositeByName(NETCONF_DATA_QNAME);
        return data?.findNode(path) as CompositeNode;
    }

    override readOperationalData(InstanceIdentifier path) {
        val result = invokeRpc(NETCONF_GET_QNAME, wrap(NETCONF_GET_QNAME, path.toFilterStructure()));
        val data = result.result.getFirstCompositeByName(NETCONF_DATA_QNAME);
        return data?.findNode(path) as CompositeNode;
    }

    override getSupportedRpcs() {
        Collections.emptySet;
    }

    override invokeRpc(QName rpc, CompositeNode input) {
        val message = rpc.toRpcMessage(input);
        val result = client.sendMessage(message);
        return result.toRpcResult();
    }

    def Node<?> findNode(CompositeNode node, InstanceIdentifier identifier) {

        var Node<?> current = node;
        for (arg : identifier.path) {
            if (current instanceof SimpleNode<?>) {
                return null;
            } else if (current instanceof CompositeNode) {
                val currentComposite = (current as CompositeNode);

                current = currentComposite.getFirstCompositeByName(arg.nodeType);
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.nodeType);
                }
                if (current == null) {
                    return null;
                }
            }
        }
        return current;
    }
    
    public def stop() {
        confReaderReg?.close()
        operReaderReg?.close()
    }

}
