/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import org.opendaylight.controller.sal.binding.spi.MappingProvider.MappingExtension;
import org.opendaylight.controller.yang.binding.Notification;
import org.opendaylight.controller.yang.binding.NotificationListener;

public interface NotificationInvoker extends MappingExtension {
    void notify(Notification notification, NotificationListener listener);
}