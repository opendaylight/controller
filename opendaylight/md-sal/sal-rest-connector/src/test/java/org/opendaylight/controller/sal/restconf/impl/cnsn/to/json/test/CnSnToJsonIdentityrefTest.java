/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

import org.junit.BeforeClass;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;
import org.opendaylight.yangtools.yang.parser.spi.meta.ReactorException;

public class CnSnToJsonIdentityrefTest extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() throws URISyntaxException, ReactorException, FileNotFoundException {
        dataLoad("/cnsn-to-json/identityref", 2, "identityref-module", "cont");
    }

}
