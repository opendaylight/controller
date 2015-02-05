/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications.impl.ops;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import javassist.ClassPool;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.netconf.notifications.NetconfNotification;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.NetconfCapabilityChange;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.impl.codec.xml.XMLStreamNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public final class NotificationsTransformUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationsTransformUtil.class);

    private NotificationsTransformUtil() {}

    static final SchemaContext NOTIFICATIONS_SCHEMA_CTX;
    static final BindingNormalizedNodeCodecRegistry CODEC_REGISTRY;
    static final XMLOutputFactory XML_FACTORY;
    static final RpcDefinition CREATE_SUBSCRIPTION_RPC;

    static final SchemaPath CAPABILITY_CHANGE_SCHEMA_PATH = SchemaPath.create(true, NetconfCapabilityChange.QNAME);

    static {
        XML_FACTORY = XMLOutputFactory.newFactory();
        XML_FACTORY.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);

        final ModuleInfoBackedContext moduleInfoBackedContext = ModuleInfoBackedContext.create();
        moduleInfoBackedContext.addModuleInfos(Collections.singletonList($YangModuleInfoImpl.getInstance()));
        moduleInfoBackedContext.addModuleInfos(Collections.singletonList(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206.$YangModuleInfoImpl.getInstance()));
        final Optional<SchemaContext> schemaContextOptional = moduleInfoBackedContext.tryToCreateSchemaContext();
        Preconditions.checkState(schemaContextOptional.isPresent());
        NOTIFICATIONS_SCHEMA_CTX = schemaContextOptional.get();

        CREATE_SUBSCRIPTION_RPC = Preconditions.checkNotNull(findCreateSubscriptionRpc());

        Preconditions.checkNotNull(CREATE_SUBSCRIPTION_RPC);

        final JavassistUtils javassist = JavassistUtils.forClassPool(ClassPool.getDefault());
        CODEC_REGISTRY = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(javassist));
        CODEC_REGISTRY.onBindingRuntimeContextUpdated(BindingRuntimeContext.create(moduleInfoBackedContext, NOTIFICATIONS_SCHEMA_CTX));
    }

    private static RpcDefinition findCreateSubscriptionRpc() {
        return Iterables.getFirst(Collections2.filter(NOTIFICATIONS_SCHEMA_CTX.getOperations(), new Predicate<RpcDefinition>() {
            @Override
            public boolean apply(final RpcDefinition input) {
                return input.getQName().getLocalName().equals(CreateSubscription.CREATE_SUBSCRIPTION);
            }
        }), null);
    }

    public static NetconfNotification transform(final NetconfCapabilityChange capabilityChange) {
        return transform(capabilityChange, Optional.<Date>absent());
    }

    public static NetconfNotification transform(final NetconfCapabilityChange capabilityChange, final Date eventTime) {
        return transform(capabilityChange, Optional.fromNullable(eventTime));
    }

    private static NetconfNotification transform(final NetconfCapabilityChange capabilityChange, final Optional<Date> eventTime) {
        final ContainerNode containerNode = CODEC_REGISTRY.toNormalizedNodeNotification(capabilityChange);
        final DOMResult result = new DOMResult(XmlUtil.newDocument());
        try {
            writeNormalizedNode(containerNode, result, CAPABILITY_CHANGE_SCHEMA_PATH);
        } catch (final XMLStreamException| IOException e) {
            throw new IllegalStateException("Unable to serialize " + capabilityChange, e);
        }
        final Document node = (Document) result.getNode();
        return eventTime.isPresent() ?
                new NetconfNotification(node, eventTime.get()):
                new NetconfNotification(node);
    }

    static void writeNormalizedNode(final NormalizedNode<?, ?> normalized, final DOMResult result, final SchemaPath schemaPath) throws IOException, XMLStreamException {
        NormalizedNodeWriter normalizedNodeWriter = null;
        NormalizedNodeStreamWriter normalizedNodeStreamWriter = null;
        XMLStreamWriter writer = null;
        try {
            writer = XML_FACTORY.createXMLStreamWriter(result);
            normalizedNodeStreamWriter = XMLStreamNormalizedNodeStreamWriter.create(writer, NOTIFICATIONS_SCHEMA_CTX, schemaPath);
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
