package org.opendaylight.controller.sal.connect.netconf

import org.opendaylight.controller.netconf.api.NetconfMessage
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import java.net.URI
import java.util.Collections
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.NodeUtils
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import java.util.ArrayList
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import java.util.concurrent.atomic.AtomicInteger
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.opendaylight.controller.sal.common.util.Rpcs

class NetconfMapping {

    public static val NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0")
    public static val NETCONF_QNAME = new QName(NETCONF_URI,null,"netconf");
    public static val NETCONF_RPC_QNAME = new QName(NETCONF_QNAME,"rpc");
    public static val NETCONF_GET_QNAME = new QName(NETCONF_QNAME,"get");
    public static val NETCONF_GET_CONFIG_QNAME = new QName(NETCONF_QNAME,"get-config");
    public static val NETCONF_RPC_REPLY_QNAME = new QName(NETCONF_QNAME,"rpc-reply");
    public static val NETCONF_OK_QNAME = new QName(NETCONF_QNAME,"ok");
    public static val NETCONF_DATA_QNAME = new QName(NETCONF_QNAME,"data");
    

    static val messageId = new AtomicInteger(0);



    static def Node<?> toFilterStructure(InstanceIdentifier identifier) {
        var Node<?> previous = null;
        for (component : identifier.path.reverse) {
            val Node<?> current = component.toNode(previous);
            previous = current;
        }
        return previous;
    }
    
    static def dispatch Node<?> toNode(NodeIdentifierWithPredicates argument, Node<?> node) {
        val list = new ArrayList<Node<?>>();
        for( arg : argument.keyValues.entrySet) {
            list.add = new SimpleNodeTOImpl(arg.key,null,arg.value);
        }
        return new CompositeNodeTOImpl(argument.nodeType,null,list)
    }
    
    static def dispatch Node<?> toNode(PathArgument argument, Node<?> node) {
        if(node != null) {
            return new CompositeNodeTOImpl(argument.nodeType,null,Collections.singletonList(node));
        } else {
            return new SimpleNodeTOImpl(argument.nodeType,null,null);
        }
    }

    static def CompositeNode toCompositeNode(NetconfMessage message) {
        return message.toRpcResult().result;
    }

    static def NetconfMessage toRpcMessage(QName rpc, CompositeNode node) {
        val rpcPayload = wrap(NETCONF_RPC_QNAME,node);
        val w3cPayload = NodeUtils.buildShadowDomTree(rpcPayload);
        w3cPayload.documentElement.setAttribute("message-id","m-"+ messageId.andIncrement);
        return new NetconfMessage(w3cPayload);
    }

    static def RpcResult<CompositeNode> toRpcResult(NetconfMessage message) {
        val rawRpc = message.document.toCompositeNode() as CompositeNode;
        //rawRpc.
        
        return Rpcs.getRpcResult(true,rawRpc,Collections.emptySet());
    }
    
    
    static def wrap(QName name,Node<?> node) {
        if(node != null) {
            return new CompositeNodeTOImpl(name,null,Collections.singletonList(node));
        }
        else {
            return new CompositeNodeTOImpl(name,null,Collections.emptyList());
        }
    }
    
    
    public static def Node<?> toCompositeNode(Document document) {
        return XmlDocumentUtils.toCompositeNode(document) as Node<?>
    }
}
