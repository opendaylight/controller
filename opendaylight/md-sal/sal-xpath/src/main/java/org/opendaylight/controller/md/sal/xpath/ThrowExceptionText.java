/*
 * Author : Neel Bommisetty
 * Email : neel250294@gmail.com
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.xpath;

import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

/**
 * Allows for fail-fast in case derived instances don't implement a method
 * correctly.
 *
 * @author Devin Avery
 * @author Neel Bommisetty
 *
 */
public class ThrowExceptionText extends ThrowExceptionNode {

    public String getData() throws DOMException {
        throw new RuntimeException( "Not Implemented" );
    }

    public void setData(String data) throws DOMException {
        throw new RuntimeException( "Not Implemented" );
    }

    public int getLength() {
        throw new RuntimeException( "Not Implemented" );
    }

    public String substringData(int offset, int count) throws DOMException {
        throw new RuntimeException( "Not Implemented" );
    }

    public void appendData(String arg) throws DOMException {
        throw new RuntimeException( "Not Implemented" );

    }

    public void insertData(int offset, String arg) throws DOMException {
        throw new RuntimeException( "Not Implemented" );

    }

    public void deleteData(int offset, int count) throws DOMException {
        throw new RuntimeException( "Not Implemented" );

    }

    public void replaceData(int offset, int count, String arg) throws DOMException {
        throw new RuntimeException( "Not Implemented" );

    }

    public Text splitText(int offset) throws DOMException {
        throw new RuntimeException( "Not Implemented" );
    }

    public boolean isElementContentWhitespace() {
        throw new RuntimeException( "Not Implemented" );
    }

    public String getWholeText() {
        throw new RuntimeException( "Not Implemented" );
    }

    public Text replaceWholeText(String content) throws DOMException {
        throw new RuntimeException( "Not Implemented" );
    }

}
