/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import javax.inject.Inject;
import org.junit.runner.RunWith;
import org.opendaylight.controller.mdsal.it.base.AbstractMdsalTestBase;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.util.Filter;

@RunWith(PaxExam.class)
public abstract class AbstractIT extends AbstractMdsalTestBase {

    @Inject
    @Filter(timeout=120*1000)
    BindingAwareBroker broker;

    @Override
    public String getModuleName() {
        return "binding-broker-impl";
    }

    @Override
    public String getInstanceName() {
        return "binding-broker-impl";
    }

    @Override
    public MavenUrlReference getFeatureRepo() {
        return maven().groupId("org.opendaylight.controller").artifactId("features-mdsal").classifier("features")
                .type("xml").versionAsInProject();
    }

    @Override
    public String getFeatureName() {
        return "odl-mdsal-broker";
    }

    @Override
    protected Option[] getAdditionalOptions() {
        return new Option[]{mavenBundle("org.opendaylight.controller", "sal-test-model").versionAsInProject()};
    }
}
