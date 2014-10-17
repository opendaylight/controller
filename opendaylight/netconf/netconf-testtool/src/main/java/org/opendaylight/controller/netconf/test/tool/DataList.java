/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.test.tool;

import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.netconf.util.xml.XmlElement;

public class DataList {
    private static List<XmlElement> configList = Collections.emptyList();

    public static List<XmlElement> getConfigList() {
        return configList;
    }

    public static void setConfigList(List<XmlElement> configList) {
        DataList.configList = configList;
    }

    public static void resetConfigList()
    {
        configList = Collections.emptyList();
    }

}
