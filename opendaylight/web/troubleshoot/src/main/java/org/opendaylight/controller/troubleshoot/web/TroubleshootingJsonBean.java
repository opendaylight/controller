
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.troubleshoot.web;

import java.util.HashMap;
import java.util.List;

public class TroubleshootingJsonBean {
    private List<String> columnNames;
    private List<HashMap<String, String>> nodeData;

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<HashMap<String, String>> getNodeData() {
        return nodeData;
    }

    public void setNodeData(List<HashMap<String, String>> nodeData) {
        this.nodeData = nodeData;
    }
}
