/**
 * 
 */
package org.opendaylight.controller.yang.data.impl;

import groovy.util.BuilderSupport;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.opendaylight.controller.yang.data.api.ModifyAction;
import org.opendaylight.controller.yang.data.api.MutableCompositeNode;
import org.opendaylight.controller.yang.data.api.Node;
import org.opendaylight.controller.yang.data.api.SimpleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author michal.rehak
 *
 */
public class MyNodeBuilder extends BuilderSupport {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(MyNodeBuilder.class);

    private URI qnNamespace;
    private String qnPrefix;
    private Date qnRevision;
    
    private CompositeNode rootNode;

	/**
	 * @param baseQName
	 */
	private MyNodeBuilder(QName baseQName) {
		qnNamespace = baseQName.getNamespace();
		qnPrefix = baseQName.getPrefix();
		qnRevision = baseQName.getRevision();
    }

	/**
	 * @return initialized singleton instance
	 */
	public static MyNodeBuilder newInstance() {
    	QName qName = null;
    	try {
			qName = new QName(
	                new URI("urn:opendaylight:controller:network"), 
	                new Date(42), "yang-data-impl-groovyTest_", null);
        } catch (URISyntaxException e) {
	        LOG.error(e.getMessage(), e);
        }
        return new MyNodeBuilder(qName);
    }

    @Override
    protected void setParent(Object parent, Object child) {
    	// do nothing
        if (child instanceof AbstractNodeTO<?>) {
            ((AbstractNodeTO<?>) child).setParent((CompositeNode) parent);
        } else {
            LOG.error("PARENTING FAILED: "+parent + " -> " + child);
        }
    }

    @Override
    protected Object createNode(Object name) {
        MutableCompositeNode newNode = NodeFactory.createMutableCompositeNode(
                createQName(name), getCurrentNode(), null, null, null);
        NodeUtils.fixParentRelation(newNode);
        return newNode;
    }

    @Override
    protected Object createNode(Object name, @SuppressWarnings("rawtypes") Map attributes) {
        ModifyAction modifyAction = processAttributes(attributes);
        MutableCompositeNode newNode = NodeFactory.createMutableCompositeNode(
                createQName(name), getCurrentNode(), null, modifyAction, null);
        NodeUtils.fixParentRelation(newNode);
        return newNode;
    }


    @Override
    protected Object createNode(Object name, @SuppressWarnings("rawtypes") Map attributes, Object value) {
        ModifyAction modifyAction = processAttributes(attributes);
        SimpleNode<Object> newNode = NodeFactory.createImmutableSimpleNode(
                createQName(name), (CompositeNode) getCurrent(), value, modifyAction);
        NodeUtils.fixParentRelation(newNode);
        return newNode;
    }
    
    /**
     * @param attributes
     * @return 
     */
    private ModifyAction processAttributes(@SuppressWarnings("rawtypes") Map attributes) {
        LOG.debug("attributes:" + attributes);
        ModifyAction modAction = null;
        
        @SuppressWarnings("unchecked")
        Map<String, String> attributesSane = attributes;
        for (Entry<String, String> attr : attributesSane.entrySet()) {
            switch (attr.getKey()) {
            case "xmlns":
                try {
                    qnNamespace = new URI(attr.getValue());
                } catch (URISyntaxException e) {
                    LOG.error(e.getMessage(), e);
                }
                break;
            case "modifyAction":
                modAction = ModifyAction.valueOf(attr.getValue());
                break;
                
            default:
                throw new IllegalArgumentException("Attribute not supported: "+attr.getKey());
            }
        }
        return modAction;
    }

    @Override
    protected Object createNode(Object name, Object value) {
        SimpleNode<Object> newNode = NodeFactory.createImmutableSimpleNode(createQName(name), (CompositeNode) getCurrent(), value);
        NodeUtils.fixParentRelation(newNode);
        return newNode;
    }

    private QName createQName(Object localName) {
    	LOG.debug("qname for: "+localName);
	    return new QName(qnNamespace, qnRevision, qnPrefix, (String) localName);
    }

	protected CompositeNode getCurrentNode() {
	    if (getCurrent() != null) {
	        if (getCurrent() instanceof CompositeNode) {
	            return (CompositeNode) getCurrent();
	            
	        } else {
	            throw new IllegalAccessError("current node is not of type CompositeNode, but: "
	                +getCurrent().getClass().getSimpleName());
	        }
	    }
	    
	    return null;
    }
	
	@Override
	protected Object postNodeCompletion(Object parent, Object node) {
	    Node<?> nodeRevisited = (Node<?>) node;
	    LOG.debug("postNodeCompletion at: \n  "+ nodeRevisited+"\n  "+parent);
	    if (nodeRevisited instanceof MutableCompositeNode) {
	        MutableCompositeNode mutant = (MutableCompositeNode) nodeRevisited;
	        if (mutant.getValue().isEmpty()) {
	            LOG.error("why is it having empty value? -- " + mutant);
	        }
	        nodeRevisited = NodeFactory.createImmutableCompositeNode(
	                mutant.getNodeType(), mutant.getParent(), mutant.getValue(), mutant.getModificationAction());
	        NodeUtils.fixChildrenRelation((CompositeNode) nodeRevisited);

	        if (parent == null) {
	            rootNode = (CompositeNode) nodeRevisited;
	        } else {
	            NodeUtils.fixParentRelation(nodeRevisited);
	            nodeRevisited.getParent().getChildren().remove(mutant);
	        }
	    }
	    
	    
	    return nodeRevisited;
	}
	
	/**
	 * @return tree root
	 */
	public CompositeNode getRootNode() {
        return rootNode;
    }
}