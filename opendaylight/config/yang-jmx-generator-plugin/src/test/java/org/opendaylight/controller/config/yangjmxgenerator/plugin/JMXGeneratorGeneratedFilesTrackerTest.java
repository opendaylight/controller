/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.plugin;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;

public class JMXGeneratorGeneratedFilesTrackerTest {

    @Test(expected = IllegalStateException.class)
    public void testGeneratedFilesTracker() throws Exception {
        JMXGenerator.GeneratedFilesTracker tracker = new JMXGenerator.GeneratedFilesTracker();

        tracker.addFile(new File("./a/b/c"));
        Assert.assertEquals(1, tracker.getFiles().size());
        tracker.addFile(new File("./a/b/c"));
    }
}
