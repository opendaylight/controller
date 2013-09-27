/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.util.jolokia;

import java.util.List;
import java.util.Set;

import javax.management.ObjectName;

import org.jolokia.client.request.J4pExecRequest;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opendaylight.controller.config.api.ConflictingVersionException;
import org.opendaylight.controller.config.api.ValidationException;
import org.opendaylight.controller.config.api.jmx.CommitStatus;
import org.opendaylight.controller.config.api.jmx.ConfigRegistryMXBean;
import org.opendaylight.controller.config.api.jmx.ObjectNameUtil;
import org.opendaylight.controller.config.util.ConfigRegistryClient;

public class ConfigRegistryJolokiaClient extends ListableJolokiaClient
        implements ConfigRegistryClient {

    public ConfigRegistryJolokiaClient(String url) {
        super(url, ConfigRegistryMXBean.OBJECT_NAME);
    }

    @Override
    public ConfigTransactionJolokiaClient createTransaction() {
        // create transaction
        J4pExecRequest execReq = new J4pExecRequest(objectName, "beginConfig");
        J4pResponse<J4pExecRequest> resp = execute(execReq);
        ObjectName transactionControllerON = extractObjectName(resp);
        return getConfigTransactionClient(transactionControllerON);
    }

    @Override
    public ConfigTransactionJolokiaClient getConfigTransactionClient(
            String transactionName) {
        ObjectName objectName = ObjectNameUtil
                .createTransactionControllerON(transactionName);
        return getConfigTransactionClient(objectName);
    }

    @Override
    public ConfigTransactionJolokiaClient getConfigTransactionClient(
            ObjectName objectName) {
        return new ConfigTransactionJolokiaClient(url, objectName, this);
    }

    @Override
    public CommitStatus commitConfig(ObjectName transactionControllerON)
            throws ConflictingVersionException, ValidationException {
        J4pExecRequest execReq = new J4pExecRequest(objectName, "commitConfig",
                transactionControllerON);
        JSONObject jsonObject;
        jsonObject = execute(execReq).getValue();
        JSONArray newInstancesArray = (JSONArray) jsonObject
                .get("newInstances");
        List<ObjectName> newInstances = jsonArrayToObjectNames(newInstancesArray);
        JSONArray reusedInstancesArray = (JSONArray) jsonObject
                .get("reusedInstances");
        List<ObjectName> reusedInstances = jsonArrayToObjectNames(reusedInstancesArray);
        JSONArray recreatedInstancesArray = (JSONArray) jsonObject
                .get("recreatedInstances");
        List<ObjectName> recreatedInstances = jsonArrayToObjectNames(recreatedInstancesArray);
        return new CommitStatus(newInstances, reusedInstances,
                recreatedInstances);
    }

    public Object getAttribute(ObjectName configBeanTransactionON, String key) {
        J4pReadRequest req = new J4pReadRequest(configBeanTransactionON, key);
        return execute(req).getValue();
    }

    public ObjectName getAttributeON(ObjectName configBeanTransactionON,
            String key) {
        JSONObject jsonAttrib = (JSONObject) getAttribute(
                configBeanTransactionON, key);
        return extractObjectName(jsonAttrib);
    }

    // proxy around ConfigTransactionManagerMXBean

    @Override
    public ObjectName beginConfig() {
        ConfigTransactionJolokiaClient result = createTransaction();
        return result.getTransactionON();
    }

    @Override
    public List<ObjectName> getOpenConfigs() {
        J4pReadRequest req = new J4pReadRequest(objectName, "OpenConfigs");
        JSONArray jsonArray = execute(req).getValue();
        return jsonArrayToObjectNames(jsonArray);
    }

    @Override
    public long getVersion() {
        J4pReadRequest req = new J4pReadRequest(objectName, "Version");
        return (Long) execute(req).getValue();
    }

    @Override
    public boolean isHealthy() {
        J4pReadRequest req = new J4pReadRequest(objectName, "Healthy");
        return (Boolean) execute(req).getValue();
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans() {
        return lookupSomething("lookupRuntimeBeans()", new Object[0]);
    }

    @Override
    public Set<ObjectName> lookupRuntimeBeans(String moduleName,
            String instanceName) {
        return lookupSomething(
                "lookupRuntimeBeans(java.lang.String,java.lang.String)",
                new Object[] { moduleName, instanceName });
    }

    @Override
    public Object invokeMethod(ObjectName on, String name, Object[] params,
            String[] signature) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getAttributeCurrentValue(ObjectName on, String attributeName) {
        throw new UnsupportedOperationException();
    }
}
