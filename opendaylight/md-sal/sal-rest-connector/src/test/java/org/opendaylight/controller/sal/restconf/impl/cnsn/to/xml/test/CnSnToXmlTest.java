/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.xml.test;

import org.junit.BeforeClass;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

/**
 *
 * CnSn = Composite node and Simple node data structure Class contains test of serializing simple nodes data values
 * according data types from YANG schema to XML file
 *
 */
public class CnSnToXmlTest extends YangAndXmlAndDataSchemaLoader {
    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-xml/yang", 2, "basic-module", "cont");
    }

}