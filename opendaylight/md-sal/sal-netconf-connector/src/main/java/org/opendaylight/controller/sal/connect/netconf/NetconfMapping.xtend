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
import java.util.List
import com.google.common.collect.ImmutableList
import org.opendaylight.yangtools.yang.data.api.SimpleNode
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode

class NetconfMapping {

    public static val NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0")
    public static val NETCONF_MONITORING_URI = "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring"
    public static val NETCONF_NOTIFICATION_URI = URI.create("urn:ietf:params:xml:ns:netconf:notification:1.0")
    
    
    public static val NETCONF_QNAME = QName.create(NETCONF_URI, null, "netconf");
    public static val NETCONF_RPC_QNAME = QName.create(NETCONF_QNAME, "rpc");
    public static val NETCONF_GET_QNAME = QName.create(NETCONF_QNAME, "get");
    public static val NETCONF_FILTER_QNAME = QName.create(NETCONF_QNAME, "filter");
    public static val NETCONF_TYPE_QNAME = QName.create(NETCONF_QNAME, "type");
    public static val NETCONF_GET_CONFIG_QNAME = QName.create(NETCONF_QNAME, "get-config");
    public static val NETCONF_SOURCE_QNAME = QName.create(NETCONF_QNAME, "source");
    public static val NETCONF_RUNNING_QNAME = QName.create(NETCONF_QNAME, "running");
    public static val NETCONF_RPC_REPLY_QNAME = QName.create(NETCONF_QNAME, "rpc-reply");
    public static val NETCONF_OK_QNAME = QName.create(NETCONF_QNAME, "ok");
    public static val NETCONF_DATA_QNAME = QName.create(NETCONF_QNAME, "data");
    public static val NETCONF_CREATE_SUBSCRIPTION_QNAME = QName.create(NETCONF_NOTIFICATION_URI,null,"create-subscription");
    public static val NETCONF_CANCEL_SUBSCRIPTION_QNAME = QName.create(NETCONF_NOTIFICATION_URI,null,"cancel-subscription");
    public static val IETF_NETCONF_MONITORING_MODULE = QName.create(NETCONF_MONITORING_URI, "2010-10-04","ietf-netconf-monitoring");

    static List<Node<?>> RUNNING = Collections.<Node<?>>singletonList(
        new SimpleNodeTOImpl(NETCONF_RUNNING_QNAME, null, null));
    public static val CONFIG_SOURCE_RUNNING = new CompositeNodeTOImpl(NETCONF_SOURCE_QNAME, null, RUNNING);

    static val messageId = new AtomicInteger(0);

    static def Node<?> toFilterStructure(InstanceIdentifier identifier) {
        var Node<?> previous = null;
        if(identifier.path.empty) {
            return null;
        }
        
        for (component : identifier.path.reverseView) {
            val Node<?> current = component.toNode(previous);
            previous = current;
        }
        return filter("subtree",previous);
    }

    static def dispatch Node<?> toNode(NodeIdentifierWithPredicates argument, Node<?> node) {
        val list = new ArrayList<Node<?>>();
        for (arg : argument.keyValues.entrySet) {
            list.add = new SimpleNodeTOImpl(arg.key, null, arg.value);
        }
        return new CompositeNodeTOImpl(argument.nodeType, null, list)
    }

    static def dispatch Node<?> toNode(PathArgument argument, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(argument.nodeType, null, Collections.singletonList(node));
        } else {
            return new SimpleNodeTOImpl(argument.nodeType, null, null);
        }
    }

    static def CompositeNode toCompositeNode(NetconfMessage message) {
        return message.toRpcResult().result;
    }

    static def NetconfMessage toRpcMessage(QName rpc, CompositeNode node) {
        val rpcPayload = wrap(NETCONF_RPC_QNAME, flattenInput(node));
        val w3cPayload = NodeUtils.buildShadowDomTree(rpcPayload);
        w3cPayload.documentElement.setAttribute("message-id", "m-" + messageId.andIncrement);
        return new NetconfMessage(w3cPayload);
    }
    
    def static flattenInput(CompositeNode node) {
        val inputQName = QName.create(node.nodeType,"input");
        val input = node.getFirstCompositeByName(inputQName);
        if(input == null) return node;
        if(input instanceof CompositeNode) {
            
            val nodes = ImmutableList.builder() //
                .addAll(input.children) //
                .addAll(node.children.filter[nodeType != inputQName]) //
                .build()
            return ImmutableCompositeNode.create(node.nodeType,nodes);
        } 
        
    }

    static def RpcResult<CompositeNode> toRpcResult(NetconfMessage message) {
        val rawRpc = message.document.toCompositeNode() as CompositeNode;

        //rawRpc.
        return Rpcs.getRpcResult(true, rawRpc, Collections.emptySet());
    }

    static def wrap(QName name, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, Collections.singletonList(node));
        } else {
            return new CompositeNodeTOImpl(name, null, Collections.emptyList());
        }
    }

    static def wrap(QName name, Node<?> additional, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, ImmutableList.of(additional, node));
        } else {
            return new CompositeNodeTOImpl(name, null, ImmutableList.of(additional));
        }
    }

    static def filter(String type, Node<?> node) {
        val it = ImmutableCompositeNode.builder(); //
        setQName(NETCONF_FILTER_QNAME);
        setAttribute(NETCONF_TYPE_QNAME,type);
        if (node != null) {
            return add(node).toInstance();
        } else {
            return toInstance();
        }
    }

    public static def Node<?> toCompositeNode(Document document) {
        return XmlDocumentUtils.toNode(document) as Node<?>
    }
}
