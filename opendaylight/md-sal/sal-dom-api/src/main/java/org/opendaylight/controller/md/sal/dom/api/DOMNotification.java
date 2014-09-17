/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * A single YANG notification.
 */
public interface DOMNotification {
    /**
     * Return the type of this notification.
     *
     * @return Notification type.
     */
    @Nonnull SchemaPath getType();

    /**
     * Return the body of this notification.
     *
     * @return Notification body.
     */
    @Nonnull ContainerNode getBody();
}
