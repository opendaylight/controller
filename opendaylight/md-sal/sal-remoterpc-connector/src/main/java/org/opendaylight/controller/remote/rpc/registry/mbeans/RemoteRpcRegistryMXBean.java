/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry.mbeans;


import java.util.Map;
import java.util.Set;

/**
 * JMX bean to check remote rpc registry
 */

public interface RemoteRpcRegistryMXBean {

    Set<String> getGlobalRpc();

    String getBucketVersions();

    Set<String> getLocalRegisteredRoutedRpc();

    Map<String,String> findRpcByName(String name);

    Map<String,String> findRpcByRoute(String route);
}
