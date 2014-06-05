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
import java.util.Set;

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
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ConstraintDefinition;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.GroupingDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.api.TypeDefinition;
import org.opendaylight.yangtools.yang.model.api.UnknownSchemaNode;
import org.opendaylight.yangtools.yang.model.api.UsesNode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

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

    public static boolean isDataEditOperation(final QName rpc) {
        return NETCONF_URI.equals(rpc.getNamespace())
                && rpc.getLocalName().equals(NETCONF_EDIT_CONFIG_QNAME.getLocalName());
    }


    public static DataNodeContainer createSchemaForEdit(final SchemaContext schemaContext) {
        final QName config = QName.create(NETCONF_EDIT_CONFIG_QNAME, "config");
        final QName editConfig = QName.create(NETCONF_EDIT_CONFIG_QNAME, "edit-config");
        final NodeContainerProxy configProxy = new NodeContainerProxy(config, schemaContext.getChildNodes());
        final NodeContainerProxy editConfigProxy = new NodeContainerProxy(editConfig, Sets.<DataSchemaNode>newHashSet(configProxy));
        return new NodeContainerProxy(NETCONF_RPC_QNAME, Sets.<DataSchemaNode>newHashSet(editConfigProxy));
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

    private static class NodeContainerProxy implements ContainerSchemaNode {

        private final Map<QName, DataSchemaNode> childNodes;
        private final QName qName;

        public NodeContainerProxy(final QName qName, final Map<QName, DataSchemaNode> childNodes) {
            this.childNodes = childNodes;
            this.qName = qName;
        }

        public NodeContainerProxy(final QName qName, final Set<DataSchemaNode> childNodes) {
            this(qName, asMap(childNodes));
        }

        private static Map<QName, DataSchemaNode> asMap(final Set<DataSchemaNode> childNodes) {
            Map<QName, DataSchemaNode> mapped = Maps.newHashMap();
            for (DataSchemaNode childNode : childNodes) {
                mapped.put(childNode.getQName(), childNode);
            }
            return mapped;
        }

        @Override
        public Set<TypeDefinition<?>> getTypeDefinitions() {
            return Collections.emptySet();
        }

        @Override
        public Set<DataSchemaNode> getChildNodes() {
            return Sets.newHashSet(childNodes.values());
        }

        @Override
        public Set<GroupingDefinition> getGroupings() {
            return Collections.emptySet();
        }

        @Override
        public DataSchemaNode getDataChildByName(final QName qName) {
            return childNodes.get(qName);
        }

        @Override
        public DataSchemaNode getDataChildByName(final String s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<UsesNode> getUses() {
            return Collections.emptySet();
        }

        @Override
        public boolean isPresenceContainer() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<AugmentationSchema> getAvailableAugmentations() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAugmenting() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isAddedByUses() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isConfiguration() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConstraintDefinition getConstraints() {
            throw new UnsupportedOperationException();
        }

        @Override
        public QName getQName() {
            return qName;
        }

        @Override
        public SchemaPath getPath() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getDescription() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getReference() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Status getStatus() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UnknownSchemaNode> getUnknownSchemaNodes() {
            return Collections.emptyList();
        }
    }

}
