/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema.mapping;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RPC_QNAME;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.exception.MissingNameSpaceException;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.MessageTransformer;
import org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil;
import org.opendaylight.controller.sal.connect.util.MessageCounter;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XmlUtils;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.transform.dom.parser.DomToNormalizedNodeParserFactory;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NetconfMessageTransformer implements MessageTransformer<NetconfMessage> {

    public static final String MESSAGE_ID_PREFIX = "m";

    private static final Logger LOG= LoggerFactory.getLogger(NetconfMessageTransformer.class);

    private static final DomToNormalizedNodeParserFactory NORMALIZED_NODE_PARSER_FACTORY = DomToNormalizedNodeParserFactory.getInstance(XmlUtils.DEFAULT_XML_CODEC_PROVIDER);

    private static final Function<SchemaNode, QName> QNAME_FUNCTION = new Function<SchemaNode, QName>() {
        @Override
        public QName apply(final SchemaNode rpcDefinition) {
            return rpcDefinition.getQName();
        }
    };

    private static final Function<SchemaNode, QName> QNAME_NOREV_FUNCTION = new Function<SchemaNode, QName>() {
        @Override
        public QName apply(final SchemaNode notification) {
            return QNAME_FUNCTION.apply(notification).withoutRevision();
        }
    };
    private static final SchemaContext BASE_NETCONF_CTX;

    static {
        try {
            final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
            // TODO this should be used only if the base is not present
            moduleInfoBackedContext.addModuleInfos(
                    Lists.newArrayList(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl.getInstance()));
            BASE_NETCONF_CTX = moduleInfoBackedContext.tryToCreateSchemaContext().get();
        } catch (final RuntimeException e) {
            LOG.error("Unable to prepare schema context for base netconf ops", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    private final SchemaContext schemaContext;
    private final MessageCounter counter;
    private final Map<QName, RpcDefinition> mappedRpcs;
    private final Multimap<QName, NotificationDefinition> mappedNotifications;

    public NetconfMessageTransformer(final SchemaContext schemaContext) {
        this.counter = new MessageCounter();
        this.schemaContext = schemaContext;

        mappedRpcs = Maps.uniqueIndex(schemaContext.getOperations(), QNAME_FUNCTION);
        mappedNotifications = Multimaps.index(schemaContext.getNotifications(), QNAME_NOREV_FUNCTION);
    }

    @Override
    public synchronized ContainerNode toNotification(final NetconfMessage message) {
        final XmlElement stripped = stripNotification(message);
        final QName notificationNoRev;
        try {
            // How to construct QName with no revision ?
            notificationNoRev = QName.cachedReference(QName.create(stripped.getNamespace(), "0000-00-00", stripped.getName()).withoutRevision());
        } catch (final MissingNameSpaceException e) {
            throw new IllegalArgumentException("Unable to parse notification " + message + ", cannot find namespace", e);
        }

        final Collection<NotificationDefinition> notificationDefinitions = mappedNotifications.get(notificationNoRev);
        Preconditions.checkArgument(notificationDefinitions.size() > 0,
                "Unable to parse notification %s, unknown notification. Available notifications: %s", notificationDefinitions, mappedNotifications.keySet());

        // FIXME if multiple revisions for same notifications are present, we should pick the most recent. Or ?
        // We should probably just put the most recent notification versions into our map. We can expect that the device sends the data according to the latest available revision of a model.
        final NotificationDefinition next = notificationDefinitions.iterator().next();

        // We wrap the notification as a container node in order to reuse the parsers and builders for container node
        final ContainerSchemaNode notificationAsContainerSchemaNode = NetconfMessageTransformUtil.createSchemaForNotification(next);
        return NORMALIZED_NODE_PARSER_FACTORY.getContainerNodeParser().parse(Collections.singleton(stripped.getDomElement()), notificationAsContainerSchemaNode);
    }

    // FIXME move somewhere to util
    private static XmlElement stripNotification(final NetconfMessage message) {
        final XmlElement xmlElement = XmlElement.fromDomDocument(message.getDocument());
        final List<XmlElement> childElements = xmlElement.getChildElements();
        Preconditions.checkArgument(childElements.size() == 2, "Unable to parse notification %s, unexpected format", message);
        try {
            return Iterables.find(childElements, new Predicate<XmlElement>() {
                @Override
                public boolean apply(final XmlElement xmlElement) {
                    return !xmlElement.getName().equals("eventTime");
                }
            });
        } catch (final NoSuchElementException e) {
            throw new IllegalArgumentException("Unable to parse notification " + message + ", cannot strip notification metadata", e);
        }
    }

    @Override
    public NetconfMessage toRpcRequest(SchemaPath rpc, final ContainerNode payload) {
        // In case no input for rpc is defined, we can simply construct the payload here
        final QName rpcQName = rpc.getLastComponent();
        Preconditions.checkNotNull(mappedRpcs.get(rpcQName), "Unknown rpc %s, available rpcs: %s", rpcQName, mappedRpcs.keySet());
        if(mappedRpcs.get(rpcQName).getInput() == null) {
            final Document document = XmlUtil.newDocument();
            final Element elementNS = document.createElementNS(rpcQName.getNamespace().toString(), rpcQName.getLocalName());
            document.appendChild(elementNS);
            return new NetconfMessage(document);
        }

        // Set the path to the input of rpc for the node stream writer
        rpc = rpc.createChild(QName.cachedReference(QName.create(rpcQName, "input")));
        final DOMResult result = prepareDomResultForRpcRequest(rpcQName);

        try {
            writeNormalizedRpc(payload, result, rpc, schemaContext);
        } catch (final XMLStreamException | IOException | IllegalStateException e) {
            throw new IllegalStateException("Unable to serialize " + rpc, e);
        }

        final Document node = result.getNode().getOwnerDocument();

        node.getDocumentElement().setAttribute(NetconfMessageTransformUtil.MESSAGE_ID_ATTR, counter.getNewMessageId(MESSAGE_ID_PREFIX));
        return new NetconfMessage(node);
    }

    private DOMResult prepareDomResultForRpcRequest(final QName rpcQName) {
        final Document document = XmlUtil.newDocument();
        final Element rpcNS = document.createElementNS(NETCONF_RPC_QNAME.getNamespace().toString(), NETCONF_RPC_QNAME.getLocalName());
        final Element elementNS = document.createElementNS(rpcQName.getNamespace().toString(), rpcQName.getLocalName());
        rpcNS.appendChild(elementNS);
        document.appendChild(rpcNS);
        return new DOMResult(elementNS);
    }

    private void writeNormalizedRpc(final ContainerNode normalized, final DOMResult result, final SchemaPath schemaPath, final SchemaContext baseNetconfCtx) throws IOException, XMLStreamException {
        final NormalizedNodeWriter normalizedNodeWriter;
        NormalizedNodeStreamWriter normalizedNodeStreamWriter = null;
        XMLStreamWriter writer = null;
        try {
            writer = NetconfMessageTransformUtil.XML_FACTORY.createXMLStreamWriter(result);
            normalizedNodeStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(writer, baseNetconfCtx, schemaPath);
            normalizedNodeWriter = NormalizedNodeWriter.forStreamWriter(normalizedNodeStreamWriter);

            for (final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> editElement : normalized.getValue()) {
                normalizedNodeWriter.write(editElement);
            }
            normalizedNodeWriter.flush();
        } finally {
            try {
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

    @Override
    public synchronized DOMRpcResult toRpcResult(final NetconfMessage message, final SchemaPath rpc) {
        final NormalizedNode<?, ?> normalizedNode;
        if (NetconfMessageTransformUtil.isDataRetrievalOperation(rpc.getLastComponent())) {
            final Element xmlData = NetconfMessageTransformUtil.getDataSubtree(message.getDocument());
            final ContainerSchemaNode schemaForDataRead = NetconfMessageTransformUtil.createSchemaForDataRead(schemaContext);
            final ContainerNode dataNode = NORMALIZED_NODE_PARSER_FACTORY.getContainerNodeParser().parse(Collections.singleton(xmlData), schemaForDataRead);

            normalizedNode = Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NetconfMessageTransformUtil.NETCONF_RPC_REPLY_QNAME))
                    .withChild(dataNode).build();
        } else {
            final Set<Element> documentElement = Collections.singleton(message.getDocument().getDocumentElement());
            final RpcDefinition rpcDefinition = mappedRpcs.get(rpc.getLastComponent());
            Preconditions.checkArgument(rpcDefinition != null, "Unable to parse response of %s, the rpc is unknown", rpc.getLastComponent());

            // In case no input for rpc is defined, we can simply construct the payload here
            if (rpcDefinition.getOutput() == null) {
                Preconditions.checkArgument(XmlElement.fromDomDocument(message.getDocument()).getOnlyChildElementWithSameNamespaceOptionally("ok").isPresent(),
                        "Unexpected content in response of rpc: %s, %s", rpcDefinition.getQName(), message);
                normalizedNode = null;
            } else {
                normalizedNode = NORMALIZED_NODE_PARSER_FACTORY.getContainerNodeParser().parse(documentElement, rpcDefinition.getOutput());
            }
        }
        return new DefaultDOMRpcResult(normalizedNode);
    }

}
