/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;

public class XSQLColumn implements Serializable, Comparable<Object> {
    private static final long serialVersionUID = 4854919735031714751L;

    private String name = null;
    private String tableName = null;
    private int charWidth = -1;
    private Class<?> type = null;
    private transient Object bluePrintNode = null;
    private String origName = null;
    private String origTableName = null;

    public XSQLColumn(Object odlNode, String _tableName, Object _bluePrintNode) {
        this.name = XSQLODLUtils.getNodeNameFromDSN(odlNode);
        this.tableName = _tableName;
        this.bluePrintNode = _bluePrintNode;
        this.type = XSQLODLUtils.getTypeForODLColumn(odlNode);
    }

    public XSQLColumn(String _name, String _tableName,String _origName, String _origTableName){
        this.name = _name;
        this.tableName = _tableName;
        this.origName = _origName;
        this.origTableName = _origTableName;
    }

    public String getName() {
        return name;
    }

    public String getTableName() {
        return tableName;
    }

    public void setCharWidth(int i) {
        if (this.charWidth < i) {
            this.charWidth = i;
        }
    }

    public int getCharWidth() {
        return this.charWidth;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.tableName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof XSQLColumn)) {
            return false;
        }
        XSQLColumn other = (XSQLColumn) obj;
        return tableName.equals(other.tableName) && name.equals(other.name);
    }

    public Object getBluePrintNode() {
        return this.bluePrintNode;
    }

    @Override
    public String toString() {
        return tableName + "." + name;
    }

    @Override
    public int compareTo(Object o) {
        return this.toString().compareTo(o.toString());
    }

    public Object getResultSetValue(Object obj){
        if(this.type.equals(String.class)){
            return obj.toString();
        }else
        if(this.type.equals(int.class)){
            return Integer.parseInt(obj.toString());
        }else
        if(this.type.equals(long.class)){
            return Long.parseLong(obj.toString());
        }else
        if(this.type.equals(byte.class)){
            return Byte.parseByte(obj.toString());
        }
        return null;
    }
}
