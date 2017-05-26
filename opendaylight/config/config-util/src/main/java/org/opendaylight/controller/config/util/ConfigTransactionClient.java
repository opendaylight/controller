/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigTransactionControllerMXBean;

public interface ConfigTransactionClient extends
        ConfigTransactionControllerMXBean, BeanReader {

    CommitStatus commit() throws ConflictingVersionException,
            ValidationException;

    void assertVersion(int expectedParentVersion, int expectedCurrentVersion);

    long getParentVersion();

    long getVersion();

    ObjectName getObjectName();

    void validateBean(ObjectName configBeanON) throws ValidationException;

    @Deprecated
    /**
     * Use {@link #destroyModule(String, String)}
     */
    void destroyConfigBean(String moduleName, String instanceName) throws InstanceNotFoundException;

    void destroyModule(String moduleName, String instanceName) throws InstanceNotFoundException;

    void setAttribute(ObjectName on, String jmxName, Attribute attribute);

    /*
     * Get the attribute named jmxName from the Object with ObjectName on
     *
     * @param on - ObjectName of the Object from which the attribute should be read
     * @param jmxName - name of the attribute to be read
     *
     * @return Object of Object on with attribute name jmxName
     */
    Attribute getAttribute(ObjectName on, String jmxName);
}
