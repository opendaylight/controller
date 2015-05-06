/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.annotations.Beta;
import java.util.EventListener;
import java.util.Set;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Listener which is notified when subscriptions changes and
 * provides set of notification types for which currently
 * subscriptions are in place.
 *
 */
@Beta
public interface DOMNotificationSubscriptionListener extends EventListener {

    /**
     * Invoked when notification subscription changed
     *
     * @param currentTypes Set of notification types
     * for which listeners are registered.
     */
    void onSubscriptionChanged(Set<SchemaPath> currentTypes);

}
