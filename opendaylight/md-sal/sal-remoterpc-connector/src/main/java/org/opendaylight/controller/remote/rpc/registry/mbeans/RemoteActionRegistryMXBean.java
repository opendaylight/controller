/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import java.util.Map;
import java.util.Set;

public interface RemoteActionRegistryMXBean {

    String getBucketVersions();

    Set<String> getLocalRegisteredAction();

    Map<String,String> findActionByName(String name);

    Map<String,String> findActionByRoute(String route);
}
