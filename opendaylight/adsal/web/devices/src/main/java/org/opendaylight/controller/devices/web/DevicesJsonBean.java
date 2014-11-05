/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.devices.web;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.authorization.Privilege;

public class DevicesJsonBean {
    private List<String> columnNames;
    private List<Map<String, String>> nodeData;
    private Privilege privilege;

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<Map<String, String>> getNodeData() {
        return nodeData;
    }

    public void setNodeData(List<Map<String, String>> nodeData) {
        this.nodeData = nodeData;
    }

    public void setPrivilege(Privilege privilege) {
        this.privilege = privilege;
    }

    public Privilege getPrivilege() {
        return privilege;
    }
}
