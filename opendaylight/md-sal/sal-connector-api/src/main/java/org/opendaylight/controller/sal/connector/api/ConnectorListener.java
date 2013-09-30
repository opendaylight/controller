/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.connector.api;

import java.util.Set;

import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

public interface ConnectorListener {

    void onPrefixesAnnounced(Set<InstanceIdentifier> prefixes);
    void onPrefixesWithdrawn(Set<InstanceIdentifier> prefixes);

}
