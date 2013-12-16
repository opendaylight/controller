/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.directory.autodetect;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;

import java.io.File;

public class FileTypeTest {

    @Test
    public void testPlaintext() throws Exception {
        File file = getResourceAsFile("/test.txt.config");

        FileType type = FileType.getFileType(file);
        Assert.assertEquals(FileType.plaintext, type);

    }

    @Test
    public void testXml() throws Exception {
        File file = getResourceAsFile("/test.xml.config");

        FileType type = FileType.getFileType(file);
        Assert.assertEquals(FileType.xml, type);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnknown() throws Exception {
        File file = getResourceAsFile("/unknown.config");

        try {
            FileType.getFileType(file);
        } catch (IllegalArgumentException e) {
            org.junit.Assert.assertThat(e.getMessage(), JUnitMatchers.containsString("File " + file + " is not of permitted storage type"));
            throw e;
        }
    }

    static File getResourceAsFile(String resource) {
        String f = FileTypeTest.class.getResource(resource).getFile();
        return new File(f);
    }

}
