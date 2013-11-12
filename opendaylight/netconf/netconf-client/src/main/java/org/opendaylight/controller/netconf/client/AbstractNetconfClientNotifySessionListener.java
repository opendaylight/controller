/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;

/**
 * Class extending {@link NetconfClientSessionListener} to provide notification capability.
 */
public abstract class AbstractNetconfClientNotifySessionListener extends NetconfClientSessionListener {
    /*
     * Maybe some capabilities could be expressed as internal NetconfClientSessionListener handlers.
     * It would enable NetconfClient functionality to be extended by using namespace handlers.
     * So far let just enable notification capability by extending and let parent class intact.
     */

    /**
     * As class purpose is to provide notification capability to session listener
     * onMessage method is not allowed to be further overridden.
     * {@see #onNotification(NetconfClientSession, NetconfMessage)}
     *
     * @param session {@see NetconfClientSessionListener#onMessage(NetconfClientSession, NetconfMessage)}
     * @param message {@see NetconfClientSessionListener#onMessage(NetconfClientSession, NetconfMessage)}
     */
    @Override
    public final synchronized void onMessage(NetconfClientSession session, NetconfMessage message) {
        if (isNotification(message)) {
            onNotification(session, message);
        } else {
            super.onMessage(session, message);
        }
    }

    /**
     * Method intended to customize notification processing.
     *
     * @param session {@see NetconfClientSessionListener#onMessage(NetconfClientSession, NetconfMessage)}
     * @param message {@see NetconfClientSessionListener#onMessage(NetconfClientSession, NetconfMessage)}
     */
    public abstract void onNotification(NetconfClientSession session, NetconfMessage message);

    private boolean isNotification(NetconfMessage message) {
        XmlElement xmle = XmlElement.fromDomDocument(message.getDocument());
        return XmlNetconfConstants.NOTIFICATION_ELEMENT_NAME.equals(xmle.getName()) ;
    }
}
