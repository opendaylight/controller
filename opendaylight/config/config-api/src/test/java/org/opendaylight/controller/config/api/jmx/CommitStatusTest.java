/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api.jmx;

import java.util.ArrayList;
import java.util.List;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CommitStatusTest {
    List<ObjectName> newInst = new ArrayList<>();
    List<ObjectName> reusedInst = new ArrayList<>();
    List<ObjectName> recreatedInst = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        newInst.add(new ObjectName("domain: key1 = value1 , key2 = value2"));
        reusedInst.add(new ObjectName("o2: key = val"));
        recreatedInst.add(new ObjectName("o3: key = k"));
    }
    @Test
    public void testCommitStatus() throws Exception {
        CommitStatus status = new CommitStatus(newInst, reusedInst, recreatedInst);
        Assert.assertEquals(status.getNewInstances(), newInst);
        Assert.assertEquals(status.getRecreatedInstances(), recreatedInst);
        Assert.assertEquals(status.getReusedInstances(), reusedInst);
    }

    @Test
    public void testEqual() throws Exception {
        CommitStatus status = new CommitStatus(newInst, reusedInst, recreatedInst);
        Assert.assertEquals(status, new CommitStatus(newInst, reusedInst, recreatedInst));
        Assert.assertEquals(status.toString(), new CommitStatus(newInst, reusedInst, recreatedInst).toString());
        Assert.assertEquals(status, status);
    }

    @Test
    public void testHashCode() throws Exception {
        CommitStatus status = new CommitStatus(newInst, reusedInst, recreatedInst);
        Assert.assertEquals(status.hashCode(), new CommitStatus(newInst, reusedInst, recreatedInst).hashCode());
    }

    @Test
    public void testNotEqual() throws Exception {
        List<ObjectName> newInst2 = new ArrayList<>();
        List<ObjectName> reusedInst2 = new ArrayList<>();
        List<ObjectName> recreatedInst2 = new ArrayList<>();

        newInst2.add(new ObjectName("first: key1 = value1"));
        reusedInst2.add(new ObjectName("second: key = val"));
        recreatedInst2.add(new ObjectName("third: key = k"));

        CommitStatus status = new CommitStatus(newInst, reusedInst, recreatedInst);
        Assert.assertNotEquals(status, null);
        Assert.assertNotEquals(status, new Object());
        Assert.assertNotEquals(status, new CommitStatus(newInst2, reusedInst, recreatedInst));
        Assert.assertNotEquals(status, new CommitStatus(newInst, reusedInst2, recreatedInst));
        Assert.assertNotEquals(status, new CommitStatus(newInst, reusedInst, recreatedInst2));

        CommitStatus status2 = new CommitStatus(newInst, reusedInst, recreatedInst);
    }
}
