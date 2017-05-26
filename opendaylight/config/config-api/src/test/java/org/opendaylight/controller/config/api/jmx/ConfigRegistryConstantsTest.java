/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api.jmx;


import org.junit.Test;
import org.opendaylight.controller.config.api.jmx.constants.ConfigRegistryConstants;

public class ConfigRegistryConstantsTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateON() throws Exception {
        ConfigRegistryConstants.createON("test.<:", "asd", "asd");
    }
}
