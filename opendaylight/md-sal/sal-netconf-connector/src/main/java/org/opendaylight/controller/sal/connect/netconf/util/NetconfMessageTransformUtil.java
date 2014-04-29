/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.Map;
import javax.annotation.Nullable;

import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class NetconfMessageTransformUtil {

    private NetconfMessageTransformUtil() {
    }

    public static final QName IETF_NETCONF_MONITORING = QName.create(
            "urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring", "2010-10-04", "ietf-netconf-monitoring");
    public static URI NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    public static QName NETCONF_QNAME = QName.create(NETCONF_URI, null, "netconf");
    public static QName NETCONF_DATA_QNAME = QName.create(NETCONF_QNAME, "data");
    public static QName NETCONF_RPC_REPLY_QNAME = QName.create(NETCONF_QNAME, "rpc-reply");
    public static QName NETCONF_ERROR_OPTION_QNAME = QName.create(NETCONF_QNAME, "error-option");
    public static QName NETCONF_RUNNING_QNAME = QName.create(NETCONF_QNAME, "running");
    static List<Node<?>> RUNNING = Collections.<Node<?>> singletonList(new SimpleNodeTOImpl<>(NETCONF_RUNNING_QNAME,
            null, null));
    public static QName NETCONF_SOURCE_QNAME = QName.create(NETCONF_QNAME, "source");
    public static CompositeNode CONFIG_SOURCE_RUNNING = new CompositeNodeTOImpl(NETCONF_SOURCE_QNAME, null, RUNNING);
    public static QName NETCONF_CANDIDATE_QNAME = QName.create(NETCONF_QNAME, "candidate");
    public static QName NETCONF_TARGET_QNAME = QName.create(NETCONF_QNAME, "target");
    public static QName NETCONF_CONFIG_QNAME = QName.create(NETCONF_QNAME, "config");
    public static QName NETCONF_COMMIT_QNAME = QName.create(NETCONF_QNAME, "commit");
    public static QName NETCONF_OPERATION_QNAME = QName.create(NETCONF_QNAME, "operation");
    public static QName NETCONF_EDIT_CONFIG_QNAME = QName.create(NETCONF_QNAME, "edit-config");
    public static QName NETCONF_GET_CONFIG_QNAME = QName.create(NETCONF_QNAME, "get-config");
    public static QName NETCONF_TYPE_QNAME = QName.create(NETCONF_QNAME, "type");
    public static QName NETCONF_FILTER_QNAME = QName.create(NETCONF_QNAME, "filter");
    public static QName NETCONF_GET_QNAME = QName.create(NETCONF_QNAME, "get");
    public static QName NETCONF_RPC_QNAME = QName.create(NETCONF_QNAME, "rpc");
    public static URI NETCONF_ROLLBACK_ON_ERROR_URI = URI
            .create("urn:ietf:params:netconf:capability:rollback-on-error:1.0");
    public static String ROLLBACK_ON_ERROR_OPTION = "rollback-on-error";

    public static Node<?> toFilterStructure(final InstanceIdentifier identifier) {
        Node<?> previous = null;
        if (identifier.getPath().isEmpty()) {
            return null;
        }

        for (final org.opendaylight.yangtools.yang.data.api.InstanceIdentifier.PathArgument component : Lists
                .reverse(identifier.getPath())) {
            if (component instanceof InstanceIdentifier.NodeIdentifierWithPredicates) {
                previous = toNode((InstanceIdentifier.NodeIdentifierWithPredicates)component, previous);
            } else {
                previous = toNode(component, previous);
            }
        }
        return filter("subtree", previous);
    }

    static Node<?> toNode(final InstanceIdentifier.NodeIdentifierWithPredicates argument, final Node<?> node) {
        final List<Node<?>> list = new ArrayList<>();
        for (final Map.Entry<QName, Object> arg : argument.getKeyValues().entrySet()) {
            list.add(new SimpleNodeTOImpl(arg.getKey(), null, arg.getValue()));
        }
        if (node != null) {
            list.add(node);
        }
        return new CompositeNodeTOImpl(argument.getNodeType(), null, list);
    }

    public static void checkValidReply(final NetconfMessage input, final NetconfMessage output) {
        final String inputMsgId = input.getDocument().getDocumentElement().getAttribute("message-id");
        final String outputMsgId = output.getDocument().getDocumentElement().getAttribute("message-id");

        if(inputMsgId.equals(outputMsgId) == false) {
            final String requestXml = XmlUtil.toString(input.getDocument());
            final String responseXml = XmlUtil.toString(output.getDocument());
            throw new IllegalStateException(String.format("Rpc request and reply message IDs must be same. Request: %s, response: %s", requestXml, responseXml));
        }
    }

    public static void checkSuccessReply(final NetconfMessage output) throws NetconfDocumentedException {
        if(NetconfMessageUtil.isErrorMessage(output)) {
            throw new IllegalStateException(String.format("Response contains error: %s", XmlUtil.toString(output.getDocument())));
        }
    }

    public static CompositeNode flattenInput(final CompositeNode node) {
        final QName inputQName = QName.create(node.getNodeType(), "input");
        final CompositeNode input = node.getFirstCompositeByName(inputQName);
        if (input == null)
            return node;
        if (input instanceof CompositeNode) {

            final List<Node<?>> nodes = ImmutableList.<Node<?>> builder() //
                    .addAll(input.getValue()) //
                    .addAll(Collections2.filter(node.getValue(), new Predicate<Node<?>>() {
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

    static Node<?> toNode(final InstanceIdentifier.PathArgument argument, final Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(argument.getNodeType(), null, Collections.<Node<?>> singletonList(node));
        } else {
            return new SimpleNodeTOImpl<Void>(argument.getNodeType(), null, null);
        }
    }

    public static Element getDataSubtree(final Document doc) {
        return (Element) doc.getElementsByTagNameNS(NETCONF_URI.toString(), "data").item(0);
    }

    public static boolean isDataRetrievalOperation(final QName rpc) {
        return NETCONF_URI == rpc.getNamespace()
                && (rpc.getLocalName().equals(NETCONF_GET_CONFIG_QNAME.getLocalName()) || rpc.getLocalName().equals(
                        NETCONF_GET_QNAME.getLocalName()));
    }

    public static CompositeNodeTOImpl wrap(final QName name, final Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, Collections.<Node<?>> singletonList(node));
        } else {
            return new CompositeNodeTOImpl(name, null, Collections.<Node<?>> emptyList());
        }
    }

    public static CompositeNodeTOImpl wrap(final QName name, final Node<?> additional, final Node<?> node) {
        if (node != null) {
            return new CompositeNodeTOImpl(name, null, ImmutableList.of(additional, node));
        } else {
            return new CompositeNodeTOImpl(name, null, ImmutableList.<Node<?>> of(additional));
        }
    }

    static ImmutableCompositeNode filter(final String type, final Node<?> node) {
        final CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder(); //
        it.setQName(NETCONF_FILTER_QNAME);
        it.setAttribute(NETCONF_TYPE_QNAME, type);
        if (node != null) {
            return it.add(node).toInstance();
        } else {
            return it.toInstance();
        }
    }

}
