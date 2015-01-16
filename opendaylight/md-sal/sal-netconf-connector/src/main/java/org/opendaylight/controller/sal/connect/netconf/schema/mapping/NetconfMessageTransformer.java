/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema.mapping;

import com.google.common.base.Optional;
import java.util.List;
import java.util.Set;
import javax.activation.UnsupportedDataTypeException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.MessageCounter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.opendaylight.yangtools.yang.data.impl.CompositeNodeTOImpl;
import org.opendaylight.yangtools.yang.data.impl.ImmutableCompositeNode;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlCodecProvider;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlDocumentUtils;
import org.opendaylight.yangtools.yang.data.impl.util.CompositeNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfMessageTransformer implements MessageTransformer<NetconfMessage> {

    public static final String MESSAGE_ID_PREFIX = "m";

    private Optional<SchemaContext> schemaContext = Optional.absent();
    private final MessageCounter counter;

    public NetconfMessageTransformer() {
        this.counter = new MessageCounter();
    }

    @Override
    public synchronized CompositeNode toNotification(final NetconfMessage message) {
        if(schemaContext.isPresent()) {
            return toNotification(message, schemaContext.get());
        } else {
            return XmlDocumentUtils.notificationToDomNodes(message.getDocument(), Optional.<Set<NotificationDefinition>>absent());
        }
    }

    private static CompositeNode toNotification(final NetconfMessage message, final SchemaContext ctx) {
        final Set<NotificationDefinition> notifications = ctx.getNotifications();
        final Document document = message.getDocument();
        return XmlDocumentUtils.notificationToDomNodes(document, Optional.fromNullable(notifications), ctx);
    }

    @Override
    public NetconfMessage toRpcRequest(final QName rpc, final CompositeNode node) {
        final CompositeNodeTOImpl rpcPayload = NetconfMessageTransformUtil.wrap(
                NetconfMessageTransformUtil.NETCONF_RPC_QNAME, NetconfMessageTransformUtil.flattenInput(node));
        final Document w3cPayload;
        try {
            final XmlCodecProvider codecProvider = XmlDocumentUtils.defaultValueCodecProvider();
            if(schemaContext.isPresent()) {
                if (NetconfMessageTransformUtil.isDataEditOperation(rpc)) {
                    final DataNodeContainer schemaForEdit = NetconfMessageTransformUtil.createSchemaForEdit(schemaContext.get());
                    w3cPayload = XmlDocumentUtils.toDocument(rpcPayload, schemaContext.get(), schemaForEdit, codecProvider);
                } else if (NetconfMessageTransformUtil.isGetOperation(rpc)) {
                    final DataNodeContainer schemaForGet = NetconfMessageTransformUtil.createSchemaForGet(schemaContext.get());
                    w3cPayload = XmlDocumentUtils.toDocument(rpcPayload, schemaContext.get(), schemaForGet, codecProvider);
                } else if (NetconfMessageTransformUtil.isGetConfigOperation(rpc)) {
                    final DataNodeContainer schemaForGetConfig = NetconfMessageTransformUtil.createSchemaForGetConfig(schemaContext.get());
                    w3cPayload = XmlDocumentUtils.toDocument(rpcPayload, schemaContext.get(), schemaForGetConfig, codecProvider);
                } else {
                    final Optional<RpcDefinition> schemaForRpc = NetconfMessageTransformUtil.findSchemaForRpc(rpc, schemaContext.get());
                    if(schemaForRpc.isPresent()) {
                        final DataNodeContainer schemaForGetConfig = NetconfMessageTransformUtil.createSchemaForRpc(schemaForRpc.get());
                        w3cPayload = XmlDocumentUtils.toDocument(rpcPayload, schemaContext.get(), schemaForGetConfig, codecProvider);
                    } else {
                        w3cPayload = toRpcRequestWithoutSchema(rpcPayload, codecProvider);
                    }
                }
            } else {
                w3cPayload = toRpcRequestWithoutSchema(rpcPayload, codecProvider);
            }
        } catch (final UnsupportedDataTypeException e) {
            throw new IllegalArgumentException("Unable to create message", e);
        }
        w3cPayload.getDocumentElement().setAttribute("message-id", counter.getNewMessageId(MESSAGE_ID_PREFIX));
        return new NetconfMessage(w3cPayload);
    }

    private Document toRpcRequestWithoutSchema(final CompositeNodeTOImpl rpcPayload, final XmlCodecProvider codecProvider) {
        return XmlDocumentUtils.toDocument(rpcPayload, codecProvider);
    }

    @Override
    public synchronized RpcResult<CompositeNode> toRpcResult(final NetconfMessage message, final QName rpc) {
        if(schemaContext.isPresent()) {
            return toRpcResult(message, rpc, schemaContext.get());
        } else {
            final CompositeNode node = (CompositeNode) XmlDocumentUtils.toDomNode(message.getDocument());
            return RpcResultBuilder.success(node).build();
        }
    }

    private static RpcResult<CompositeNode> toRpcResult(final NetconfMessage message, final QName rpc, final SchemaContext context) {
        final CompositeNode compositeNode;
        if (NetconfMessageTransformUtil.isDataRetrievalOperation(rpc)) {
            final Element xmlData = NetconfMessageTransformUtil.getDataSubtree(message.getDocument());
            final List<org.opendaylight.yangtools.yang.data.api.Node<?>> dataNodes = XmlDocumentUtils.toDomNodes(xmlData,
                    Optional.of(context.getDataDefinitions()), context);

            final CompositeNodeBuilder<ImmutableCompositeNode> it = ImmutableCompositeNode.builder();
            it.setQName(NetconfMessageTransformUtil.NETCONF_RPC_REPLY_QNAME);
            it.add(ImmutableCompositeNode.create(NetconfMessageTransformUtil.NETCONF_DATA_QNAME, dataNodes));
            compositeNode = it.build();
        } else {
            final CompositeNode rpcReply = XmlDocumentUtils.rpcReplyToDomNodes(message.getDocument(), rpc, context);
            if (rpcReply != null) {
                compositeNode = rpcReply;
            } else {
                compositeNode = (CompositeNode) XmlDocumentUtils.toDomNode(message.getDocument());
            }
        }
        return RpcResultBuilder.success( compositeNode ).build();
    }

    @Override
    public synchronized void onGlobalContextUpdated(final SchemaContext schemaContext) {
        this.schemaContext = Optional.fromNullable(schemaContext);
    }
}
