/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.testutils.inject;

import dagger.MembersInjector;
import javax.inject.Inject;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * JUnit rule to be able to use {@link Inject} in a *Test class.
 *
 * <p>Usage pattern:
 * <pre>
 * public class ExampleTest {
 *
 *     &#064;Singleton
 *     &#064;Component(modules = TestModule1.class)
 *     interface Configuration extends MembersInjector&lt;ExampleTest&gt; {
 *         &#064;Override void injectMembers(ExampleTest test);
 *     }
 *
 *     &#064;Rule public InjectorRule injector = new InjectorRule(DaggerExampleTest_Configuration.create());
 *
 *     &#064;Inject Service service;
 *
 *     &#064;Test public void serviceTest() {
 *         assertEquals("hello, world", service.hi());
 *     }
 * }
 * </pre>
 *
 * @author Michael Vorburger
 */
@SuppressWarnings("rawtypes")
public class InjectorRule implements MethodRule {

    private final MembersInjector injector;

    public InjectorRule(MembersInjector injector) {
        this.injector = injector;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        injector.injectMembers(target);
        return base;
    }

}
