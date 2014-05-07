/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.activation.UnsupportedDataTypeException;
import javax.annotation.Nullable;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.NodeIdentifierWithPredicates;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfMapping {

    public static URI NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    public static String NETCONF_MONITORING_URI = "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring";
    public static URI NETCONF_NOTIFICATION_URI = URI.create("urn:ietf:params:xml:ns:netconf:notification:1.0");
    public static URI NETCONF_ROLLBACK_ON_ERROR_URI = URI.create("urn:ietf:params:netconf:capability:rollback-on-error:1.0");

    public static QName NETCONF_QNAME = QName.create(NETCONF_URI, null, "netconf");
    public static QName NETCONF_RPC_QNAME = QName.create(NETCONF_QNAME, "rpc");
    public static QName NETCONF_GET_QNAME = QName.create(NETCONF_QNAME, "get");
    public static QName NETCONF_FILTER_QNAME = QName.create(NETCONF_QNAME, "filter");
    public static QName NETCONF_TYPE_QNAME = QName.create(NETCONF_QNAME, "type");
    public static QName NETCONF_GET_CONFIG_QNAME = QName.create(NETCONF_QNAME, "get-config");
    public static QName NETCONF_EDIT_CONFIG_QNAME = QName.create(NETCONF_QNAME, "edit-config");
    public static QName NETCONF_DELETE_CONFIG_QNAME = QName.create(NETCONF_QNAME, "delete-config");
    public static QName NETCONF_OPERATION_QNAME = QName.create(NETCONF_QNAME, "operation");
    public static QName NETCONF_COMMIT_QNAME = QName.create(NETCONF_QNAME, "commit");

    public static QName NETCONF_CONFIG_QNAME = QName.create(NETCONF_QNAME, "config");
    public static QName NETCONF_SOURCE_QNAME = QName.create(NETCONF_QNAME, "source");
    public static QName NETCONF_TARGET_QNAME = QName.create(NETCONF_QNAME, "target");

    public static QName NETCONF_CANDIDATE_QNAME = QName.create(NETCONF_QNAME, "candidate");
    public static QName NETCONF_RUNNING_QNAME = QName.create(NETCONF_QNAME, "running");

    public static QName NETCONF_ERROR_OPTION_QNAME = QName.create(NETCONF_QNAME, "error-option");
    public static String ROLLBACK_ON_ERROR_OPTION = "rollback-on-error";

    public static QName NETCONF_RPC_REPLY_QNAME = QName.create(NETCONF_QNAME, "rpc-reply");
    public static QName NETCONF_OK_QNAME = QName.create(NETCONF_QNAME, "ok");
    public static QName NETCONF_DATA_QNAME = QName.create(NETCONF_QNAME, "data");
    public static QName NETCONF_CREATE_SUBSCRIPTION_QNAME = QName.create(NETCONF_NOTIFICATION_URI, null,
            "create-subscription");
    public static QName NETCONF_CANCEL_SUBSCRIPTION_QNAME = QName.create(NETCONF_NOTIFICATION_URI, null,
            "cancel-subscription");
    public static QName IETF_NETCONF_MONITORING_MODULE = QName.create(NETCONF_MONITORING_URI, "2010-10-04",
            "ietf-netconf-monitoring");

    static List<Node<?>> RUNNING = Collections.<Node<?>> singletonList(new SimpleNodeTOImpl(NETCONF_RUNNING_QNAME,
            null, null));

    public static CompositeNode CONFIG_SOURCE_RUNNING = new CompositeNodeTOImpl(NETCONF_SOURCE_QNAME, null, RUNNING);

    static AtomicInteger messageId = new AtomicInteger(0);

    static Node<?> toFilterStructure(InstanceIdentifier identifier) {
        Node<?> previous = null;
        if (identifier.getPath().isEmpty()) {
            return null;
        }

        for (org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument component : Lists
                .reverse(identifier.getPath())) {
            if (component instanceof NodeIdentifierWithPredicates) {
                previous = toNode((NodeIdentifierWithPredicates)component, previous);
            } else {
                previous = toNode(component, previous);
            }
        }
        return filter("subtree", previous);
    }

    static Node<?> toNode(NodeIdentifierWithPredicates argument, Node<?> node) {
        List<Node<?>> list = new ArrayList<>();
        for (Map.Entry<QName, Object> arg : argument.getKeyValues().entrySet()) {
            list.add(new SimpleNodeTOImpl(arg.getKey(), null, arg.getValue()));
        }
        if (node != null) {
            list.add(node);
        }
        return new CompositeNodeTOImpl(argument.getNodeType(), null, list);
    }

    static Node<?> toNode(PathArgument argument, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(argument.getNodeType(), null, Collections.<Node<?>> singletonList(node));
        } else {
            return new SimpleNodeTOImpl(argument.getNodeType(), null, null);
        }
    }

    static CompositeNode toCompositeNode(NetconfMessage message, Optional<SchemaContext> ctx) {
        // TODO: implement general normalization to normalize incoming Netconf
        // Message
        // for Schema Context counterpart
        return null;
    }

    static CompositeNode toNotificationNode(NetconfMessage message, Optional<SchemaContext> ctx) {
        if (ctx.isPresent()) {
            SchemaContext schemaContext = ctx.get();
            Set<NotificationDefinition> notifications = schemaContext.getNotifications();
            Document document = message.getDocument();
            return XmlDocumentUtils.notificationToDomNodes(document, Optional.fromNullable(notifications), ctx.get());
        }
        return null;
    }

    static NetconfMessage toRpcMessage(QName rpc, CompositeNode node, Optional<SchemaContext> ctx) {
        CompositeNodeTOImpl rpcPayload = wrap(NETCONF_RPC_QNAME, flattenInput(node));
        Document w3cPayload = null;
        try {
            w3cPayload = XmlDocumentUtils.toDocument(rpcPayload, XmlDocumentUtils.defaultValueCodecProvider());
        } catch (UnsupportedDataTypeException e) {
            throw new IllegalArgumentException("Unable to create message", e);
        }
        w3cPayload.getDocumentElement().setAttribute("message-id", "m-" + messageId.getAndIncrement());
        return new NetconfMessage(w3cPayload);
    }

    static CompositeNode flattenInput(final CompositeNode node) {
        final QName inputQName = QName.create(node.getNodeType(), "input");
        CompositeNode input = node.getFirstCompositeByName(inputQName);
        if (input == null)
            return node;
        if (input instanceof CompositeNode) {

            List<Node<?>> nodes = ImmutableList.<Node<?>> builder() //
                    .addAll(input.getChildren()) //
                    .addAll(Collections2.filter(node.getChildren(), new Predicate<Node<?>>() {
                        @Override
                        public boolean apply(@Nullable final Node<?> input) {
                            return input.getNodeType() != inputQName;
                        }
                    })) //
                    .build();

            return ImmutableCompositeNode.create(node.getNodeType(), nodes);
        }

        return input;
    }

    static RpcResult<CompositeNode> toRpcResult(NetconfMessage message, final QName rpc, Optional<SchemaContext> context) {
        CompositeNode rawRpc;
        if (context.isPresent())
            if (isDataRetrieQNameReply(rpc)) {

                Element xmlData = getDataSubtree(message.getDocument());

                List<org.opendaylight.yangtools.yang.data.api.Node<?>> dataNodes = XmlDocumentUtils.toDomNodes(xmlData,
                        Optional.of(context.get().getDataDefinitions()));

                CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
                it.setQName(NETCONF_RPC_REPLY_QNAME);
                it.add(ImmutableCompositeNode.create(NETCONF_DATA_QNAME, dataNodes));

                rawRpc = it.toInstance();
                // sys(xmlData)
            } else {
                rawRpc = (CompositeNode) toCompositeNode(message, context);
            }
        else {
            rawRpc = (CompositeNode) toCompositeNode(message.getDocument());
        }
        // rawRpc.
        return Rpcs.getRpcResult(true, rawRpc, Collections.<RpcError> emptySet());
    }

    static Element getDataSubtree(Document doc) {
        return (Element) doc.getElementsByTagNameNS(NETCONF_URI.toString(), "data").item(0);
    }

    static boolean isDataRetrieQNameReply(QName it) {
        return NETCONF_URI == it.getNamespace()
                && (it.getLocalName() == NETCONF_GET_CONFIG_QNAME.getLocalName() || it.getLocalName() == NETCONF_GET_QNAME
                        .getLocalName());
    }

    static CompositeNodeTOImpl wrap(QName name, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, Collections.<Node<?>> singletonList(node));
        } else {
            return new CompositeNodeTOImpl(name, null, Collections.<Node<?>> emptyList());
        }
    }

    static CompositeNodeTOImpl wrap(QName name, Node<?> additional, Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, ImmutableList.of(additional, node));
        } else {
            return new CompositeNodeTOImpl(name, null, ImmutableList.<Node<?>> of(additional));
        }
    }

    static ImmutableCompositeNode filter(String type, Node<?> node) {
        CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder(); //
        it.setQName(NETCONF_FILTER_QNAME);
        it.setAttribute(NETCONF_TYPE_QNAME, type);
        if (node != null) {
            return it.add(node).toInstance();
        } else {
            return it.toInstance();
        }
    }

    public static Node<?> toCompositeNode(Document document) {
        return XmlDocumentUtils.toDomNode(document);
    }

    public static void checkValidReply(NetconfMessage input, NetconfMessage output) {
        String inputMsgId = input.getDocument().getDocumentElement().getAttribute("message-id");
        String outputMsgId = output.getDocument().getDocumentElement().getAttribute("message-id");

        if(inputMsgId.equals(outputMsgId) == false) {
            String requestXml = XmlUtil.toString(input.getDocument());
            String responseXml = XmlUtil.toString(output.getDocument());
            throw new IllegalStateException(String.format("Rpc request and reply message IDs must be same. Request: %s, response: %s", requestXml, responseXml));
        }
    }

    public static void checkSuccessReply(NetconfMessage output) throws NetconfDocumentedException {
        if(NetconfMessageUtil.isErrorMessage(output)) {
            throw new IllegalStateException(String.format("Response contains error: %s", XmlUtil.toString(output.getDocument())));
        }
    }
}
