/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connect.netconf.schema.mapping;

import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.EVENT_TIME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_RPC_QNAME;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.NETCONF_URI;
import static org.opendaylight.controller.sal.connect.netconf.util.NetconfMessageTransformUtil.toPath;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcResult;
import org.opendaylight.controller.md.sal.dom.spi.DefaultDOMRpcResult;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.notifications.NetconfNotification;
import org.opendaylight.controller.netconf.util.OrderedNormalizedNodeWriter;
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
            moduleInfoBackedContext.addModuleInfos(
                    Lists.newArrayList(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.$YangModuleInfoImpl.getInstance()));
            BASE_NETCONF_CTX = moduleInfoBackedContext.tryToCreateSchemaContext().get();
        } catch (final RuntimeException e) {
            LOG.error("Unable to prepare schema context for base netconf ops", e);
            throw new ExceptionInInitializerError(e);
        }
    }
    private static final Map<QName, RpcDefinition> MAPPED_BASE_RPCS = Maps.uniqueIndex(BASE_NETCONF_CTX.getOperations(), QNAME_FUNCTION);

    private final SchemaContext schemaContext;
    private final MessageCounter counter;
    private final Map<QName, RpcDefinition> mappedRpcs;
    private final Multimap<QName, NotificationDefinition> mappedNotifications;
    private final DomToNormalizedNodeParserFactory parserFactory;

    public NetconfMessageTransformer(final SchemaContext schemaContext, final boolean strictParsing) {
        this.counter = new MessageCounter();
        this.schemaContext = schemaContext;
        parserFactory = DomToNormalizedNodeParserFactory.getInstance(XmlUtils.DEFAULT_XML_CODEC_PROVIDER, schemaContext, strictParsing);

        mappedRpcs = Maps.uniqueIndex(schemaContext.getOperations(), QNAME_FUNCTION);
        mappedNotifications = Multimaps.index(schemaContext.getNotifications(), QNAME_NOREV_FUNCTION);
    }

    @Override
    public synchronized DOMNotification toNotification(final NetconfMessage message) {
        final Map.Entry<Date, XmlElement> stripped = stripNotification(message);
        final QName notificationNoRev;
        try {
            notificationNoRev = QName.create(stripped.getValue().getNamespace(), stripped.getValue().getName()).withoutRevision();
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
        final ContainerNode content = parserFactory.getContainerNodeParser().parse(Collections.singleton(stripped.getValue().getDomElement()),
                notificationAsContainerSchemaNode);
        return new NetconfDeviceNotification(content, stripped.getKey());
    }

    private static final ThreadLocal<SimpleDateFormat> EVENT_TIME_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(NetconfNotification.RFC3339_DATE_FORMAT_BLUEPRINT);
        }

        public void set(SimpleDateFormat value) {
            throw new UnsupportedOperationException();
        }
    };

    // FIXME move somewhere to util
    private static Map.Entry<Date, XmlElement> stripNotification(final NetconfMessage message) {
        final XmlElement xmlElement = XmlElement.fromDomDocument(message.getDocument());
        final List<XmlElement> childElements = xmlElement.getChildElements();
        Preconditions.checkArgument(childElements.size() == 2, "Unable to parse notification %s, unexpected format", message);

        final XmlElement eventTimeElement;
        final XmlElement notificationElement;

        if (childElements.get(0).getName().equals(EVENT_TIME)) {
            eventTimeElement = childElements.get(0);
            notificationElement = childElements.get(1);
        }
        else if(childElements.get(1).getName().equals(EVENT_TIME)) {
            eventTimeElement = childElements.get(1);
            notificationElement = childElements.get(0);
        } else {
            throw new IllegalArgumentException("Notification payload does not contain " + EVENT_TIME + " " + message);
        }

        try {
            return new AbstractMap.SimpleEntry<>(EVENT_TIME_FORMAT.get().parse(eventTimeElement.getTextContent()), notificationElement);
        } catch (NetconfDocumentedException e) {
            throw new IllegalArgumentException("Notification payload does not contain " + EVENT_TIME + " " + message);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Notification event time in wrong format " + EVENT_TIME + " " + message);
        }
    }

    @Override
    public NetconfMessage toRpcRequest(SchemaPath rpc, final NormalizedNode<?, ?> payload) {
        // In case no input for rpc is defined, we can simply construct the payload here
        final QName rpcQName = rpc.getLastComponent();
        Map<QName, RpcDefinition> currentMappedRpcs = mappedRpcs;

        // Determine whether a base netconf operation is being invoked and also check if the device exposed model for base netconf
        // If no, use pre built base netconf operations model
        final boolean needToUseBaseCtx = mappedRpcs.get(rpcQName) == null && isBaseRpc(rpcQName);
        if(needToUseBaseCtx) {
            currentMappedRpcs = MAPPED_BASE_RPCS;
        }

        Preconditions.checkNotNull(currentMappedRpcs.get(rpcQName), "Unknown rpc %s, available rpcs: %s", rpcQName, currentMappedRpcs.keySet());
        if(currentMappedRpcs.get(rpcQName).getInput() == null) {
            return new NetconfMessage(prepareDomResultForRpcRequest(rpcQName).getNode().getOwnerDocument());
        }

        Preconditions.checkNotNull(payload, "Transforming an rpc with input: %s, payload cannot be null", rpcQName);
        Preconditions.checkArgument(payload instanceof ContainerNode,
                "Transforming an rpc with input: %s, payload has to be a container, but was: %s", rpcQName, payload);

        // Set the path to the input of rpc for the node stream writer
        rpc = rpc.createChild(QName.cachedReference(QName.create(rpcQName, "input")));
        final DOMResult result = prepareDomResultForRpcRequest(rpcQName);

        try {
            // If the schema context for netconf device does not contain model for base netconf operations, use default pre build context with just the base model
            // This way operations like lock/unlock are supported even if the source for base model was not provided
            writeNormalizedRpc(((ContainerNode) payload), result, rpc, needToUseBaseCtx ? BASE_NETCONF_CTX : schemaContext);
        } catch (final XMLStreamException | IOException | IllegalStateException e) {
            throw new IllegalStateException("Unable to serialize " + rpc, e);
        }

        final Document node = result.getNode().getOwnerDocument();

        return new NetconfMessage(node);
    }

    private static boolean isBaseRpc(final QName rpc) {
        return rpc.getNamespace().equals(NETCONF_URI);
    }

    private DOMResult prepareDomResultForRpcRequest(final QName rpcQName) {
        final Document document = XmlUtil.newDocument();
        final Element rpcNS = document.createElementNS(NETCONF_RPC_QNAME.getNamespace().toString(), NETCONF_RPC_QNAME.getLocalName());
        // set msg id
        rpcNS.setAttribute(NetconfMessageTransformUtil.MESSAGE_ID_ATTR, counter.getNewMessageId(MESSAGE_ID_PREFIX));
        final Element elementNS = document.createElementNS(rpcQName.getNamespace().toString(), rpcQName.getLocalName());
        rpcNS.appendChild(elementNS);
        document.appendChild(rpcNS);
        return new DOMResult(elementNS);
    }

    private void writeNormalizedRpc(final ContainerNode normalized, final DOMResult result, final SchemaPath schemaPath, final SchemaContext baseNetconfCtx) throws IOException, XMLStreamException {
        final OrderedNormalizedNodeWriter normalizedNodeWriter;
        NormalizedNodeStreamWriter normalizedNodeStreamWriter = null;
        XMLStreamWriter writer = null;
        try {
            writer = NetconfMessageTransformUtil.XML_FACTORY.createXMLStreamWriter(result);
            normalizedNodeStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(writer, baseNetconfCtx, schemaPath);
            normalizedNodeWriter = new OrderedNormalizedNodeWriter(normalizedNodeStreamWriter, baseNetconfCtx, schemaPath);
            Collection<DataContainerChild<?, ?>> value = (Collection) normalized.getValue();
            normalizedNodeWriter.write(value);
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
        final QName rpcQName = rpc.getLastComponent();
        if (NetconfMessageTransformUtil.isDataRetrievalOperation(rpcQName)) {
            final Element xmlData = NetconfMessageTransformUtil.getDataSubtree(message.getDocument());
            final ContainerSchemaNode schemaForDataRead = NetconfMessageTransformUtil.createSchemaForDataRead(schemaContext);
            final ContainerNode dataNode = parserFactory.getContainerNodeParser().parse(Collections.singleton(xmlData), schemaForDataRead);

            normalizedNode = Builders.containerBuilder().withNodeIdentifier(new YangInstanceIdentifier.NodeIdentifier(NetconfMessageTransformUtil.NETCONF_RPC_REPLY_QNAME))
                    .withChild(dataNode).build();
        } else {
            final Set<Element> documentElement = Collections.singleton(message.getDocument().getDocumentElement());

            Map<QName, RpcDefinition> currentMappedRpcs = mappedRpcs;

            // Determine whether a base netconf operation is being invoked and also check if the device exposed model for base netconf
            // If no, use pre built base netconf operations model
            final boolean needToUseBaseCtx = mappedRpcs.get(rpcQName) == null && isBaseRpc(rpcQName);
            if(needToUseBaseCtx) {
                currentMappedRpcs = MAPPED_BASE_RPCS;
            }

            final RpcDefinition rpcDefinition = currentMappedRpcs.get(rpcQName);
            Preconditions.checkArgument(rpcDefinition != null, "Unable to parse response of %s, the rpc is unknown", rpcQName);

            // In case no input for rpc is defined, we can simply construct the payload here
            if (rpcDefinition.getOutput() == null) {
                Preconditions.checkArgument(XmlElement.fromDomDocument(message.getDocument()).getOnlyChildElementWithSameNamespaceOptionally("ok").isPresent(),
                        "Unexpected content in response of rpc: %s, %s", rpcDefinition.getQName(), message);
                normalizedNode = null;
            } else {
                normalizedNode = parserFactory.getContainerNodeParser().parse(documentElement, rpcDefinition.getOutput());
            }
        }
        return new DefaultDOMRpcResult(normalizedNode);
    }

    private static class NetconfDeviceNotification implements DOMNotification, DOMEvent {
        private final ContainerNode content;
        private final SchemaPath schemaPath;
        private final Date eventTime;

        NetconfDeviceNotification(final ContainerNode content, final Date eventTime) {
            this.content = content;
            this.eventTime = eventTime;
            this.schemaPath = toPath(content.getNodeType());
        }

        @Nonnull
        @Override
        public SchemaPath getType() {
            return schemaPath;

        }

        @Nonnull
        @Override
        public ContainerNode getBody() {
            return content;
        }

        @Override
        public Date getEventTime() {
            return eventTime;
        }
    }
}
