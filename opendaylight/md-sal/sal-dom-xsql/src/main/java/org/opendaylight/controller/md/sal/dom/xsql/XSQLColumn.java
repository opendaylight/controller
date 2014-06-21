package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;

public class XSQLColumn implements Serializable,Comparable{
	private String name = null;
	private String tableName = null; 
	private transient Object bluePrintNode = null; 
	
	public XSQLColumn(String _name,String _tableName, Object _bluePrintNode){
		this.name = _name;
		this.tableName = _tableName;
		this.bluePrintNode = _bluePrintNode;
	}
			
	public String getName() {
		return name;
	}

	public String getTableName() {
		return tableName;
	}

	@Override
	public int hashCode(){
		return this.name.hashCode()+this.tableName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof XSQLColumn))
			return false;
		XSQLColumn other = (XSQLColumn)obj;
		if(tableName.equals(other.tableName) && name.equals(other.name))
			return true;
		return false;
	}

	public Object getBluePrintNode(){
		return this.bluePrintNode;
	}
	
	@Override
	public String toString() {
		return tableName+"."+name;
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}	
	
	
	
}