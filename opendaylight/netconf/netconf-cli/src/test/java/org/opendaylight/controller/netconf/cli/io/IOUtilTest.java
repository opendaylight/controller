/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.cli.io;

import com.google.common.collect.Maps;
import java.util.Map;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.netconf.cli.commands.CommandConstants;
import org.opendaylight.yangtools.yang.common.QName;

public class IOUtilTest {

    @Test
    public void testQNameFromKeyStringNew() throws Exception {
        final String s = IOUtil.qNameToKeyString(CommandConstants.HELP_QNAME, "module");
        final Map<String, QName> modulesMap = Maps.newHashMap();
        modulesMap.put("module", QName.create(CommandConstants.HELP_QNAME, "module"));
        final QName qName = IOUtil.qNameFromKeyString(s, modulesMap);
        Assert.assertEquals(CommandConstants.HELP_QNAME, qName);
    }
}
