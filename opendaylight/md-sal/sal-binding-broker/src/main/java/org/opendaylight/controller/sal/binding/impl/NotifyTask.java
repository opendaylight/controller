/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.concurrent.Callable;

import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.util.ToStringHelper;
import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyTask implements Callable<Object> {
    private static Logger log = new Function0<Logger>() {
        @Override
        public Logger apply() {
            Logger _logger = LoggerFactory.getLogger(NotifyTask.class);
            return _logger;
        }
    }.apply();

    @SuppressWarnings("rawtypes")
    private final NotificationListener _listener;

    public NotificationListener getListener() {
        return this._listener;
    }

    private final Notification _notification;

    public Notification getNotification() {
        return this._notification;
    }

    @Override
    public Object call() {
        try {
            boolean _isDebugEnabled = NotifyTask.log.isDebugEnabled();
            if (_isDebugEnabled) {
                Notification _notification = this.getNotification();
                NotificationListener _listener = this.getListener();
                NotifyTask.log.debug("Delivering notification {} to {}", _notification, _listener);
            } else {
                Notification _notification_1 = this.getNotification();
                Class<? extends Notification> _class = _notification_1.getClass();
                String _name = _class.getName();
                NotificationListener _listener_1 = this.getListener();
                NotifyTask.log.trace("Delivering notification {} to {}", _name, _listener_1);
            }
            NotificationListener _listener_2 = this.getListener();
            Notification _notification_2 = this.getNotification();
            _listener_2.onNotification(_notification_2);
            boolean _isDebugEnabled_1 = NotifyTask.log.isDebugEnabled();
            if (_isDebugEnabled_1) {
                Notification _notification_3 = this.getNotification();
                NotificationListener _listener_3 = this.getListener();
                NotifyTask.log.debug("Notification delivered {} to {}", _notification_3, _listener_3);
            } else {
                Notification _notification_4 = this.getNotification();
                Class<? extends Notification> _class_1 = _notification_4.getClass();
                String _name_1 = _class_1.getName();
                NotificationListener _listener_4 = this.getListener();
                NotifyTask.log.trace("Notification delivered {} to {}", _name_1, _listener_4);
            }
        } catch (final Throwable _t) {
            if (_t instanceof Exception) {
                final Exception e = (Exception)_t;
                NotificationListener _listener_5 = this.getListener();
                NotifyTask.log.error("Unhandled exception thrown by listener: {}", _listener_5, e);
            } else {
                throw Exceptions.sneakyThrow(_t);
            }
        }
        return null;
    }

    public NotifyTask(final NotificationListener listener, final Notification notification) {
        super();
        this._listener = listener;
        this._notification = notification;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_listener== null) ? 0 : _listener.hashCode());
        result = prime * result + ((_notification== null) ? 0 : _notification.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NotifyTask other = (NotifyTask) obj;
        if (_listener == null) {
            if (other._listener != null)
                return false;
        } else if (!_listener.equals(other._listener))
            return false;
        if (_notification == null) {
            if (other._notification != null)
                return false;
        } else if (!_notification.equals(other._notification))
            return false;
        return true;
    }

    @Override
    public String toString() {
        String result = new ToStringHelper().toString(this);
        return result;
    }
}
