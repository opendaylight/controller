package org.opendaylight.controller.northbound.commons.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/*package*/ class Accessor {
    protected final AccessibleObject _accessorObj;

    public Accessor(AccessibleObject accessor) {
        _accessorObj = accessor;
        _accessorObj.setAccessible(true);
    }

    public AccessibleObject getAccessibleObject() {
        return _accessorObj;
    }

    public Annotation[] getAnnotations() {
        return _accessorObj.getAnnotations();
    }

    public Object getValue(Object parent) throws QueryException {
        try {
            if (_accessorObj instanceof Field) {
                return ((Field)_accessorObj).get(parent);
            } else {
                // assume method
                return ((Method)_accessorObj).invoke(parent);
            }
        } catch (Exception e) {
            throw new QueryException("Failure in retrieving value", e);
        }
    }
    public Type getGenericType() {
        if (_accessorObj instanceof Field) {
            return ((Field)_accessorObj).getGenericType();
        } else {
            // assume method
            return ((Method)_accessorObj).getGenericReturnType();
        }
    }
    public Class<?> getType() {

        if (_accessorObj instanceof Field) {
            return ((Field)_accessorObj).getType();
        } else {
            // assume method
            return ((Method)_accessorObj).getReturnType();
        }
    }

}
