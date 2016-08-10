/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils.example;

import static org.junit.Assert.assertEquals;

import dagger.Component;
import dagger.MembersInjector;
import javax.inject.Inject;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.controller.config.testutils.inject.InjectorRule;

public class ExampleTest {

    @Component(modules = TestModule1.class)
    interface Configuration extends MembersInjector<ExampleTest> {
        @Override
        void injectMembers(ExampleTest test);
    }

    @Rule
    public InjectorRule injector = new InjectorRule(DaggerExampleTest_Configuration.create());

    @Inject
    Service service;

    @Test
    public void serviceTest() {
        assertEquals("hello, world", service.hi());
    }

}
