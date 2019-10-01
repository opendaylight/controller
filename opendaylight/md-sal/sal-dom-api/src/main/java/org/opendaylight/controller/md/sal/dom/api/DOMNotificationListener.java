/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import java.util.EventListener;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Interface implemented by listeners interested in {@link DOMNotification}s.
 * @deprecated Use {@link org.opendaylight.mdsal.dom.api.DOMNotificationListener} instead.
 */
@Deprecated(forRemoval = true)
public interface DOMNotificationListener extends EventListener {
    /**
     * Invoked whenever a {@link DOMNotification} matching the subscription
     * criteria is received.
     *
     * @param notification Received notification
     */
    void onNotification(@NonNull DOMNotification notification);
}
