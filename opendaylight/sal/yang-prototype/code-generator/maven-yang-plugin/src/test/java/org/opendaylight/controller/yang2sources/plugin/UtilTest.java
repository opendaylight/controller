/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang2sources.plugin;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.Test;

public class UtilTest {

    @Test
    public void testCache() throws FileNotFoundException {
        String yang = new File(getClass().getResource("/yang/mock.yang")
                .getFile())
                .getParent();
        Collection<File> files = Util.listFiles(new File(yang));
        Collection<File> files2 = Util.listFiles(new File(yang));
        assertTrue(files == files2);
    }

}
