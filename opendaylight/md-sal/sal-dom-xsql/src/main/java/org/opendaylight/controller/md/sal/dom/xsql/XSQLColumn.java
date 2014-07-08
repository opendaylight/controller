package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;

public class XSQLColumn implements Serializable, Comparable {
    private String name = null;
    private String tableName = null;
    private int charWidth = -1;
    private Class type = null;
    private transient Object bluePrintNode = null;

    public XSQLColumn(Object odlNode, String _tableName, Object _bluePrintNode) {
        this.name = XSQLODLUtils.getNodeNameFromDSN(odlNode);
        this.tableName = _tableName;
        this.bluePrintNode = _bluePrintNode;
        this.type = XSQLODLUtils.getTypeForODLColumn(odlNode);
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
