/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.notifications.impl.ops;

import com.google.common.base.Preconditions;
import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.controller.netconf.util.mapping.AbstractNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.Netconf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.NetconfBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.netconf.Streams;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Get extends AbstractNetconfOperation implements AutoCloseable {

    private static final String GET = "get";
    private static final InstanceIdentifier<Netconf> NETCONF_SUBTREE_INSTANCE_IDENTIFIER = InstanceIdentifier.builder(Netconf.class).build();

    private final NetconfNotificationRegistry notificationRegistry;

    public Get(final String netconfSessionIdForReporting, final NetconfNotificationRegistry notificationRegistry) {
        super(netconfSessionIdForReporting);
        Preconditions.checkNotNull(notificationRegistry);
        this.notificationRegistry = notificationRegistry;
    }

    @Override
    protected String getOperationName() {
        return GET;
    }

    @Override
    public Document handle(final Document requestMessage, final NetconfOperationChainedExecution subsequentOperation) throws NetconfDocumentedException {
        final Document partialResponse = subsequentOperation.execute(requestMessage);
        final Streams availableStreams = notificationRegistry.getAvailableStreams();
        if(availableStreams.getStream().isEmpty() == false) {
            serializeStreamsSubtree(partialResponse, availableStreams);
        }
        return partialResponse;
    }

    static void serializeStreamsSubtree(final Document partialResponse, final Streams availableStreams) throws NetconfDocumentedException {
        final Netconf netconfSubtree = new NetconfBuilder().setStreams(availableStreams).build();
        final NormalizedNode<?, ?> normalized = toNormalized(netconfSubtree);

        final DOMResult result = new DOMResult(getPlaceholder(partialResponse));

        try {
            NotificationsTransformUtil.writeNormalizedNode(normalized, result, SchemaPath.ROOT);
        } catch (final XMLStreamException | IOException e) {
            throw new IllegalStateException("Unable to serialize " + netconfSubtree, e);
        }
    }

    private static Element getPlaceholder(final Document innerResult)
            throws NetconfDocumentedException {
        final XmlElement rootElement = XmlElement.fromDomElementWithExpected(
                innerResult.getDocumentElement(), XmlNetconfConstants.RPC_REPLY_KEY, XmlNetconfConstants.RFC4741_TARGET_NAMESPACE);
        return rootElement.getOnlyChildElement(XmlNetconfConstants.DATA_KEY).getDomElement();
    }

    private static NormalizedNode<?, ?> toNormalized(final Netconf netconfSubtree) {
        return NotificationsTransformUtil.CODEC_REGISTRY.toNormalizedNode(NETCONF_SUBTREE_INSTANCE_IDENTIFIER, netconfSubtree).getValue();
    }

    @Override
    protected Element handle(final Document document, final XmlElement message, final NetconfOperationChainedExecution subsequentOperation)
            throws NetconfDocumentedException {
        throw new UnsupportedOperationException("Never gets called");
    }

    @Override
    protected HandlingPriority getHandlingPriority() {
        return HandlingPriority.HANDLE_WITH_DEFAULT_PRIORITY.increasePriority(2);
    }

    @Override
    public void close() throws Exception {

    }
}
