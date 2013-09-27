/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util.jolokia;

import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;

import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pWriteRequest;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.AttributeEntry;
import org.opendaylight.controller.config.util.ConfigTransactionClient;

public class ConfigTransactionJolokiaClient extends ListableJolokiaClient
        implements ConfigTransactionClient {

    private final ConfigRegistryJolokiaClient configRegistryJolokiaClient;

    public ConfigTransactionJolokiaClient(String url,
            ObjectName transactionControllerON,
            ConfigRegistryJolokiaClient configRegistryJolokiaClient) {
        super(url, transactionControllerON);
        this.configRegistryJolokiaClient = configRegistryJolokiaClient;
    }

    public ObjectName getTransactionON() {
        return objectName;
    }

    @Override
    public CommitStatus commit() throws ConflictingVersionException,
            ValidationException {
        return configRegistryJolokiaClient.commitConfig(objectName);
    }

    @Override
    public ObjectName createModule(String moduleName, String instanceName)
            throws InstanceAlreadyExistsException {
        J4pExecRequest execReq = new J4pExecRequest(objectName, "createModule",
                moduleName, instanceName);
        try {
            return extractObjectName(execute(execReq));
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && e.getMessage().startsWith(
                            InstanceAlreadyExistsException.class.getName()))
                throw new InstanceAlreadyExistsException();
            throw e;
        }
    }

    @Override
    public void destroyModule(ObjectName configBeanON) {
        J4pExecRequest execReq = new J4pExecRequest(objectName,
                "destroyModule(javax.management.ObjectName)", configBeanON);
        execute(execReq);
    }

    @Override
    public void destroyConfigBean(String moduleName, String instanceName)
            throws InstanceNotFoundException {
        destroyModule(ObjectNameUtil.createTransactionModuleON(
                getTransactionName(), moduleName, instanceName));
    }

    @Override
    public void abortConfig() {
        J4pExecRequest execReq = new J4pExecRequest(objectName, "abortConfig");
        execute(execReq);
    }

    @Override
    public void validateConfig() throws ValidationException {
        J4pExecRequest execReq = new J4pExecRequest(objectName,
                "validateConfig");
        execute(execReq);
    }

    @Override
    public long getParentVersion() {
        J4pReadRequest req = new J4pReadRequest(objectName, "ParentVersion");
        return (Long) execute(req).getValue();
    }

    @Override
    public long getVersion() {
        J4pReadRequest req = new J4pReadRequest(objectName, "Version");
        return (Long) execute(req).getValue();
    }

    public void setAttribute(ObjectName configBeanTransactionON, String key,
            Object value) {
        J4pWriteRequest req = new J4pWriteRequest(configBeanTransactionON, key,
                value);
        try {
            execute(req);
        } catch (RuntimeException e) {
            if (e.getMessage() != null
                    && e.getMessage().startsWith(
                            AttributeNotFoundException.class.getName())) {
                // try to fix wrong case
                Map<String, AttributeEntry> allAttributes = getAttributes(configBeanTransactionON);
                for (AttributeEntry attrib : allAttributes.values()) {
                    if (attrib.getKey().equalsIgnoreCase(key)) {
                        req = new J4pWriteRequest(configBeanTransactionON,
                                attrib.getKey(), value);
                        execute(req);
                        return;
                    }
                }
            }
            throw e;
        }
    }

    public Object getAttribute(ObjectName objectName, String key) {
        return configRegistryJolokiaClient.getAttribute(objectName, key);
    }

    public ObjectName getAttributeON(ObjectName objectName, String key) {
        return configRegistryJolokiaClient.getAttributeON(objectName, key);
    }

    @Override
    public String getTransactionName() {
        return ObjectNameUtil.getTransactionName(objectName);
    }

    @Override
    public void validateBean(ObjectName rwON) throws ValidationException {
        J4pExecRequest req = new J4pExecRequest(rwON, "validate", new Object[0]);
        execute(req);
    }

    @Override
    public void assertVersion(int expectedParentVersion,
            int expectedCurrentVersion) {
        if (expectedParentVersion != getParentVersion()) {
            throw new IllegalStateException();
        }
        if (expectedCurrentVersion != getVersion()) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void setAttribute(ObjectName on, String jmxName, Attribute attribute) {
        throw new UnsupportedOperationException();
    }

}
