/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.util;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import javax.management.MXBean;

import org.junit.Test;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.yangtools.concepts.Identifiable;

import com.google.common.collect.Sets;

public class InterfacesHelperTest {

    interface SuperA {

    }

    interface SuperBMXBean {

    }

    interface SuperC extends SuperA, SuperBMXBean {

    }

    class SuperClass implements SuperC {

    }

    @MXBean
    interface SubA {

    }

    abstract class SubClass extends SuperClass implements SubA, Module {

    }

    @Test
    public void testGetAllInterfaces() {
        Set<Class<?>> expected = Sets.<Class<?>> newHashSet(SuperA.class, SuperBMXBean.class, SuperC.class,
                SubA.class, Identifiable.class, Module.class);
        assertEquals(expected,
                InterfacesHelper.getAllInterfaces(SubClass.class));
    }

    @Test
    public void testGetMXInterfaces() {
        Set<Class<?>> expected = Sets.<Class<?>> newHashSet(SuperBMXBean.class, SubA.class);
        assertEquals(expected, InterfacesHelper.getMXInterfaces(SubClass.class));
    }

}
