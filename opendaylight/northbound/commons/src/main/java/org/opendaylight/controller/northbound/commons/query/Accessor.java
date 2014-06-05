package org.opendaylight.controller.northbound.commons.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/*package*/ interface Accessor {

  public Object getValue(Object parent, Class<?> type);

  public Annotation[] getAnnotations();
  public Type getGenericType();
  public Class<?> getType();
  public AccessibleObject getAccessibleObject();

  public static class FieldAccessor implements Accessor {
    private final Field _field;

    public FieldAccessor(Field f) {
      _field = f;
      _field.setAccessible(true);
    }

    @Override
    public Annotation[] getAnnotations() {
      return _field.getAnnotations();
    }

    @Override
    public Type getGenericType() {
      return _field.getGenericType();
    }

    @Override
    public Class<?> getType() {
      return _field.getType();
    }


    @Override
    public Object getValue(Object parent, Class<?> type) {
      try {
        return _field.get(parent);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return _field;
      }
  }

  public static class MethodAccessor implements Accessor {
    private final Method _method;

    public MethodAccessor(Method m) {
      _method = m;
      _method.setAccessible(true);
    }

    @Override
    public Annotation[] getAnnotations() {
      return _method.getAnnotations();
    }

    @Override
    public Type getGenericType() {
      return _method.getGenericReturnType();
    }

    @Override
    public Class<?> getType() {
      return _method.getReturnType();
    }
    @Override
    public Object getValue(Object parent, Class<?> type) {
      try {
        return _method.invoke(parent);
      } catch (IllegalAccessException e) {
        throw new IllegalStateException(e);
      } catch (InvocationTargetException e) {
        throw new IllegalStateException(e);
      }
    }

    @Override
    public AccessibleObject getAccessibleObject() {
      return _method;
    }
  }
}
