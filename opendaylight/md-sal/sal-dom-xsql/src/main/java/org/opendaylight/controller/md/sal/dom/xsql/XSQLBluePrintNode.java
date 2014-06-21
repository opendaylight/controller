package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class XSQLBluePrintNode implements Serializable{
	
	private static final long serialVersionUID = 1L;	
	private Class<?> myInterface = null;
	private String myInterfaceString = null;
	private Set<XSQLBluePrintRelation> relations = new HashSet<XSQLBluePrintRelation>();
	private Set<XSQLBluePrintNode> inheritingNodes = new HashSet<XSQLBluePrintNode>();
	private Set<XSQLBluePrintNode> children = new HashSet<XSQLBluePrintNode>();
	private XSQLBluePrintNode parent = null;
	
	private int level = -1; 
	private transient Set<String> parentHierarchySet = null;
	private String myInterfaceName = null;
	private Set<XSQLColumn> columns = new HashSet<XSQLColumn>();
	
	private transient Object odlNode = null; 
	private boolean module = false;
	private String tableName = null;
	private String odlNodeName = null;
	private String cacheKey = null;
	
	public XSQLBluePrintNode(Class<?> _myInterface,int _level){
		this.myInterface = _myInterface;
		this.myInterfaceString = _myInterface.getName();
		this.myInterfaceName = myInterface.getSimpleName();		
		this.level = _level;
	}
	
	public XSQLBluePrintNode(Object _odlNode,int _level){
		this.odlNode = _odlNode;
		this.level = _level;
		this.module = XSQLODLUtils.isModule(_odlNode);
	}	
	
	public boolean isModule(){
		return this.module;
	}
	
	public Set<XSQLBluePrintNode> getChildren(){
		return this.children;
	}
			
	public String getODLNodeName(){
		if(this.odlNodeName==null){
			this.odlNodeName = XSQLODLUtils.getODLNodeName(this.odlNode);
		}
		return this.odlNodeName;
	}
	
	public String getTableName(){
		if(this.tableName==null){
			this.tableName = XSQLODLUtils.getTableName(this.odlNode);
		}
		return this.tableName;
	}
			
	public String getCacheKey(){
		if(this.cacheKey==null){
			this.cacheKey = XSQLODLUtils.getCacheKey(this.odlNode); 
		}
		return this.cacheKey;
	}
	
	public Object getODLNode(){
		return this.odlNode;
	}
	
	public void setParent(XSQLBluePrintNode p){
		this.parent = p;
	}	
	
	public void AddChild(XSQLBluePrintNode ch){
		this.children.add(ch);
	}
		
	public String getName(){
		if(this.myInterface!=null)
			return this.myInterfaceName;
		else
		if(this.odlNode!=null)
			return getTableName();
		else
			return "Unknown";
	}
		
	public boolean isModelChild(Class p){
		if(this.relations.size()==0)
			return false;
		for(XSQLBluePrintRelation parentRelation:this.relations){
			if(parentRelation.getParent().getInterface().equals(p))
				return true;
		}
		for(XSQLBluePrintRelation dtr:this.relations){
			XSQLBluePrintNode parent = dtr.getParent();
			if(!parent.getInterface().equals(this.getInterface()) && !parent.getInterface().isAssignableFrom(this.getInterface()) && 
					this.getInterface().isAssignableFrom(parent.getInterface()) && parent.isModelChild(p))
				return true;
		}		
		return false;		
	}
		
	public Set<XSQLBluePrintRelation> getRelations(){
		return this.relations;
	}
		
	public String getClassName(){
		return this.myInterfaceString;
	}
		
	public void addInheritingNode(XSQLBluePrintNode node){
		this.inheritingNodes.add(node);
	}
	
	public Set<XSQLBluePrintNode> getInheritingNodes(){
		return this.inheritingNodes;
	}
	
	public void addColumn(Object node,String tableName){	
		XSQLColumn c = new XSQLColumn(XSQLODLUtils.getNodeNameFromDSN(node),getTableName(),this);
		this.columns.add(c);
	}
	
	public void addColumn(String methodName){
		if(methodName.startsWith("get"))
			methodName = methodName.substring(3);
		else
		if(methodName.startsWith("is"))
			methodName = methodName.substring(2);		
		XSQLColumn c = new XSQLColumn(methodName, myInterfaceName,null);
		this.columns.add(c);
	}	
	
	public Collection<XSQLColumn> getColumns(){
		return this.columns;
	}	
	
	public XSQLColumn findColumnByName(String name) throws SQLException {
		
		XSQLColumn exactMatch = null;
		XSQLColumn indexOfMatch = null;
		XSQLColumn exactLowercaseMatch = null;
		XSQLColumn indexOfLowerCaseMatch = null;
		
		for(XSQLColumn col:columns){
			if(col.getName().equals(name))
				exactMatch = col;
			if(col.getName().indexOf(name)!=-1)
				indexOfMatch = col;
			if(col.getName().toLowerCase().equals(name.toLowerCase()))
				exactLowercaseMatch = col;
			if(col.getName().toLowerCase().indexOf(name.toLowerCase())!=-1)
				indexOfLowerCaseMatch = col;			
		}
		
		if(exactMatch!=null)
			return exactMatch;
		if(exactLowercaseMatch!=null)
			return exactLowercaseMatch;
		if(indexOfMatch!=null)
			return indexOfMatch;
		if(indexOfLowerCaseMatch!=null)
			return indexOfLowerCaseMatch;
		
		throw new SQLException("Unknown field name '"+name+"'");
	}
	
	
	public void addParent(XSQLBluePrintNode parent,String property){
		try{
			if(property.equals("ContainingTPs"))
				return;	
			//Method m = parent.getInterface().getMethod("get"+property, null);
			//if(!m.getDeclaringClass().equals(parent.getInterface()))
				//return;
			XSQLBluePrintRelation rel = new XSQLBluePrintRelation(parent, property,myInterface);
			relations.add(rel);
		}catch(Exception err){
			err.printStackTrace();
		}
	}
	
	public XSQLBluePrintNode getParent(){
		return this.parent;
	}
	
	public Set<XSQLBluePrintRelation> getClonedParents(){
		Set<XSQLBluePrintRelation> result = new HashSet<XSQLBluePrintRelation>();
		result.addAll(this.relations);
		return result;
	}
	
	public Set<String> buildParentsSet(Set<Class> visited,XSQLBluePrint cache){
				
		if(visited.equals(myInterface)){
			return null;
		}
		
		visited.add(myInterface);
		
		if(parentHierarchySet!=null){
			return parentHierarchySet;
		}
		
		parentHierarchySet = new HashSet<String>();
		
		for(XSQLBluePrintRelation vr:relations){
			XSQLBluePrintNode p = cache.getNQLBluePrintNode(vr.getParent().getInterface().getSimpleName());
			Set<String> parentResult = p.buildParentsSet(visited, cache);
			if(parentResult!=null){
				parentHierarchySet.addAll(parentResult);
			}
			String ps = vr.getParent().getInterface().getSimpleName();
			parentHierarchySet.add(ps);
			
		}
		return parentHierarchySet;
	}
	
	public String toString(){
		if(myInterfaceName!=null)
			return myInterfaceName;
		if(odlNode!=null){
			return getTableName();
		}
		return "Unknown";
	}
	
	public Class getInterface(){
		return this.myInterface;
	}
	
	/*
	public void appendToRetrievalSpecification(RetrievalSpecification rs,Set loopStop,int pathLogicalPhysical,Class originalInterface,boolean isRoot, Map cache){
		if(loopStop.contains(myInterface.getSimpleName())){
			return;
		}
		loopStop.add(myInterface.getSimpleName());
		TypeDef reqProperties = rs.getRequiredProperties();
		for(NQLRelation rel:relations){
			reqProperties.addProperty(rel.getNEClosestClass().getName(),rel.getProperty());
			rel.getParent().appendToRetrievalSpecification(rs, loopStop, pathLogicalPhysical, originalInterface, isRoot,cache);
		}
		/*
		if(myInterface.getInterfaces()!=null && myInterface.getInterfaces().length>0){
			NQLBluePrintNode javaParentNode = (NQLBluePrintNode)cache.get(myInterface.getInterfaces()[0].getSimpleName());
			if(javaParentNode!=null){
				for(NQLRelation rel:javaParentNode.relations){
					reqProperties.addProperty(rel.getParent().getInterface().getName(),rel.getProperty());
				}
			}
		}*/
		/*
		for(NQLBluePrintNode in:this.inheritingNodes){
			if(in!=this){
				in.appendToRetrievalSpecification(rs, loopStop, pathLogicalPhysical, originalInterface, isRoot, cache);
			}
		}*/
		
		/*
		for(NQLRelation rel:relations){
			if(isRoot && !rel.getProperty().equals("ContainedCurrentCTPs"))
				continue;
			if(IAspect.class.isAssignableFrom(myInterface)){	
				try{
					String parentOidType = "com.sheer.imo.keys."+rel.getParent().getInterface().getSimpleName()+"Oid";
					String myOidType = "com.sheer.imo.keys."+myInterface.getSimpleName()+"Oid";
					if(ITicketListAspect.class.isAssignableFrom(myInterface)){
						rs.getRequiredAspect().addProperty(INEOid.class.getName(),ITicketListAspectOid.class.getName());
						reqProperties.addProperty(INE.class.getName(),ITicketListAspectOid.class.getName());						
					}else{
						rs.getRequiredAspect().addProperty(Class.forName(parentOidType).getName(),Class.forName(myOidType).getName());
						reqProperties.addProperty(rel.getParent().getInterface().getName(),Class.forName(myOidType).getName());
					}
				}catch(Exception err){
					err.printStackTrace();
				}
			}else{
				if(pathLogicalPhysical==-1 || 
						(pathLogicalPhysical==1 && !rel.getParent().getInterface().equals(IPhysicalRoot.class)) ||
						(pathLogicalPhysical==2 && !rel.getParent().getInterface().equals(ILogicalRoot.class))){
					if(!beenHere.contains(rel.getParent().getInterface())){
						beenHere.add(rel.getParent().getInterface());
						if(originalInterface.equals(rel.getParent().getInterface()) || !originalInterface.isAssignableFrom(rel.getParent().getInterface())){
							reqProperties.addProperty(rel.getParent().getInterface().getName(),rel.getProperty());
							if(rel.getParent()!=null){
								String uniqueKey = rel.getParent().toString()+rel.getProperty();
								if(!loopStop.contains(uniqueKey)){
									loopStop.add(uniqueKey);
									if(pathLogicalPhysical==-1 || 
											(pathLogicalPhysical==1 && !rel.getParent().getInterface().equals(IPhysicalRoot.class)) ||
											(pathLogicalPhysical==2 && !rel.getParent().getInterface().equals(ILogicalRoot.class))){
									
										rel.getParent().appendToRetrievalSpecification(rs,loopStop,rel.getParent().getInterface(),pathLogicalPhysical,originalInterface,false);
									}
								}
							}
							
						}
					}
				}
			}
		}*/
	//}

	public int getLevel(){
		return this.level;
	}

	@Override
	public boolean equals(Object obj) {
		XSQLBluePrintNode other = (XSQLBluePrintNode)obj;
		if(odlNode!=null){
			return getCacheKey().equals(other.getCacheKey());
		}else
		if(this.cacheKey!=null){
			return this.cacheKey.equals(other.cacheKey);
		}else
			return other.myInterface.equals(myInterface);
	}

	@Override
	public int hashCode() {
		if(myInterfaceString!=null)
			return myInterfaceString.hashCode();
		else
		if(odlNode!=null){
			return getCacheKey().hashCode();
		}
		return 0;
	}
		
}