package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XSQLBluePrintNode implements Serializable {

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
    private Map<String, XSQLColumn> origNameToColumn = new HashMap<String, XSQLColumn>();

    private transient List<Object> odlNodes = new ArrayList<>();
    private boolean module = false;
    private String bluePrintTableName = null;
    private String odlTableName = null;
    private String origName = null;

    public void mergeAugmentation(XSQLBluePrintNode aug){
        this.relations.addAll(aug.relations);
        this.inheritingNodes.addAll(aug.inheritingNodes);
        this.children.addAll(aug.children);
        this.columns.addAll(aug.columns);
        this.origNameToColumn.putAll(aug.origNameToColumn);
        this.odlNodes.addAll(aug.odlNodes);
    }

    public XSQLBluePrintNode(String name, String _origName, int _level) {
        this.level = _level;
        this.odlTableName = name;
        this.bluePrintTableName = name;
        this.origName = _origName;
    }

    public XSQLBluePrintNode(Class<?> _myInterface, int _level) {
        this.myInterface = _myInterface;
        this.myInterfaceString = _myInterface.getName();
        this.myInterfaceName = myInterface.getSimpleName();
        this.level = _level;
    }

    public XSQLBluePrintNode(Object _odlNode, int _level,XSQLBluePrintNode _parent) {
        this.odlNodes.add(_odlNode);
        this.level = _level;
        this.module = XSQLODLUtils.isModule(_odlNode);
        this.parent = _parent;
        this.bluePrintTableName = XSQLODLUtils.getBluePrintName(_odlNode);
        this.odlTableName = XSQLODLUtils.getODLNodeName(this.odlNodes.get(0));
    }

    public String getOrigName() {
        return this.origName;
    }

    public String getBluePrintNodeName() {
        return this.bluePrintTableName;
    }

    public boolean isModule() {
        return this.module;
    }

    public Set<XSQLBluePrintNode> getChildren() {
        return this.children;
    }

    public String getODLTableName() {
        if (this.odlTableName == null) {
            this.odlTableName = XSQLODLUtils.getODLNodeName(this.odlNodes.get(0));
        }
        return this.odlTableName;
    }

    public List<Object> getODLNodes() {
        return this.odlNodes;
    }

    public void AddChild(XSQLBluePrintNode ch) {
        this.children.add(ch);
    }

    public boolean isModelChild(Class<?> p) {
        if (this.relations.size() == 0) {
            return false;
        }
        for (XSQLBluePrintRelation parentRelation : this.relations) {
            if (parentRelation.getParent().getInterface().equals(p)) {
                return true;
            }
        }
        for (XSQLBluePrintRelation dtr : this.relations) {
            XSQLBluePrintNode parent = dtr.getParent();
            if (!parent.getInterface().equals(this.getInterface())
                    && !parent.getInterface().isAssignableFrom(
                            this.getInterface())
                    && this.getInterface().isAssignableFrom(
                            parent.getInterface()) && parent.isModelChild(p)) {
                return true;
            }
        }
        return false;
    }

    public Set<XSQLBluePrintRelation> getRelations() {
        return this.relations;
    }

    public String getClassName() {
        return this.myInterfaceString;
    }

    public void addInheritingNode(XSQLBluePrintNode node) {
        this.inheritingNodes.add(node);
    }

    public Set<XSQLBluePrintNode> getInheritingNodes() {
        return this.inheritingNodes;
    }

    public void addColumn(Object node, String tableName) {
        XSQLColumn c = new XSQLColumn(node, getBluePrintNodeName(), this);
        this.columns.add(c);
    }

    public XSQLColumn addColumn(String name, String tableName, String origName,
            String origTableName) {
        XSQLColumn c = new XSQLColumn(name, tableName, origName, origTableName);
        this.columns.add(c);
        this.origNameToColumn.put(origName, c);
        return c;
    }

    public void addColumn(String methodName) {
        if (methodName.startsWith("get")) {
            methodName = methodName.substring(3);
        } else if (methodName.startsWith("is")) {
            methodName = methodName.substring(2);
        }
        XSQLColumn c = new XSQLColumn(methodName, myInterfaceName, null);
        this.columns.add(c);
    }

    public Collection<XSQLColumn> getColumns() {
        return this.columns;
    }

    public XSQLColumn findColumnByName(String name) throws SQLException {

        XSQLColumn exactMatch = null;
        XSQLColumn indexOfMatch = null;
        XSQLColumn exactLowercaseMatch = null;
        XSQLColumn indexOfLowerCaseMatch = null;

        for (XSQLColumn col : columns) {
            if (col.getName().equals(name)) {
                exactMatch = col;
            }
            if (col.getName().indexOf(name) != -1) {
                indexOfMatch = col;
            }
            if (col.getName().toLowerCase().equals(name.toLowerCase())) {
                exactLowercaseMatch = col;
            }
            if (col.getName().toLowerCase().indexOf(name.toLowerCase()) != -1) {
                indexOfLowerCaseMatch = col;
            }
        }

        if (exactMatch != null) {
            return exactMatch;
        }
        if (exactLowercaseMatch != null) {
            return exactLowercaseMatch;
        }
        if (indexOfMatch != null) {
            return indexOfMatch;
        }
        if (indexOfLowerCaseMatch != null) {
            return indexOfLowerCaseMatch;
        }

        throw new SQLException("Unknown field name '" + name + "'");
    }

    public void addParent(XSQLBluePrintNode parent, String property) {
        try {
            if (property.equals("ContainingTPs")) {
                return;
            }
            // Method m = parent.getInterface().getMethod("get"+property, null);
            // if(!m.getDeclaringClass().equals(parent.getInterface()))
            // return;
            XSQLBluePrintRelation rel = new XSQLBluePrintRelation(parent,
                    property, myInterface);
            relations.add(rel);
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public XSQLBluePrintNode getParent() {
        return this.parent;
    }

    public Set<XSQLBluePrintRelation> getClonedParents() {
        Set<XSQLBluePrintRelation> result = new HashSet<XSQLBluePrintRelation>();
        result.addAll(this.relations);
        return result;
    }

    public String toString() {
        if (myInterfaceName != null) {
            return myInterfaceName;
        }
        if (odlNodes.size()>0) {
            return getBluePrintNodeName();
        }
        if (odlTableName != null) {
            return odlTableName;
        }
        return "Unknown";
    }

    public Class<?> getInterface() {
        return this.myInterface;
    }

    public int getLevel() {
        return this.level;
    }

    @Override
    public boolean equals(Object obj) {
        XSQLBluePrintNode other = (XSQLBluePrintNode) obj;
        if (odlNodes!=null && odlNodes.size()>0) {
            return getBluePrintNodeName().equals(other.getBluePrintNodeName());
        } else if (this.odlTableName == null && other.odlTableName != null) {
            return false;
        }
        if (this.odlTableName != null && other.odlTableName == null) {
            return false;
        }
        else {
            return this.odlTableName.equals(other.odlTableName);
        }
    }

    @Override
    public int hashCode() {
        if (myInterfaceString != null) {
            return myInterfaceString.hashCode();
        } else if (odlNodes!=null && odlNodes.size()>0) {
            return bluePrintTableName.hashCode();
        }
        return 0;
    }

}
