/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.annotations.Beta;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

/**
 * Registry of {@link DOMNotificationSubscriptionListener}
 * which listens for changes in notification types.
 *
 */
@Beta
public interface DOMNotificationSubscriptionListenerRegistry  {

    <L extends DOMNotificationSubscriptionListener> ListenerRegistration<L> registerSubscriptionListener(L listener);

}
