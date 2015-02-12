/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.Node;
import org.opendaylight.yangtools.yang.data.api.SimpleNode;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.NodeFactory;
import org.opendaylight.yangtools.yang.data.impl.SimpleNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfMessageTransformUtil {

    public static final String MESSAGE_ID_ATTR = "message-id";

    private NetconfMessageTransformUtil() {}

    public static final QName IETF_NETCONF_MONITORING = QName.create(NetconfState.QNAME, "ietf-netconf-monitoring");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_FORMAT = QName.create(IETF_NETCONF_MONITORING, "format");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_LOCATION = QName.create(IETF_NETCONF_MONITORING, "location");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_IDENTIFIER = QName.create(IETF_NETCONF_MONITORING, "identifier");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_VERSION = QName.create(IETF_NETCONF_MONITORING, "version");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_NAMESPACE = QName.create(IETF_NETCONF_MONITORING, "namespace");

    public static URI NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    public static QName NETCONF_QNAME = QName.create(NETCONF_URI, null, "netconf");
    public static QName NETCONF_DATA_QNAME = QName.create(NETCONF_QNAME, "data");
    public static QName NETCONF_RPC_REPLY_QNAME = QName.create(NETCONF_QNAME, "rpc-reply");
    public static QName NETCONF_ERROR_OPTION_QNAME = QName.create(NETCONF_QNAME, "error-option");
    public static QName NETCONF_RUNNING_QNAME = QName.create(NETCONF_QNAME, "running");
    public static QName NETCONF_SOURCE_QNAME = QName.create(NETCONF_QNAME, "source");
    public static QName NETCONF_CANDIDATE_QNAME = QName.create(NETCONF_QNAME, "candidate");
    public static QName NETCONF_TARGET_QNAME = QName.create(NETCONF_QNAME, "target");
    public static QName NETCONF_CONFIG_QNAME = QName.create(NETCONF_QNAME, "config");
    public static QName NETCONF_COMMIT_QNAME = QName.create(NETCONF_QNAME, "commit");
    public static QName NETCONF_VALIDATE_QNAME = QName.create(NETCONF_QNAME, "validate");
    public static QName NETCONF_COPY_CONFIG_QNAME = QName.create(NETCONF_QNAME, "copy-config");
    public static QName NETCONF_OPERATION_QNAME = QName.create(NETCONF_QNAME, "operation");
    public static QName NETCONF_DEFAULT_OPERATION_QNAME = QName.create(NETCONF_OPERATION_QNAME, "default-operation");
    public static QName NETCONF_EDIT_CONFIG_QNAME = QName.create(NETCONF_QNAME, "edit-config");
    public static QName NETCONF_GET_CONFIG_QNAME = QName.create(NETCONF_QNAME, "get-config");
    public static QName NETCONF_DISCARD_CHANGES_QNAME = QName.create(NETCONF_QNAME, "discard-changes");
    public static QName NETCONF_TYPE_QNAME = QName.create(NETCONF_QNAME, "type");
    public static QName NETCONF_FILTER_QNAME = QName.create(NETCONF_QNAME, "filter");
    public static QName NETCONF_GET_QNAME = QName.create(NETCONF_QNAME, "get");
    public static QName NETCONF_RPC_QNAME = QName.create(NETCONF_QNAME, "rpc");

    public static URI NETCONF_ROLLBACK_ON_ERROR_URI = URI
            .create("urn:ietf:params:netconf:capability:rollback-on-error:1.0");
    public static String ROLLBACK_ON_ERROR_OPTION = "rollback-on-error";

    public static URI NETCONF_CANDIDATE_URI = URI
            .create("urn:ietf:params:netconf:capability:candidate:1.0");

    public static URI NETCONF_RUNNING_WRITABLE_URI = URI
            .create("urn:ietf:params:netconf:capability:writable-running:1.0");

    public static QName NETCONF_LOCK_QNAME = QName.create(NETCONF_QNAME, "lock");
    public static QName NETCONF_UNLOCK_QNAME = QName.create(NETCONF_QNAME, "unlock");

    // Discard changes message
    public static final CompositeNode DISCARD_CHANGES_RPC_CONTENT =
            NodeFactory.createImmutableCompositeNode(NETCONF_DISCARD_CHANGES_QNAME, null, Collections.<Node<?>>emptyList());

    // Commit changes message
    public static final CompositeNode COMMIT_RPC_CONTENT =
            NodeFactory.createImmutableCompositeNode(NETCONF_COMMIT_QNAME, null, Collections.<Node<?>>emptyList());

    public static Node<?> toFilterStructure(final YangInstanceIdentifier identifier) {
        Node<?> previous = null;
        if (Iterables.isEmpty(identifier.getPathArguments())) {
            return null;
        }

        for (final org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument component : identifier.getReversePathArguments()) {
            if (component instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
                previous = toNode((YangInstanceIdentifier.NodeIdentifierWithPredicates)component, previous);
            } else {
                previous = toNode(component, previous);
            }
        }
        return filter("subtree", previous);
    }

    static Node<?> toNode(final YangInstanceIdentifier.NodeIdentifierWithPredicates argument, final Node<?> node) {
        final List<Node<?>> list = new ArrayList<>();
        for (final Map.Entry<QName, Object> arg : argument.getKeyValues().entrySet()) {
            list.add(new SimpleNodeTOImpl(arg.getKey(), null, arg.getValue()));
        }
        if (node != null) {
            list.add(node);
        }
        return new CompositeNodeTOImpl(argument.getNodeType(), null, list);
    }

    public static boolean checkValidReplyBoolean(final NetconfMessage input, final NetconfMessage output ) {

        final String inputMsgId = input.getDocument().getDocumentElement().getAttribute(MESSAGE_ID_ATTR);
        final String outputMsgId = output.getDocument().getDocumentElement().getAttribute(MESSAGE_ID_ATTR);

        return inputMsgId.equals(outputMsgId);
    }

    public static void checkValidReply(final NetconfMessage input, final NetconfMessage output)
            throws NetconfDocumentedException {
        final String inputMsgId = input.getDocument().getDocumentElement().getAttribute(MESSAGE_ID_ATTR);
        final String outputMsgId = output.getDocument().getDocumentElement().getAttribute(MESSAGE_ID_ATTR);

        if(inputMsgId.equals(outputMsgId) == false) {
            Map<String,String> errorInfo = ImmutableMap.<String,String>builder()
                    .put( "actual-message-id", outputMsgId )
                    .put( "expected-message-id", inputMsgId )
                    .build();

            throw new NetconfDocumentedException( "Response message contained unknown \"message-id\"",
                    null, NetconfDocumentedException.ErrorType.protocol,
                    NetconfDocumentedException.ErrorTag.bad_attribute,
                    NetconfDocumentedException.ErrorSeverity.error, errorInfo );
        }
    }

    public static void checkSuccessReply(final NetconfMessage output) throws NetconfDocumentedException {
        if(NetconfMessageUtil.isErrorMessage(output)) {
            throw NetconfDocumentedException.fromXMLDocument( output.getDocument() );
        }
    }

    public static RpcError toRpcError( final NetconfDocumentedException ex )
    {
        StringBuilder infoBuilder = new StringBuilder();
        Map<String, String> errorInfo = ex.getErrorInfo();
        if( errorInfo != null )
        {
            for( Entry<String,String> e: errorInfo.entrySet() ) {
                infoBuilder.append( '<' ).append( e.getKey() ).append( '>' ).append( e.getValue() )
                .append( "</" ).append( e.getKey() ).append( '>' );

            }
        }

        ErrorSeverity severity = toRpcErrorSeverity( ex.getErrorSeverity() );
        return severity == ErrorSeverity.ERROR ?
                RpcResultBuilder.newError(
                        toRpcErrorType( ex.getErrorType() ), ex.getErrorTag().getTagValue(),
                        ex.getLocalizedMessage(), null, infoBuilder.toString(), ex.getCause() ) :
                            RpcResultBuilder.newWarning(
                                    toRpcErrorType( ex.getErrorType() ), ex.getErrorTag().getTagValue(),
                                    ex.getLocalizedMessage(), null, infoBuilder.toString(), ex.getCause() );
    }

    private static ErrorSeverity toRpcErrorSeverity( final NetconfDocumentedException.ErrorSeverity severity ) {
        switch( severity ) {
        case warning:
            return RpcError.ErrorSeverity.WARNING;
        default:
            return RpcError.ErrorSeverity.ERROR;
        }
    }

    private static RpcError.ErrorType toRpcErrorType( final NetconfDocumentedException.ErrorType type )
    {
        switch( type ) {
        case protocol:
            return RpcError.ErrorType.PROTOCOL;
        case rpc:
            return RpcError.ErrorType.RPC;
        case transport:
            return RpcError.ErrorType.TRANSPORT;
        default:
            return RpcError.ErrorType.APPLICATION;
        }
    }

    public static CompositeNode flattenInput(final CompositeNode node) {
        final QName inputQName = QName.create(node.getNodeType(), "input");
        final CompositeNode input = node.getFirstCompositeByName(inputQName);
        if (input == null) {
            return node;
        }
        if (input instanceof CompositeNode) {

            final List<Node<?>> nodes = ImmutableList.<Node<?>> builder() //
                    .addAll(input.getValue()) //
                    .addAll(Collections2.filter(node.getValue(), new Predicate<Node<?>>() {
                        @Override
                        public boolean apply(@Nullable final Node<?> input) {
                            return !inputQName.equals(input.getNodeType());
                        }
                    })) //
                    .build();

            return ImmutableCompositeNode.create(node.getNodeType(), nodes);
        }

        return input;
    }

    static Node<?> toNode(final YangInstanceIdentifier.PathArgument argument, final Node<?> node) {
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
        return NETCONF_URI.equals(rpc.getNamespace())
                && (rpc.getLocalName().equals(NETCONF_GET_CONFIG_QNAME.getLocalName()) || rpc.getLocalName().equals(
                        NETCONF_GET_QNAME.getLocalName()));
    }

    public static boolean isGetOperation(final QName rpc) {
        return NETCONF_URI.equals(rpc.getNamespace()) && rpc.getLocalName().equals(NETCONF_GET_QNAME.getLocalName());
    }

    public static boolean isGetConfigOperation(final QName rpc) {
        return NETCONF_URI.equals(rpc.getNamespace()) && rpc.getLocalName().equals(NETCONF_GET_CONFIG_QNAME.getLocalName());
    }

    public static boolean isDataEditOperation(final QName rpc) {
        return NETCONF_URI.equals(rpc.getNamespace())
                && rpc.getLocalName().equals(NETCONF_EDIT_CONFIG_QNAME.getLocalName());
    }

    /**
     * Creates artificial schema node for edit-config rpc. This artificial schema looks like:
     * <pre>
     * {@code
     * rpc
     *   edit-config
     *     config
     *         // All schema nodes from remote schema
     *     config
     *   edit-config
     * rpc
     * }
     * </pre>
     *
     * This makes the translation of rpc edit-config request(especially the config node)
     * to xml use schema which is crucial for some types of nodes e.g. identity-ref.
     */
    public static DataNodeContainer createSchemaForEdit(final SchemaContext schemaContext) {
        final QName config = QName.create(NETCONF_EDIT_CONFIG_QNAME, "config");
        final QName editConfig = QName.create(NETCONF_EDIT_CONFIG_QNAME, "edit-config");
        final NodeContainerProxy configProxy = new NodeContainerProxy(config, schemaContext.getChildNodes());
        final NodeContainerProxy editConfigProxy = new NodeContainerProxy(editConfig, Sets.<DataSchemaNode>newHashSet(configProxy));
        return new NodeContainerProxy(NETCONF_RPC_QNAME, Sets.<DataSchemaNode>newHashSet(editConfigProxy));
    }

    /**
     * Creates artificial schema node for edit-config rpc. This artificial schema looks like:
     * <pre>
     * {@code
     * rpc
     *   get
     *     filter
     *         // All schema nodes from remote schema
     *     filter
     *   get
     * rpc
     * }
     * </pre>
     *
     * This makes the translation of rpc get request(especially the config node)
     * to xml use schema which is crucial for some types of nodes e.g. identity-ref.
     */
    public static DataNodeContainer createSchemaForGet(final SchemaContext schemaContext) {
        final QName filter = QName.create(NETCONF_GET_QNAME, "filter");
        final QName get = QName.create(NETCONF_GET_QNAME, "get");
        final NodeContainerProxy configProxy = new NodeContainerProxy(filter, schemaContext.getChildNodes());
        final NodeContainerProxy editConfigProxy = new NodeContainerProxy(get, Sets.<DataSchemaNode>newHashSet(configProxy));
        return new NodeContainerProxy(NETCONF_RPC_QNAME, Sets.<DataSchemaNode>newHashSet(editConfigProxy));
    }

    /**
     * Creates artificial schema node for get rpc. This artificial schema looks like:
     * <pre>
     * {@code
     * rpc
     *   get-config
     *     filter
     *         // All schema nodes from remote schema
     *     filter
     *   get-config
     * rpc
     * }
     * </pre>
     *
     * This makes the translation of rpc get-config request(especially the config node)
     * to xml use schema which is crucial for some types of nodes e.g. identity-ref.
     */
    public static DataNodeContainer createSchemaForGetConfig(final SchemaContext schemaContext) {
        final QName filter = QName.create(NETCONF_GET_CONFIG_QNAME, "filter");
        final QName getConfig = QName.create(NETCONF_GET_CONFIG_QNAME, "get-config");
        final NodeContainerProxy configProxy = new NodeContainerProxy(filter, schemaContext.getChildNodes());
        final NodeContainerProxy editConfigProxy = new NodeContainerProxy(getConfig, Sets.<DataSchemaNode>newHashSet(configProxy));
        return new NodeContainerProxy(NETCONF_RPC_QNAME, Sets.<DataSchemaNode>newHashSet(editConfigProxy));
    }


    public static Optional<RpcDefinition> findSchemaForRpc(final QName rpcName, final SchemaContext schemaContext) {
        Preconditions.checkNotNull(rpcName);
        Preconditions.checkNotNull(schemaContext);

        for (final RpcDefinition rpcDefinition : schemaContext.getOperations()) {
            if(rpcDefinition.getQName().equals(rpcName)) {
                return Optional.of(rpcDefinition);
            }
        }

        return Optional.absent();
    }

    /**
     * Creates artificial schema node for schema defined rpc. This artificial schema looks like:
     * <pre>
     * {@code
     * rpc
     *   rpc-name
     *      // All schema nodes from remote schema
     *   rpc-name
     * rpc
     * }
     * </pre>
     *
     * This makes the translation of schema defined rpc request
     * to xml use schema which is crucial for some types of nodes e.g. identity-ref.
     */
    public static DataNodeContainer createSchemaForRpc(final RpcDefinition rpcDefinition) {
        final NodeContainerProxy rpcBodyProxy = new NodeContainerProxy(rpcDefinition.getQName(), rpcDefinition.getInput().getChildNodes());
        return new NodeContainerProxy(NETCONF_RPC_QNAME, Sets.<DataSchemaNode>newHashSet(rpcBodyProxy));
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

    public static Node<?> findNode(final CompositeNode node, final YangInstanceIdentifier identifier) {

        Node<?> current = node;
        for (final YangInstanceIdentifier.PathArgument arg : identifier.getPathArguments()) {
            if (current instanceof SimpleNode<?>) {
                return null;
            } else if (current instanceof CompositeNode) {
                final CompositeNode currentComposite = (CompositeNode) current;

                current = currentComposite.getFirstCompositeByName(arg.getNodeType());
                if (current == null) {
                    current = currentComposite.getFirstCompositeByName(arg.getNodeType().withoutRevision());
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.getNodeType());
                }
                if (current == null) {
                    current = currentComposite.getFirstSimpleByName(arg.getNodeType().withoutRevision());
                }
                if (current == null) {
                    return null;
                }
            }
        }
        return current;
    }

    public static String modifyOperationToXmlString(final ModifyAction operation) {
        return operation.name().toLowerCase();
    }


    public static CompositeNode createEditConfigStructure(final YangInstanceIdentifier dataPath, final Optional<ModifyAction> operation,
                                                    final Optional<CompositeNode> lastChildOverride) {
        Preconditions.checkArgument(Iterables.isEmpty(dataPath.getPathArguments()) == false, "Instance identifier with empty path %s", dataPath);

        List<YangInstanceIdentifier.PathArgument> reversedPath = Lists.reverse(dataPath.getPath());

        // Create deepest edit element with expected edit operation
        CompositeNode previous = getDeepestEditElement(reversedPath.get(0), operation, lastChildOverride);

        // Remove already processed deepest child
        reversedPath = Lists.newArrayList(reversedPath);
        reversedPath.remove(0);

        // Create edit structure in reversed order
        for (final YangInstanceIdentifier.PathArgument arg : reversedPath) {
            final CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
            builder.setQName(arg.getNodeType());

            addPredicatesToCompositeNodeBuilder(getPredicates(arg), builder);

            builder.add(previous);
            previous = builder.toInstance();
        }
        return ImmutableCompositeNode.create(NETCONF_CONFIG_QNAME, ImmutableList.<Node<?>>of(previous));
    }

    public static void addPredicatesToCompositeNodeBuilder(final Map<QName, Object> predicates, final CompositeNodeBuilder<ImmutableCompositeNode> builder) {
        for (final Map.Entry<QName, Object> entry : predicates.entrySet()) {
            builder.addLeaf(entry.getKey(), entry.getValue());
        }
    }

    public static Map<QName, Object> getPredicates(final YangInstanceIdentifier.PathArgument arg) {
        Map<QName, Object> predicates = Collections.emptyMap();
        if (arg instanceof YangInstanceIdentifier.NodeIdentifierWithPredicates) {
            predicates = ((YangInstanceIdentifier.NodeIdentifierWithPredicates) arg).getKeyValues();
        }
        return predicates;
    }

    public static CompositeNode getDeepestEditElement(final YangInstanceIdentifier.PathArgument arg, final Optional<ModifyAction> operation, final Optional<CompositeNode> lastChildOverride) {
        final CompositeNodeBuilder<ImmutableCompositeNode> builder = ImmutableCompositeNode.builder();
        builder.setQName(arg.getNodeType());

        final Map<QName, Object> predicates = getPredicates(arg);
        addPredicatesToCompositeNodeBuilder(predicates, builder);

        if (operation.isPresent()) {
            builder.setAttribute(NETCONF_OPERATION_QNAME, modifyOperationToXmlString(operation.get()));
        }
        if (lastChildOverride.isPresent()) {
            final List<Node<?>> children = lastChildOverride.get().getValue();
            for(final Node<?> child : children) {
                if(!predicates.containsKey(child.getKey())) {
                    builder.add(child);
                }
            }
        }

        return builder.toInstance();
    }
}
