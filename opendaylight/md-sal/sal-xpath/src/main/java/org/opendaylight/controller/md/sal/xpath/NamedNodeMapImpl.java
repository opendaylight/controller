/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com

 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Defines an empty NamedNodeMap.
 * @author Devin Avery
 *
 */
public class NamedNodeMapImpl implements NamedNodeMap {

    public static final NamedNodeMap EMPTY_MAP = new NamedNodeMapImpl();

    private NamedNodeMapImpl(){

    }

    @Override
    public Node getNamedItem(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node setNamedItem(Node arg) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node removeNamedItem(String name) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node item(int index) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Node getNamedItemNS(String namespaceURI, String localName)
            throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node setNamedItemNS(Node arg) throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node removeNamedItemNS(String namespaceURI, String localName)
            throws DOMException {
        // TODO Auto-generated method stub
        return null;
    }

}
