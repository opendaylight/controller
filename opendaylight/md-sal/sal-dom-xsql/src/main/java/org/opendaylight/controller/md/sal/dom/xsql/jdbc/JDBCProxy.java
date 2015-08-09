/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.xsql.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCProxy implements InvocationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JDBCProxy.class);
    private Object myObject = null;
    private Class<?> myObjectClass = null;

    public JDBCProxy(Object obj) {
        this.myObject = obj;
        this.myObjectClass = this.myObject.getClass();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LOG.debug("Class {} Method {}", this.myObjectClass.getSimpleName(), method.getName());
        return method.invoke(this.myObject, args);
    }
}
