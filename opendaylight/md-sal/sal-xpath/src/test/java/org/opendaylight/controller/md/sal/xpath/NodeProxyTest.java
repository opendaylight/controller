/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.md.sal.xpath;

import java.io.FileInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class NodeProxyTest {

    private final XPath xpath = XPathFactory.newInstance().newXPath();
    private Node root;

    private Document parseDocument( String file ) throws Exception{
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware( true );
        DocumentBuilder dBuilder = builderFactory.newDocumentBuilder();
        return dBuilder.parse( new FileInputStream(file));
    }

    @Before
    public void setUp() throws Exception{
        Document d = parseDocument( "src/test/resources/condensed_sample_yang.xml" );

        NamespaceContextManager nsContext = new NamespaceContextManager();
        nsContext.addNamespaceMapping( "ns", "opendaylight:xpath:test:simple:list" );
        xpath.setNamespaceContext( nsContext );

        //Node nodeRoot = (Node)xpath.evaluate( "/ns:topContainer", d, XPathConstants.NODE );
        //assertNotNull( nodeRoot );
        //d.removeChild( nodeRoot );


        //root = (NodeProxy)NodeProxy.wrapNode( nodeRoot );
        //root = nodeRoot; //NodeProxy.wrapNode( d );
        root = NodeProxy.wrapNode( d );
    }

    @Test
    public void test() throws Exception {

        evaluateXPath( "/ns:topContainer/ns:childContainer" );
        //evaluateXPath( "count(//ns:orderedListNestedList)" );

    }

    private void evaluateXPath(String string) throws Exception{
        System.out.println( "Result: '" + xpath.evaluate( string, root ) + "'" );
    }


}
