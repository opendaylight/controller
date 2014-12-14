package org.opendaylight.controller.northbound.commons.query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 */
/*package*/ class IteratableTypeInfo extends TypeInfo {

    public IteratableTypeInfo(String name, Accessor accessor) {
        super(name, accessor.getType(), accessor);
    }

    @Override
    public Object retrieve(Object target, String[] query, int index) throws QueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("retrieve collection: {}/{} type:{}", index, query.length,
                    target.getClass());
        }
        explore();
        Collection<?> c = (Collection<?>) target;
        Iterator<?> it = c.iterator();
        List<Object> objects = new ArrayList<Object>();
        while (it.hasNext()) {
            Object item = it.next();
            for (TypeInfo child : _types.values()) {
                Object val = child.retrieve(item, query, index);
                if (val != null) {
                    objects.add(val);
                }
            }
        }
        return objects;

    }

    @Override
    public synchronized void explore() {
        if (_explored) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exploring iteratable type: {} gtype: {}", _class,
                    _accessor.getGenericType());
        }
        Type t = _accessor.getGenericType();
        if (t instanceof ParameterizedType) {
            Type[] pt = ((ParameterizedType) t).getActualTypeArguments();
            // First type is a child, ignore rest
            if (pt.length > 0) {
                _types.put(_name, new TypeInfo(_name, (Class)pt[0], null));
            }
        }
        _explored = true;
    }

    @Override
    public TypeInfo getCollectionChild(Class<?> childType) {
        explore();
        for (TypeInfo ti : _types.values()) {
            if (ti.getType().equals(childType)) {
                return ti;
            }
        }
        return null;
    }
}
