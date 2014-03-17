/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.protocol.framework;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.netty.eventexecutor.GlobalEventExecutorModuleFactory;

final class GlobalEventExecutorUtil {

    private GlobalEventExecutorUtil() {
        throw new UnsupportedOperationException();
    }

    public static ObjectName create(final ConfigTransactionJMXClient transaction) throws InstanceAlreadyExistsException {
        try {
            return transaction.lookupConfigBean(GlobalEventExecutorModuleFactory.NAME,
                    GlobalEventExecutorModuleFactory.SINGLETON_NAME);
        } catch (InstanceNotFoundException e) {
            try {
                return transaction.createModule(GlobalEventExecutorModuleFactory.NAME,
                        GlobalEventExecutorModuleFactory.SINGLETON_NAME);
            } catch (InstanceAlreadyExistsException e1) {
                throw new IllegalStateException(e1);
            }
        }
    }

}
