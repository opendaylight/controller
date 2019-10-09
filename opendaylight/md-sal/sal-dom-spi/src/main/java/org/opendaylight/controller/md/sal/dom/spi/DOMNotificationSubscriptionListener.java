/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.annotations.Beta;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Listener which is notified when subscriptions changes and
 * provides set of notification types for which currently
 * subscriptions are in place.
 *
 * @deprecated Use {@link org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListener} instead.
 */
@Beta
@Deprecated(forRemoval = true)
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE", justification = "Migration")
public interface DOMNotificationSubscriptionListener
        extends org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListener {
}
