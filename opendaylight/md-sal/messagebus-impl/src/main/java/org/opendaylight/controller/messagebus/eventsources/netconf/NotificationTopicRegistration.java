/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messagebus.eventsources.netconf;

import java.util.ArrayList;

import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class NotificationTopicRegistration implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationTopicRegistration.class);

    public enum NotificationSourceType{
        NetconfDeviceStream,
        ConnectionStatusChange;
    }

    private boolean active;
    private final NotificationSourceType notificationSourceType;
    private final String sourceName;
    private final String notificationUrnPrefix;
    private boolean replaySupported;

    protected NotificationTopicRegistration(NotificationSourceType notificationSourceType, String sourceName, String notificationUrnPrefix) {
        this.notificationSourceType = notificationSourceType;
        this.sourceName = sourceName;
        this.notificationUrnPrefix = notificationUrnPrefix;
        this.active = false;
        this.setReplaySupported(false);
    }

    public boolean isActive() {
        return active;
    }

    protected void setActive(boolean active) {
        this.active = active;
    }

    public NotificationSourceType getNotificationSourceType() {
        return notificationSourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getNotificationUrnPrefix() {
        return notificationUrnPrefix;
    }

    public boolean checkNotificationPath(SchemaPath notificationPath){
        if(notificationPath == null){
            return false;
        }
        String nameSpace = notificationPath.getLastComponent().toString();
        LOG.debug("CheckNotification - name space {} - NotificationUrnPrefix {}", nameSpace, getNotificationUrnPrefix());
        return nameSpace.startsWith(getNotificationUrnPrefix());
    }
    abstract void activateNotificationSource();

    abstract void deActivateNotificationSource();

    abstract void reActivateNotificationSource();

    abstract boolean registerNotificationTopic(SchemaPath notificationPath, TopicId topicId);

    abstract void unRegisterNotificationTopic(TopicId topicId);

    abstract ArrayList<TopicId> getNotificationTopicIds(SchemaPath notificationPath);

    public boolean isReplaySupported() {
        return replaySupported;
    }

    protected void setReplaySupported(boolean replaySupported) {
        this.replaySupported = replaySupported;
    }

}
