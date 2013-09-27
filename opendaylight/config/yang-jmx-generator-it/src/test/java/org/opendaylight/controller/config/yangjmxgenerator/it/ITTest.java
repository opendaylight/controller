/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yangjmxgenerator.it;

import static org.junit.Assert.fail;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.test.impl.DtoA;
import org.opendaylight.controller.config.yang.test.impl.DtoB;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleFactory;
import org.opendaylight.controller.config.yang.test.impl.TestImplModuleMXBean;

@Ignore
// ietf beans are not JMX compliant beans:
// Do not know how to make a
// org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev2010924.AsNumber
// from a CompositeData: no method from(CompositeData); no constructor has
// @ConstructorProperties annotation; does not have a public no-arg constructor;
// not an interface
public class ITTest extends AbstractConfigTest {

    private TestImplModuleFactory factory;
    private final String instanceName = "instance";

    @Before
    public void setUp() {

        factory = new TestImplModuleFactory();
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(
                factory));
    }

    @Test
    public void testCreateBean() throws InstanceAlreadyExistsException,
            ValidationException, ConflictingVersionException {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        createModule(transaction, instanceName);
        transaction.validateConfig();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 1, 0, 0);
    }

    @Test
    public void testReusingOldInstance() throws InstanceAlreadyExistsException,
            ConflictingVersionException, ValidationException {

        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();
        createModule(transaction, instanceName);

        transaction.commit();

        assertBeanCount(1, factory.getImplementationName());

        transaction = configRegistryClient.createTransaction();
        CommitStatus status = transaction.commit();

        assertBeanCount(1, factory.getImplementationName());
        assertStatus(status, 0, 0, 1);

    }

    @Test
    public void testInstanceAlreadyExistsException()
            throws ConflictingVersionException, ValidationException,
            InstanceAlreadyExistsException {
        ConfigTransactionJMXClient transaction = configRegistryClient
                .createTransaction();

        createModule(transaction, instanceName);
        transaction.commit();

        transaction = configRegistryClient.createTransaction();
        try {
            createModule(transaction, instanceName);
            fail();
        } catch (InstanceAlreadyExistsException e) {

        }
    }

    private ObjectName createModule(ConfigTransactionJMXClient transaction,
            String instanceName) throws InstanceAlreadyExistsException {
        ObjectName nameCreated = transaction.createModule(
                factory.getImplementationName(), instanceName);
        TestImplModuleMXBean mxBean = transaction.newMBeanProxy(nameCreated,
                TestImplModuleMXBean.class);
        mxBean.setSimpleInt((long) 45);
        // mxBean.setAsNumber(new AsNumber((long) 999));
        mxBean.setDtoA(new DtoA());
        mxBean.setDtoB(new DtoB());
        return nameCreated;

    }

}
