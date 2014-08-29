/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.md.sal.xpath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;

final class NamespaceContextManager implements NamespaceContext {

    private final Map<String,String> prefixToNamespace = new HashMap<>();
    private final Map<String,String> namespaceToPrefix = new HashMap<>();

    public void addNamespaceMapping( String prefix, String namespace ){
        prefixToNamespace.put( prefix, namespace );
        namespaceToPrefix.put( namespace, prefix );
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        System.out.println( "getPrefixes: " + namespaceURI );
        return Collections.singleton( namespaceToPrefix.get( namespaceURI ) ).iterator();
    }

    @Override
    public String getPrefix(String namespaceURI) {
        System.out.println( "getPrefix: " + namespaceURI );
        return namespaceToPrefix.get( namespaceURI );
    }

    @Override
    public String getNamespaceURI(String prefix) {
        System.out.println( "getNamespaceURI: '" + prefix + "'");
        return prefixToNamespace.get( prefix );
    }
}