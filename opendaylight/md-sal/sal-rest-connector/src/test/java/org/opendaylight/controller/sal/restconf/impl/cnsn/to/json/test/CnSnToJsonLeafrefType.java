/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.restconf.impl.cnsn.to.json.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.opendaylight.controller.sal.restconf.impl.test.YangAndXmlAndDataSchemaLoader;

/**
 *
 * All tests are commented now because leafref isn't supported now
 *
 */

public class CnSnToJsonLeafrefType extends YangAndXmlAndDataSchemaLoader {

    @BeforeClass
    public static void initialization() {
        dataLoad("/cnsn-to-json/leafref", 2, "main-module", "cont");
    }

    private void validateJson(final String regex, final String value) {
        assertNotNull(value);
        final Pattern ptrn = Pattern.compile(regex, Pattern.DOTALL);
        final Matcher mtch = ptrn.matcher(value);
        assertTrue(mtch.matches());
    }

}
