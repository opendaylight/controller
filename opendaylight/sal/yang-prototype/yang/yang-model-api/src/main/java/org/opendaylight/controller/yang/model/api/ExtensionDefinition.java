/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.yang.model.api;

public interface ExtensionDefinition extends SchemaNode {

    /**
     * Returns the <code>String</code> that is the name of argument to the
     * Keyword. If no argument statement is present the method will return
     * <code>null</code> <br>
     * The argument statement is defined in <a
     * href="https://tools.ietf.org/html/rfc6020#section-7.17.2">[RFC-6020] The
     * argument Statement</a>
     * 
     * @return the <code>String</code> that is the name of argument to the
     *         Keyword. If no argument statement is present the method will
     *         return <code>null</code>
     */
    public String getArgument();

    /**
     * This statement indicates if the argument is mapped to an XML element in
     * YIN or to an XML attribute.<br>
     * By contract if implementation of ExtensionDefinition does not specify the
     * yin-element statement the return value is by default set to
     * <code>false</code>
     * 
     * <br>
     * <br>
     * For more specific definition please look into <a
     * href="https://tools.ietf.org/html/rfc6020#section-7.17.2.2">[RFC-6020]
     * The yin-element Statement</a>
     * 
     * @return <code>true</code> if the argument is mapped to an XML element in
     *         YIN or returns <code>false</code> if the argument is mapped to an
     *         XML attribute.
     */
    public boolean isYinElement();
}
