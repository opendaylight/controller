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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.edit.config.input.EditContent;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004.NetconfState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorSeverity;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.ModifyAction;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.api.NormalizedNodeAttrBuilder;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfMessageTransformUtil {

    private static final Logger LOG= LoggerFactory.getLogger(NetconfMessageTransformUtil.class);

    public static final String MESSAGE_ID_ATTR = "message-id";
    public static final XMLOutputFactory XML_FACTORY;

    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, false);
    }

    public static final QName CREATE_SUBSCRIPTION_RPC_QNAME = QName.cachedReference(QName.create(CreateSubscriptionInput.QNAME, "create-subscription"));
    private static final String SUBTREE = "subtree";

    // Blank document used for creation of new DOM nodes
    private static final Document BLANK_DOCUMENT = XmlUtil.newDocument();

    private NetconfMessageTransformUtil() {}

    public static final QName IETF_NETCONF_MONITORING = QName.create(NetconfState.QNAME, "ietf-netconf-monitoring");
    public static final QName GET_DATA_QNAME = QName.create(IETF_NETCONF_MONITORING, "data");
    public static final QName GET_SCHEMA_QNAME = QName.create(IETF_NETCONF_MONITORING, "get-schema");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_FORMAT = QName.create(IETF_NETCONF_MONITORING, "format");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_LOCATION = QName.create(IETF_NETCONF_MONITORING, "location");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_IDENTIFIER = QName.create(IETF_NETCONF_MONITORING, "identifier");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_VERSION = QName.create(IETF_NETCONF_MONITORING, "version");
    public static final QName IETF_NETCONF_MONITORING_SCHEMA_NAMESPACE = QName.create(IETF_NETCONF_MONITORING, "namespace");

    public static final QName IETF_NETCONF_NOTIFICATIONS = QName.create(NetconfCapabilityChange.QNAME, "ietf-netconf-notifications");

    public static URI NETCONF_URI = URI.create("urn:ietf:params:xml:ns:netconf:base:1.0");
    public static QName NETCONF_QNAME = QName.create(NETCONF_URI.toString(), "2011-06-01", "netconf");
    public static QName NETCONF_DATA_QNAME = QName.create(NETCONF_QNAME, "data");
    public static QName NETCONF_RPC_REPLY_QNAME = QName.create(NETCONF_QNAME, "rpc-reply");
    public static QName NETCONF_OK_QNAME = QName.create(NETCONF_QNAME, "ok");
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

    public static URI NETCONF_NOTIFICATONS_URI = URI
            .create("urn:ietf:params:netconf:capability:notification:1.0");

    public static URI NETCONF_RUNNING_WRITABLE_URI = URI
            .create("urn:ietf:params:netconf:capability:writable-running:1.0");

    public static QName NETCONF_LOCK_QNAME = QName.create(NETCONF_QNAME, "lock");
    public static QName NETCONF_UNLOCK_QNAME = QName.create(NETCONF_QNAME, "unlock");

    // Discard changes message
    public static final ContainerNode DISCARD_CHANGES_RPC_CONTENT =
            Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NETCONF_DISCARD_CHANGES_QNAME)).build();

    // Commit changes message
    public static final ContainerNode COMMIT_RPC_CONTENT =
            Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NETCONF_COMMIT_QNAME)).build();

    // Get message
    public static final ContainerNode GET_RPC_CONTENT =
            Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NETCONF_GET_QNAME)).build();

    // Create-subscription changes message
    public static final ContainerNode CREATE_SUBSCRIPTION_RPC_CONTENT =
            Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(CREATE_SUBSCRIPTION_RPC_QNAME)).build();

    public static DataContainerChild<?, ?> toFilterStructure(final YangInstanceIdentifier identifier, final SchemaContext ctx) {
        final NormalizedNodeAttrBuilder<YangInstanceIdentifier.NodeIdentifier, DOMSource, AnyXmlNode> anyXmlBuilder = Builders.anyXmlBuilder().withNodeIdentifier(toId(NETCONF_FILTER_QNAME));
        anyXmlBuilder.withAttributes(Collections.singletonMap(NETCONF_TYPE_QNAME, SUBTREE));

        final NormalizedNode<?, ?> filterContent = InstanceIdToNodes.serialize(ctx, identifier);

        final Element element = XmlUtil.createElement(BLANK_DOCUMENT, NETCONF_FILTER_QNAME.getLocalName(), Optional.of(NETCONF_FILTER_QNAME.getNamespace().toString()));
        element.setAttributeNS(NETCONF_FILTER_QNAME.getNamespace().toString(), NETCONF_TYPE_QNAME.getLocalName(), "subtree");

        try {
            writeNormalizedNode(filterContent, new DOMResult(element), SchemaPath.ROOT, ctx);
        } catch (IOException | XMLStreamException e) {
            throw new IllegalStateException("Unable to serialize filter element for path " + identifier, e);
        }
        anyXmlBuilder.withValue(new DOMSource(element));

        return anyXmlBuilder.build();
    }

    public static void checkValidReply(final NetconfMessage input, final NetconfMessage output)
            throws NetconfDocumentedException {
        final String inputMsgId = input.getDocument().getDocumentElement().getAttribute(MESSAGE_ID_ATTR);
        final String outputMsgId = output.getDocument().getDocumentElement().getAttribute(MESSAGE_ID_ATTR);

        if(inputMsgId.equals(outputMsgId) == false) {
            final Map<String,String> errorInfo = ImmutableMap.<String,String>builder()
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
            throw NetconfDocumentedException.fromXMLDocument(output.getDocument());
        }
    }

    public static RpcError toRpcError( final NetconfDocumentedException ex ) {
        final StringBuilder infoBuilder = new StringBuilder();
        final Map<String, String> errorInfo = ex.getErrorInfo();
        if(errorInfo != null) {
            for( final Entry<String,String> e: errorInfo.entrySet() ) {
                infoBuilder.append( '<' ).append( e.getKey() ).append( '>' ).append( e.getValue() )
                .append( "</" ).append( e.getKey() ).append( '>' );

            }
        }

        final ErrorSeverity severity = toRpcErrorSeverity( ex.getErrorSeverity() );
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

    private static RpcError.ErrorType toRpcErrorType(final NetconfDocumentedException.ErrorType type) {
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

    public static YangInstanceIdentifier.NodeIdentifier toId(final YangInstanceIdentifier.PathArgument qname) {
        return toId(qname.getNodeType());
    }

    public static YangInstanceIdentifier.NodeIdentifier toId(final QName nodeType) {
        return new YangInstanceIdentifier.NodeIdentifier(nodeType);
    }

    public static Element getDataSubtree(final Document doc) {
        return (Element) doc.getElementsByTagNameNS(NETCONF_URI.toString(), "data").item(0);
    }

    public static boolean isDataRetrievalOperation(final QName rpc) {
        return NETCONF_URI.equals(rpc.getNamespace())
                && (rpc.getLocalName().equals(NETCONF_GET_CONFIG_QNAME.getLocalName()) || rpc.getLocalName().equals(
                        NETCONF_GET_QNAME.getLocalName()));
    }

    public static ContainerSchemaNode createSchemaForDataRead(final SchemaContext schemaContext) {
        final QName config = QName.create(NETCONF_EDIT_CONFIG_QNAME, "data");
        return new NodeContainerProxy(config, schemaContext.getChildNodes());
    }

    public static ContainerSchemaNode createSchemaForNotification(final NotificationDefinition next) {
        return new NodeContainerProxy(next.getQName(), next.getChildNodes(), next.getAvailableAugmentations());
    }

    public static ContainerNode wrap(final QName name, final DataContainerChild<?, ?>... node) {
        return Builders.containerBuilder().withNodeIdentifier(toId(name)).withValue(Lists.newArrayList(node)).build();
    }

    public static DataContainerChild<?, ?> createEditConfigStructure(final SchemaContext ctx, final YangInstanceIdentifier dataPath,
                                                                     final Optional<ModifyAction> operation, final Optional<NormalizedNode<?, ?>> lastChildOverride) {
        final NormalizedNode<?, ?> configContent;

        if(Iterables.isEmpty(dataPath.getPathArguments())) {
            Preconditions.checkArgument(lastChildOverride.isPresent(), "Data has to be present when creating structure for top level element");
            Preconditions.checkArgument(lastChildOverride.get() instanceof DataContainerChild<?, ?>,
                    "Data has to be either container or a list node when creating structure for top level element, but was: %s", lastChildOverride.get());
            configContent = lastChildOverride.get();
        } else {
            configContent = InstanceIdToNodes.serialize(ctx, dataPath, lastChildOverride, operation);
        }

        final Element element = XmlUtil.createElement(BLANK_DOCUMENT, NETCONF_CONFIG_QNAME.getLocalName(), Optional.of(NETCONF_CONFIG_QNAME.getNamespace().toString()));
        try {
            writeNormalizedNode(configContent, new DOMResult(element), SchemaPath.ROOT, ctx);
        } catch (IOException | XMLStreamException e) {
            throw new IllegalStateException("Unable to serialize edit config content element for path " + dataPath, e);
        }
        final DOMSource value = new DOMSource(element);

        return Builders.choiceBuilder().withNodeIdentifier(toId(EditContent.QNAME)).withChild(
                Builders.anyXmlBuilder().withNodeIdentifier(toId(NETCONF_CONFIG_QNAME)).withValue(value).build()).build();
    }

    public static SchemaPath toPath(final QName rpc) {
        return SchemaPath.create(true, rpc);
    }

    public static String modifyOperationToXmlString(final ModifyAction operation) {
        return operation.name().toLowerCase();
    }

    // FIXME similar code is in netconf-notifications-impl , DRY
    public static void writeNormalizedNode(final NormalizedNode<?, ?> normalized, final DOMResult result, final SchemaPath schemaPath, final SchemaContext context)
            throws IOException, XMLStreamException {
        NormalizedNodeWriter normalizedNodeWriter = null;
        NormalizedNodeStreamWriter normalizedNodeStreamWriter = null;
        XMLStreamWriter writer = null;
        try {
            writer = XML_FACTORY.createXMLStreamWriter(result);
            normalizedNodeStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(writer, context, schemaPath);
            normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(normalizedNodeStreamWriter);

            normalizedNodeWriter.write(normalized);

            normalizedNodeWriter.flush();
        } finally {
            try {
                if(normalizedNodeWriter != null) {
                    normalizedNodeWriter.close();
                }
                if(normalizedNodeStreamWriter != null) {
                    normalizedNodeStreamWriter.close();
                }
                if(writer != null) {
                    writer.close();
                }
            } catch (final Exception e) {
                LOG.warn("Unable to close resource properly", e);
            }
        }
    }
}
