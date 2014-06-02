package org.opendaylight.controller.northbound.commons.query;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

/**
 */
public class IteratableTypeInfo extends TypeInfo {

  public IteratableTypeInfo(String name, Accessor accessor) {
    super(name, accessor.getType(), accessor);
  }

  @Override
  public Object retrieve(Object target, String[] query, int index) {
    if (DEBUG) p("retrieve collection: " + index + "/" + query.length + " " + target.getClass());
    Collection c = (Collection) target;
    Iterator it = c.iterator();
    while (it.hasNext()) {
      Object item = it.next();
      for (TypeInfo child : _types.values()) {
        Object obj = child.retrieve(item, query, index);
        if (obj != null) return obj;
      }
    }
    return null;
  }

  @Override
  public synchronized void explore(Class clz) {
    if (_explored) return;
    if (DEBUG) p("explore iteratable type: " + clz + " generic:" + _accessor.getGenericType());
    Type t = _accessor.getGenericType();
    if (t instanceof ParameterizedType) {
      for(Type pt : ((ParameterizedType)t).getActualTypeArguments()) {
        Class c = (Class)pt;
        if (DEBUG) p("---" + c);
        _types.put(c.getName(), new TypeInfo(_name, c, null));
      }
    }
    _explored = true;
  }

}
