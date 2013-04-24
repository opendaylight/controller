
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.switchmanager;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.NodeConnector.NodeConnectorIDType;
import org.opendaylight.controller.sal.utils.GUIField;

import org.opendaylight.controller.switchmanager.SpanConfig;

/**
 * The class represents a Span Port configuration for a network node.
 */
public class SpanConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String guiFields[] = { GUIField.NODE.toString(),
            GUIField.SPANPORTS.toString() };

    // Order matters: JSP file expects following fields in the following order
    private String nodeId;
    private String spanPort;

    public SpanConfig() {
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getSpanPort() {
        return spanPort;
    }

    public Node getNode() {
        return Node.fromString(nodeId);
    }

    private boolean hasValidNodeId() {
        return (getNode() != null);
    }

    private boolean hasValidSpanPort() {
        return (spanPort != null && !spanPort.isEmpty());
    }

    public boolean isValidConfig() {
        return (hasValidNodeId() && hasValidSpanPort());
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    public static ArrayList<String> getFieldsNames() {
        ArrayList<String> fieldList = new ArrayList<String>();
        for (Field fld : SpanConfig.class.getDeclaredFields()) {
            fieldList.add(fld.getName());
        }
        //remove the two static fields
        for (short i = 0; i < 2; i++) {
            fieldList.remove(0);
        }
        return fieldList;
    }

    public static List<String> getGuiFieldsNames() {
        List<String> fieldList = new ArrayList<String>();
        for (String str : guiFields) {
            fieldList.add(str);
        }
        return fieldList;
    }

    public ArrayList<NodeConnector> getPortArrayList() {
        Node node = Node.fromString(nodeId);
        ArrayList<NodeConnector> portList = new ArrayList<NodeConnector>();
        String[] elemArray = spanPort.split(",");
        for (String elem : elemArray) {
            if (elem.contains("-")) {
                String[] limits = elem.split("-");
                for (short j = Short.valueOf(limits[0]); j <= Short
                        .valueOf(limits[1]); j++) {
                    try {
                        portList.add(new NodeConnector(
                                NodeConnectorIDType.OPENFLOW, Short.valueOf(j),
                                node));
                    } catch (ConstructionException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                try {
                    portList.add(new NodeConnector(
                            NodeConnectorIDType.OPENFLOW, Short.valueOf(elem),
                            node));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                } catch (ConstructionException e) {
                    e.printStackTrace();
                }
            }
        }
        return portList;
    }

    public boolean matchNode(String nodeId) {
        return this.nodeId.equals(nodeId);
    }
}
