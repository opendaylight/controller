/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api.jmx;

import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

import javax.management.ObjectName;

public interface DefaultCommitOperationMXBean {

    static String typeName = "NetconfNotificationProvider";
    public static ObjectName objectName = ObjectNameUtil.createONWithDomainAndType(typeName);

}
