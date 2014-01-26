/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.tests.zmqroutingtable.rest;

import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;

import java.io.Serializable;
import java.net.URI;

/**
 * @author: syedbahm
 * Date: 12/10/13
 */
public class RouteIdentifierImpl implements RpcRouter.RouteIdentifier, Serializable {

    private final URI namespace;
    private final QName QNAME;
    private final QName instance;

    public RouteIdentifierImpl() {
        namespace = URI.create("http://cisco.com/example");
        QNAME = new QName(namespace, "global");
        instance = new QName(URI.create("127.0.0.1"), "local");
    }

    public RouteIdentifierImpl(String url,String instanceIP){
        namespace = URI.create(url);
        QNAME = new QName(namespace,"global");
        instance =  new QName(URI.create(instanceIP), "local");
    }


    @Override
    public QName getContext() {
        return QNAME;
    }

    @Override
    public QName getType() {
        return QNAME;
    }

    @Override
    public org.opendaylight.yangtools.yang.data.api.InstanceIdentifier getRoute() {
        return InstanceIdentifier.of(instance);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteIdentifierImpl that = (RouteIdentifierImpl) o;

        if (!QNAME.equals(that.QNAME)) return false;
        if (!instance.equals(that.instance)) return false;
        if (!namespace.equals(that.namespace)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + QNAME.hashCode();
        result = 31 * result + instance.hashCode();
        return result;
    }
}
