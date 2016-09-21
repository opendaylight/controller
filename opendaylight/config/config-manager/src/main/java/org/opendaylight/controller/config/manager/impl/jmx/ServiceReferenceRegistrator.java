/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.manager.impl.jmx;

import javax.management.InstanceAlreadyExistsException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;

public interface ServiceReferenceRegistrator extends AutoCloseable {

    String getNullableTransactionName();

    ServiceReferenceJMXRegistration registerMBean(ServiceReferenceMXBeanImpl object,
                                                          ObjectName on) throws InstanceAlreadyExistsException;

    @Override
    void close();

    class ServiceReferenceJMXRegistration implements AutoCloseable {
        private final InternalJMXRegistration registration;

        ServiceReferenceJMXRegistration(InternalJMXRegistration registration) {
            this.registration = registration;
        }

        @Override
        public void close() {
            registration.close();
        }
    }

    interface ServiceReferenceTransactionRegistratorFactory {
        ServiceReferenceRegistrator create();
    }

    class ServiceReferenceRegistratorImpl implements ServiceReferenceRegistrator {
        private final InternalJMXRegistrator currentJMXRegistrator;
        private final String nullableTransactionName;

        public ServiceReferenceRegistratorImpl(NestableJMXRegistrator parentRegistrator, String nullableTransactionName){
            currentJMXRegistrator = parentRegistrator.createChild();
            this.nullableTransactionName = nullableTransactionName;
        }

        public String getNullableTransactionName() {
            return nullableTransactionName;
        }


        public ServiceReferenceJMXRegistration registerMBean(ServiceReferenceMXBeanImpl object,
                                                             ObjectName on) throws InstanceAlreadyExistsException {
            String actualTransactionName = ObjectNameUtil.getTransactionName(on);
            boolean broken = false;
            broken |= (nullableTransactionName == null) != (actualTransactionName == null);
            broken |= (nullableTransactionName != null) && nullableTransactionName.equals(actualTransactionName) == false;
            if (broken) {
                throw new IllegalArgumentException("Transaction name mismatch between expected "
                        + nullableTransactionName + ", got " + actualTransactionName + " in " + on);
            }
            if (ObjectNameUtil.isServiceReference(on) == false) {
                throw new IllegalArgumentException("Invalid type of " + on);
            }
            return new ServiceReferenceJMXRegistration(currentJMXRegistrator.registerMBean(object, on));
        }


        @Override
        public void close() {
            currentJMXRegistrator.close();
        }
        public interface ServiceReferenceTransactionRegistratorFactory {
            ServiceReferenceRegistrator create();
        }
    }


    class ServiceReferenceTransactionRegistratorFactoryImpl implements ServiceReferenceTransactionRegistratorFactory {
        private final NestableJMXRegistrator parentRegistrator;
        private final String nullableTransactionName;

        public ServiceReferenceTransactionRegistratorFactoryImpl(TransactionModuleJMXRegistrator parentRegistrator,
                                                             String nullableTransactionName) {
            this.parentRegistrator = parentRegistrator;
            this.nullableTransactionName = nullableTransactionName;
        }

        public ServiceReferenceTransactionRegistratorFactoryImpl(BaseJMXRegistrator baseJMXRegistrator) {
            this.parentRegistrator = baseJMXRegistrator;
            this.nullableTransactionName = null;
        }

        public ServiceReferenceRegistrator create() {
            return new ServiceReferenceRegistratorImpl(parentRegistrator, nullableTransactionName);
        }
    }
}
