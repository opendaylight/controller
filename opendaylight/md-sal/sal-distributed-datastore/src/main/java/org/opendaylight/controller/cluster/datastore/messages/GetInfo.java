/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.messages;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.mgmt.api.DataTreeListenerInfo;

/**
 * Local message sent to an actor to retrieve {@link DataTreeListenerInfo} for reporting.
 *
 * @author Thomas Pantelis
 */
@NonNullByDefault
public final class GetInfo {
    public static final GetInfo INSTANCE = new GetInfo();

    private GetInfo() {
        // Hidden on purpose
    }
}
