/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/* Blue print node represents a schema node in the scema context of ODL
 * It maps the relation to the parent schema node and to the children as
 * well as the normalize name of the schema context
 */
public class XSQLBluePrintNode implements Serializable {

    private static final long serialVersionUID = 1L;
    //Known augmentation for this node
    private Set<XSQLBluePrintNode> augmetations = new HashSet<XSQLBluePrintNode>();
    //the children in the model
    private Set<XSQLBluePrintNode> children = new HashSet<XSQLBluePrintNode>();
    //The parent in the model
    private XSQLBluePrintNode parent = null;
    //Depth level of this node in the tree model
    private int level = -1;
    //The columns/attribute as identified in the scema context for this node
    private Set<XSQLColumn> columns = new HashSet<XSQLColumn>();
    //When there are augmentation, we need to keep the original method name to
    //extract it from the augmentation
    private Map<String, XSQLColumn> origNameToColumn = new HashMap<String, XSQLColumn>();
    //The scema nodes that are mapped to this table
    //There can be more than one if there are augmentations or two version of the same
    //model loaded
    private transient Object[] odlSchemaNodes = null;
    //If this is a root module in the schema context that this
    //will indicate that
    private boolean module = false;
    //The name of the table for sql queries
    private String bluePrintTableName = null;
    //The different names in ODL for this table
    private String odlTableNames[] = null;
    //In sql you can create logical names when you do somehting like "as <some name>", this is to support
    //that
    private String origName = null;

    //Merge two blue print nodes that were identified to be either one augmentation of the other
    //or the same model but different versions
    public void mergeAugmentation(XSQLBluePrintNode aug) {
        this.augmetations.addAll(aug.augmetations);
        this.children.addAll(aug.children);
        this.columns.addAll(aug.columns);
        this.origNameToColumn.putAll(aug.origNameToColumn);
        if (aug.odlSchemaNodes != null) {
            for (Object sn : aug.odlSchemaNodes) {
                addToSchemaNodes(sn);
            }
        }
    }

    public XSQLBluePrintNode(String name, String _origName, int _level) {
        this.level = _level;
        this.odlTableNames = new String[]{name};
        this.bluePrintTableName = name;
        this.origName = _origName;
    }

    public XSQLBluePrintNode(Object _odlNode, int _level,XSQLBluePrintNode _parent) {
        addToSchemaNodes(_odlNode);
        this.level = _level;
        this.module = XSQLODLUtils.isModule(_odlNode);
        this.parent = _parent;
        this.bluePrintTableName = XSQLODLUtils.getBluePrintName(_odlNode);
    }

    //Add the schema node to the list of nodes
    //make sure the same node isn't added twice, only augmentations and
    //different model versions
    private void addToSchemaNodes(Object schemaObject) {
        if (this.odlSchemaNodes == null){
            this.odlSchemaNodes = new Object[1];
            this.odlTableNames = new String[1];
        }else {
            //Check if already have this model schema node.
            //Same model schema node can come from different modules
            //We need to keep those that are same model but different version
            //as some modules can use version x of the model
            //and some can use version y of the same model.
            for(Object so:this.odlSchemaNodes){
                if(so.toString().equals(schemaObject.toString()))
                    return;
            }
            Object[] temp = new Object[this.odlSchemaNodes.length + 1];
            System.arraycopy(this.odlSchemaNodes, 0, temp, 0,this.odlSchemaNodes.length);
            this.odlSchemaNodes = temp;
            String[] tempS = new String[this.odlTableNames.length + 1];
            System.arraycopy(this.odlTableNames,0, tempS, 0, this.odlTableNames.length);
            this.odlTableNames = tempS;
        }
        this.odlTableNames[this.odlTableNames.length - 1] = XSQLODLUtils.getODLNodeName(schemaObject);
        this.odlSchemaNodes[this.odlSchemaNodes.length - 1] = schemaObject;
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

    public Object[] getODLSchemaNodes(){
        return this.odlSchemaNodes;
    }

    public String[] getODLTableNames() {
        if (this.odlTableNames == null) {
            for(int i=0;i<this.odlSchemaNodes.length;i++){
                if(this.odlSchemaNodes[i]!=null){
                    this.odlTableNames[i] = XSQLODLUtils.getODLNodeName(this.odlSchemaNodes[i]);
                }
            }
        }
        return this.odlTableNames;
    }

    public boolean containTableName(String name){
        if(this.odlTableNames!=null){
            for(String tName:this.odlTableNames){
                if(tName.equals(name))
                    return true;
            }
        }
        return false;
    }

    public void addChild(XSQLBluePrintNode ch) {
        this.children.add(ch);
    }

    public void addInheritingNode(XSQLBluePrintNode node) {
        this.augmetations.add(node);
    }

    public Set<XSQLBluePrintNode> getInheritingNodes() {
        return this.augmetations;
    }

    public void addColumn(Object node, String tableName) {
        XSQLColumn c = new XSQLColumn(node, getBluePrintNodeName(), this);
        this.columns.add(c);
    }

    public XSQLColumn addColumn(String name, String tableName, String origName,String origTableName) {
        XSQLColumn c = new XSQLColumn(name, tableName, origName, origTableName);
        this.columns.add(c);
        this.origNameToColumn.put(origName, c);
        return c;
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

    public XSQLBluePrintNode getParent() {
        return this.parent;
    }

    public String toString() {
        if (this.odlSchemaNodes != null) {
            return getBluePrintNodeName();
        }
        if (odlTableNames != null) {
            return odlTableNames[0];
        }
        return "Unknown";
    }

    public int getLevel() {
        return this.level;
    }

    @Override
    public boolean equals(Object obj) {
        XSQLBluePrintNode other = (XSQLBluePrintNode) obj;
        if (this.odlSchemaNodes != null) {
            return getBluePrintNodeName().equals(other.getBluePrintNodeName());
        } else if (this.odlTableNames == null && other.odlTableNames != null) {
            return false;
        }
        if (this.odlTableNames != null && other.odlTableNames == null) {
            return false;
        } else {
            if(this.odlTableNames.length!=other.odlTableNames.length)
                return false;
            for(int i=0;i<this.odlTableNames.length;i++){
                if(!this.odlTableNames[i].equals(other.odlTableNames[i]))
                    return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (this.odlSchemaNodes != null) {
            return bluePrintTableName.hashCode();
        }
        return 0;
    }
}
