/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.api;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;

public class IdentityAttributeRefTest {

    IdentityAttributeRef attr = new IdentityAttributeRef("attr");

    @Test
    public void testConstructor() throws Exception {
        String param = new String("attr");
        Assert.assertEquals(attr.getqNameOfIdentity(), param);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor2() throws Exception {
        IdentityAttributeRef attr = new IdentityAttributeRef(null);
    }

    @Test
    public void testHashCode() throws Exception {
        Assert.assertEquals(attr.hashCode(), new String("attr").hashCode());
    }

    @Test
    public void testEqual() throws Exception {
        Assert.assertEquals(attr, attr);
    }

    @Test
    public void testEqual2() throws Exception {
        Assert.assertEquals(attr, new IdentityAttributeRef("attr"));
    }

    @Test
    public void testNotEqual() throws Exception {
        Assert.assertNotEquals(attr, new IdentityAttributeRef("different"));
    }

    @Test
    public void testResolveIdentity() throws Exception {
        DependencyResolver res = mock(DependencyResolver.class);
        IdentityAttributeRef a = new IdentityAttributeRef("abcd");
        doReturn(SubIdentity.class).when(res).resolveIdentity(a, Identity.class);
        a.resolveIdentity(res, Identity.class);
        verify(res).resolveIdentity(a, Identity.class);
    }

    @Test
    public void testValidateIdentity() throws Exception {
        DependencyResolver res = mock(DependencyResolver.class);
        JmxAttribute jmxAttr = new JmxAttribute("abc");
        doNothing().when(res).validateIdentity(attr, Identity.class, jmxAttr);
        attr.validateIdentity(res, Identity.class, jmxAttr);
        verify(res).validateIdentity(attr, Identity.class, jmxAttr);
    }

    static class Identity extends BaseIdentity {}

    static class SubIdentity extends Identity {}
}
