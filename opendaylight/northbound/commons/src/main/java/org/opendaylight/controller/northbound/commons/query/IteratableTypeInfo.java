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
  public Object retrieve(Object target, String[] query, int index) {
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
        objects.add(child.retrieve(item, query, index));
      }
    }
    return objects;

  }

  @Override
  public synchronized void explore() {
    if (_explored) return;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exploring iteratable type: {} gtype: {}", _class,
          _accessor.getGenericType());
    }
    Type t = _accessor.getGenericType();
    if (t instanceof ParameterizedType) {
      for(Type pt : ((ParameterizedType)t).getActualTypeArguments()) {
        Class<?> c = (Class<?>)pt;
        _types.put(c.getName(), new TypeInfo(_name, c, null));
      }
    }
    _explored = true;
  }

}
