/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.test;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

public class XmlAndJsonToCnSnLeafRefTest extends YangAndXmlAndDataSchemaLoader {

    final QName refContQName = QName.create("referenced:module", "2014-04-17", "cont");
    final QName refLf1QName = QName.create(refContQName, "lf1");
    final QName contQName = QName.create("leafref:module", "2014-04-17", "cont");
    final QName lf1QName = QName.create(contQName, "lf1");
    final QName lf2QName = QName.create(contQName, "lf2");
    final QName lf3QName = QName.create(contQName, "lf3");

    @BeforeClass
    public static void initialize() throws URISyntaxException, ReactorException, FileNotFoundException {
        dataLoad("/leafref/yang", 2, "leafref-module", "cont");
    }


}
