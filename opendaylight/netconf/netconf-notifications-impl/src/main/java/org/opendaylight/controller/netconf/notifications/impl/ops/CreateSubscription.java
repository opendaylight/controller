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
import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.mapping.api.SessionAwareNetconfOperation;
import org.opendaylight.controller.netconf.notifications.NetconfNotification;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationListener;
import org.opendaylight.controller.netconf.notifications.NetconfNotificationRegistry;
import org.opendaylight.controller.netconf.notifications.NotificationListenerRegistration;
import org.opendaylight.controller.netconf.notifications.impl.NetconfNotificationManager;
import org.opendaylight.controller.netconf.util.mapping.AbstractLastNetconfOperation;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.CreateSubscriptionInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.StreamNameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create subscription listens for create subscription requests and registers notification listeners into notification registry.
 * Received notifications are sent to the client right away
 */
public class CreateSubscription extends AbstractLastNetconfOperation implements SessionAwareNetconfOperation, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CreateSubscription.class);

    static final String CREATE_SUBSCRIPTION = "create-subscription";

    private final NetconfNotificationRegistry notifications;
    private final List<NotificationListenerRegistration> subscriptions = Lists.newArrayList();
    private NetconfSession netconfSession;

    public CreateSubscription(final String netconfSessionIdForReporting, final NetconfNotificationRegistry notifications) {
        super(netconfSessionIdForReporting);
        this.notifications = notifications;
    }

    @Override
    protected Element handleWithNoSubsequentOperations(final Document document, final XmlElement operationElement) throws NetconfDocumentedException {
        operationElement.checkName(CREATE_SUBSCRIPTION);
        operationElement.checkNamespace(CreateSubscriptionInput.QNAME.getNamespace().toString());
        // FIXME reimplement using CODEC_REGISTRY and parse everything into generated class instance
        // Waiting ofr https://git.opendaylight.org/gerrit/#/c/13763/

        // FIXME filter could be supported same way as netconf server filters get and get-config results
        final Optional<XmlElement> filter = operationElement.getOnlyChildElementWithSameNamespaceOptionally("filter");
        Preconditions.checkArgument(filter.isPresent() == false, "Filter element not yet supported");

        // Replay not supported
        final Optional<XmlElement> startTime = operationElement.getOnlyChildElementWithSameNamespaceOptionally("startTime");
        Preconditions.checkArgument(startTime.isPresent() == false, "StartTime element not yet supported");

        // Stop time not supported
        final Optional<XmlElement> stopTime = operationElement.getOnlyChildElementWithSameNamespaceOptionally("stopTime");
        Preconditions.checkArgument(stopTime.isPresent() == false, "StopTime element not yet supported");

        final StreamNameType streamNameType = parseStreamIfPresent(operationElement);

        Preconditions.checkNotNull(netconfSession);
        // Premature streams are allowed (meaning listener can register even if no provider is available yet)
        if(notifications.isStreamAvailable(streamNameType) == false) {
            LOG.warn("Registering premature stream {}. No publisher available yet for session {}", streamNameType, getNetconfSessionIdForReporting());
        }

        final NotificationListenerRegistration notificationListenerRegistration =
                notifications.registerNotificationListener(streamNameType, new NotificationSubscription(netconfSession));
        subscriptions.add(notificationListenerRegistration);

        return XmlUtil.createElement(document, XmlNetconfConstants.OK, Optional.<String>absent());
    }

    private StreamNameType parseStreamIfPresent(final XmlElement operationElement) throws NetconfDocumentedException {
        final Optional<XmlElement> stream = operationElement.getOnlyChildElementWithSameNamespaceOptionally("stream");
        return stream.isPresent() ? new StreamNameType(stream.get().getTextContent()) : NetconfNotificationManager.BASE_STREAM_NAME;
    }

    @Override
    protected String getOperationName() {
        return CREATE_SUBSCRIPTION;
    }

    @Override
    protected String getOperationNamespace() {
        return CreateSubscriptionInput.QNAME.getNamespace().toString();
    }

    @Override
    public void setSession(final NetconfSession session) {
        this.netconfSession = session;
    }

    @Override
    public void close() {
        netconfSession = null;
        // Unregister from notification streams
        for (final NotificationListenerRegistration subscription : subscriptions) {
            subscription.close();
        }
    }

    private static class NotificationSubscription implements NetconfNotificationListener {
        private final NetconfSession currentSession;

        public NotificationSubscription(final NetconfSession currentSession) {
            this.currentSession = currentSession;
        }

        @Override
        public void onNotification(final StreamNameType stream, final NetconfNotification notification) {
            currentSession.sendMessage(notification);
        }
    }
}
