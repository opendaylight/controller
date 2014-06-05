package org.opendaylight.controller.northbound.commons.query;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper over a JAXB type to allow traversal of the object graph and
 * search for specific values in the object tree.
 */
/*package*/ class TypeInfo {

    public static final Logger LOGGER = LoggerFactory.getLogger(TypeInfo.class);
    public static final String DEFAULT_NAME = "##default";

    protected final String _name; // the jaxb name
    protected Class<?> _class; // jaxb type class
    protected final XmlAccessType _accessType; // jaxb access type
    protected final Accessor _accessor; // accessor to access object value
    protected Map<String,TypeInfo> _types = new HashMap<String,TypeInfo>();
    protected volatile boolean _explored = false;
    /**
     * Create a TypeInfo with a name and a class type. The accessor will be null
     * for a root node.
     */
    protected TypeInfo(String name, Class<?> clz, Accessor accessor) {
        _name = name;
        _class = clz;
        _accessor = accessor;
        XmlAccessorType accessorType = null;
        if(clz == null) {
          throw new NullPointerException("Type class can not be null");
        }
         accessorType = clz.getAnnotation(XmlAccessorType.class);
        _accessType = (accessorType == null ?
                XmlAccessType.PUBLIC_MEMBER : accessorType.value());
    }

    /**
     * @return the Accessor to access the value
     */
    public final Accessor getAccessor() {
        return _accessor;
    }

    /**
     * @return get the child by name
     */
    public final TypeInfo getChild(String name) {
        return _types.get(name);
    }

    /**
     * @return the object value by a selector query
     */
    public Object retrieve(Object target, String[] query, int index) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("retrieve: {}/{} type:{}", index, query.length, target.getClass());
        }
        if (index > query.length) return null;
        explore();
        TypeInfo child = getChild(query[index]);
        if (child == null) return null;
        target = child.getAccessor().getValue(target, null);
        if (index+1 == query.length) {
            // match found
            return target;
        }
        return child.retrieve(target, query, index+1);
    }

    /**
     * Explore the type info for children.
     */
    public synchronized void explore() {
        if (_explored) return;
        for (Class<?> c = _class; c != null; c = c.getSuperclass()) {
          // Currently only fields and methods annotated with JAXB annotations are
          // considered as valid for search purposes.
          //check methods first
          for (Method m : c.getDeclaredMethods()) {
            String tn = getTypeName(m);
            if (tn != null) {
              if (LOGGER.isDebugEnabled()) LOGGER.debug(
                  "exploring type: {} name: {} method: {}",
                  _class.getSimpleName(), tn, m);
              _types.put(tn, createTypeInfo(tn, new Accessor.MethodAccessor(m)));
            }
          }
          for (Field f : c.getDeclaredFields()) {
            String tn = getTypeName(f);
            if (tn != null) {
              if (LOGGER.isDebugEnabled()) LOGGER.debug(
                  "exploring type: {} name: {} field: {}",
                  _class.getSimpleName(), tn, f);
              _types.put(tn, createTypeInfo(tn, new Accessor.FieldAccessor(f)));
            }
          }
        }
        _explored = true;
    }

    public final String getTypeName(Field f) {
      // ignore static, transient and xmltransient fields
      if (Modifier.isTransient(f.getModifiers()) ||
          Modifier.isStatic(f.getModifiers()) ||
          f.getAnnotation(XmlTransient.class) != null ) {
        return null;
      }
      // try to read annotation
      String name = getTypeName(f.getAnnotations(), f.getName());
      if (name == null) { // no annotation present check accesstype
        if (_accessType == XmlAccessType.NONE) { // none return name
          return name;
        } else if (_accessType == XmlAccessType.FIELD) {
          // return field name if no annotation present
          return f.getName();
        } else if (_accessType == XmlAccessType.PUBLIC_MEMBER
            && Modifier.isPublic(f.getModifiers())) { // look for public access
          return f.getName();
        }
      }
      // return annotated name ( if any )
      return name;
    }

    public final String getTypeName(Method m) {
   // ignore static, transient and xmltransient fields
      if (Modifier.isTransient(m.getModifiers()) ||
          Modifier.isStatic(m.getModifiers()) ||
          m.getAnnotation(XmlTransient.class) != null ) {
        return null;
      }
      // try to read annotation
      String name = getTypeName(m.getAnnotations(), m.getName());
      if (name == null) { // no annotation present check accesstype
        if (_accessType == XmlAccessType.NONE) { // none return name
          return name;
        } else if (_accessType == XmlAccessType.PROPERTY) {
          // return bean property name if no annotation present
          return getBeanPropertyName(m);
        } else if (_accessType == XmlAccessType.PUBLIC_MEMBER
            && Modifier.isPublic(m.getModifiers())) { // look for public access
          return getBeanPropertyName(m);
        }
      }
      // return annotated name ( if any )
      return name;
    }

    private String getBeanPropertyName(Method m){
      try
      {
        Class<?> clazz=m.getDeclaringClass();
        BeanInfo info = Introspector.getBeanInfo(clazz);
        PropertyDescriptor[] props = info.getPropertyDescriptors();
        for (PropertyDescriptor pd : props)
        {
          if (m.equals(pd.getReadMethod())) return pd.getName();
        }
      }
      catch (IntrospectionException e)
      {
        LOGGER.error("Could not read bean property name for method = {}",
            m.getName(), e);
      }
      return null;
    }

    public static void p(String msg) {
        System.out.println("==== " + msg);
    }

    public static TypeInfo createRoot(String name, Class<?> clz) {
        // root is always a composite type
        // FIXME assert its a JAXB type
        XmlRootElement root = clz.getAnnotation(XmlRootElement.class);
        if (root == null) throw new IllegalArgumentException("Not a JAXB type: " + clz);
        if (name == null) name = getRootName(clz);
        return new TypeInfo(name, clz, null);
    }

    public static TypeInfo createTypeInfo(String name, Accessor accessor) {
      if (accessor.getAccessibleObject().getAnnotation(XmlElementWrapper.class) != null) {
        //XmlElementWrapperType
        return new WrapperTypeInfo(name, accessor);
      } else if (Collection.class.isAssignableFrom(accessor.getType())) {
            // collection type
            return new IteratableTypeInfo(name, accessor);
        }
        return new TypeInfo(name, accessor.getType(), accessor);
    }

    protected static String getRootName(Class<?> cls) {
        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        if (root == null) return null;
        String rootName = root.name();
        if (DEFAULT_NAME.equals(rootName)) {
            String clsName = cls.getSimpleName();
            rootName = Character.toLowerCase(clsName.charAt(0)) + clsName.substring(1);
        }
        return rootName;
    }

    protected static String getTypeName(Annotation[] annotations, String dflt) {
        String name = null;
        for (Annotation a : annotations) {
            if (a.annotationType() == XmlAttribute.class) {
                name = ((XmlAttribute)a).name();
            } else if (a.annotationType() == XmlElement.class) {
                name = ((XmlElement)a).name();
            } else if (a.annotationType() == XmlElementRef.class) {
                name = ((XmlElementRef)a).name();
            } else if (a.annotationType() == XmlElementWrapper.class) {
                name = ((XmlElementWrapper)a).name();
             // break the loop as we don't want name to be overwritten by XmlElement
                break;
            } else if (a.annotationType() == XmlType.class) {
                name = ((XmlType)a).name();
            } else if (a.annotationType() == XmlTransient.class) {
                // transient type
                return null;
            }
        }
        if (DEFAULT_NAME.equals(name)) return dflt;
        return name;
    }

    @Override
    public String toString() {
        return " TypeInfo [_name=" + _name + ", _class=" + _class
                + ", _accessType=" + _accessType + ", _accessor=" + _accessor
                + ", _types=" + _types + ", _explored=" + _explored + " ] ";
    }
}
