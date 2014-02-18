/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf

import com.google.common.base.Optional
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import java.net.URI
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Set
import java.util.concurrent.atomic.AtomicInteger
import org.opendaylight.controller.netconf.api.NetconfMessage
import org.opendaylight.controller.sal.common.util.Rpcs
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.RpcResult
import org.opendaylight.yangtools.yang.data.api.CompositeNode
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument
import org.opendaylight.yangtools.yang.data.api.Node
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import org.w3c.dom.Document
import org.w3c.dom.Element

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
    public static val NETCONF_EDIT_CONFIG_QNAME = QName.create(NETCONF_QNAME, "edit-config");
    public static val NETCONF_DELETE_CONFIG_QNAME = QName.create(NETCONF_QNAME, "delete-config");
    public static val NETCONF_OPERATION_QNAME = QName.create(NETCONF_QNAME, "operation");
    public static val NETCONF_COMMIT_QNAME = QName.create(NETCONF_QNAME, "commit");
    
    public static val NETCONF_CONFIG_QNAME = QName.create(NETCONF_QNAME, "config");
    public static val NETCONF_SOURCE_QNAME = QName.create(NETCONF_QNAME, "source");
    public static val NETCONF_TARGET_QNAME = QName.create(NETCONF_QNAME, "target");
    
    public static val NETCONF_CANDIDATE_QNAME = QName.create(NETCONF_QNAME, "candidate");
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
        if (node != null) {
            list.add(node);
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

    static def CompositeNode toCompositeNode(NetconfMessage message,Optional<SchemaContext> ctx) {
        //TODO: implement general normalization to normalize incoming Netconf Message 
        // for Schema Context counterpart
        return null
    }
    
    static def CompositeNode toNotificationNode(NetconfMessage message,Optional<SchemaContext> ctx) {
        if (ctx.present) {
            val schemaContext = ctx.get
            val notifications = schemaContext.notifications
            val document = message.document
            return XmlDocumentUtils.notificationToDomNodes(document, Optional.<Set<NotificationDefinition>>fromNullable(notifications))
        }
        return null
    }

    static def NetconfMessage toRpcMessage(QName rpc, CompositeNode node,Optional<SchemaContext> ctx) {
        val rpcPayload = wrap(NETCONF_RPC_QNAME, flattenInput(node))
        val w3cPayload = XmlDocumentUtils.toDocument(rpcPayload, XmlDocumentUtils.defaultValueCodecProvider)
        w3cPayload.documentElement.setAttribute("message-id", "m-" + messageId.andIncrement)
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

    static def RpcResult<CompositeNode> toRpcResult(NetconfMessage message,QName rpc,Optional<SchemaContext> context) {
        var CompositeNode rawRpc;
        if(context.present) {
            if(isDataRetrievalReply(rpc)) {
                
                val xmlData = message.document.dataSubtree
                val dataNodes = XmlDocumentUtils.toDomNodes(xmlData, Optional.of(context.get.dataDefinitions))
                
                val it = ImmutableCompositeNode.builder()
                setQName(NETCONF_RPC_REPLY_QNAME)
                add(ImmutableCompositeNode.create(NETCONF_DATA_QNAME, dataNodes));
                
                rawRpc = it.toInstance;
                //sys(xmlData)
            } else {
                val rpcSchema = context.get.operations.findFirst[QName == rpc]
                rawRpc = message.document.toCompositeNode() as CompositeNode;
            }
            
            
            
        } else {
            rawRpc = message.document.toCompositeNode() as CompositeNode;
        }
        //rawRpc.
        return Rpcs.getRpcResult(true, rawRpc, Collections.emptySet());
    }
    
    def static Element getDataSubtree(Document doc) {
        doc.getElementsByTagNameNS(NETCONF_URI.toString,"data").item(0) as Element
    }
    
    def static boolean isDataRetrievalReply(QName it) {
        return NETCONF_URI == namespace && ( localName == NETCONF_GET_CONFIG_QNAME.localName || localName == NETCONF_GET_QNAME.localName) 
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
        return XmlDocumentUtils.toDomNode(document) as Node<?>
    }
    
    public static def checkValidReply(NetconfMessage input, NetconfMessage output) {
        val inputMsgId = input.document.documentElement.getAttribute("message-id")
        val outputMsgId = output.document.documentElement.getAttribute("message-id")
        Preconditions.checkState(inputMsgId == outputMsgId,"Rpc request and reply message IDs must be same.");
        
    }
    
}
