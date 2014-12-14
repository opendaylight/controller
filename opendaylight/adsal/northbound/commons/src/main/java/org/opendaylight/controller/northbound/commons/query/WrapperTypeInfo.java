/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.northbound.commons.query;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.xml.bind.annotation.XmlElement;

public class WrapperTypeInfo extends TypeInfo {

    protected WrapperTypeInfo(String name, Accessor accessor) {
        super(name, accessor.getType(), accessor);
    }

    @Override
    public Object retrieve(Object target, String[] query, int index) throws QueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("retrieve collection: {}/{}", index, query.length);
        }
        if (index >= query.length) {
            return null;
        }
        explore();
        TypeInfo child = getChild(query[index]);
        if (child == null) {
            return null;
        }
        if (query.length == index+1) { // skipping this node
            return target;
        }else { // if list of list go to next node to get value
            return child.retrieve(target, query, index+1);
        }
    }

    @Override
    public synchronized void explore() {
        if (_explored) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exploring wrapper type: {} gtype: {}", _class,
                    _accessor.getGenericType());
        }
        String tn = null;
        AccessibleObject accessibleObject = _accessor.getAccessibleObject();
        XmlElement xmlElement = accessibleObject.getAnnotation(XmlElement.class);
        if (accessibleObject instanceof Field) {
            Field f = (Field) accessibleObject;
            tn = DEFAULT_NAME.equals(xmlElement.name())?f.getName() : xmlElement.name();
        }else if (accessibleObject instanceof Method) {
            Method m = (Method) accessibleObject;
            tn = DEFAULT_NAME.equals(xmlElement.name())?m.getName() : xmlElement.name();
        }
        this._types.put(tn, new IteratableTypeInfo(tn, this._accessor));
        _explored = true;
    }

}
