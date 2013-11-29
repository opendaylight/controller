/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.util.messages;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;

public class NetconfMessageFactoryTest {


    @Test
    public void testAuth() throws Exception {
        NetconfMessageFactory parser = new NetconfMessageFactory();
        File authHelloFile = new File(getClass().getResource("/netconfMessages/client_hello_with_auth.xml").getFile());
        parser.parse(Files.toByteArray(authHelloFile));

    }
}
