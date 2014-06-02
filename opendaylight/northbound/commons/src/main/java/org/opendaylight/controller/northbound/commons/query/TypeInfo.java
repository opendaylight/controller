package org.opendaylight.controller.northbound.commons.query;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper over a JAXB type to allow traversal of the object graph and
 * search for specific values in the object tree.
 */
public class TypeInfo {

  protected static final boolean DEBUG = true;
  public static final String DEFAULT_NAME = "##default";

  protected final String _name; // the jaxb name
  protected Class _class; // jaxb type class
  protected final XmlAccessType _accessType; // jaxb access type
  protected final Accessor _accessor; // accessor to access object value
  protected Map<String,TypeInfo> _types = new HashMap<String,TypeInfo>(); // children
  protected volatile boolean _explored = false; // if node has been explored or not

  /**
   * Create a TypeInfo with a name and a class type. The accessor will be null
   * for a root node.
   */
  protected TypeInfo(String name, Class clz, Accessor accessor) {
    _name = name;
    _class = clz;
    _accessor = accessor;

    XmlAccessorType accessorType = (XmlAccessorType)
        clz.getAnnotation(XmlAccessorType.class);
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
    if (DEBUG) p("retrieve: " + index + "/" + query.length + " " + target.getClass());
    if (index > query.length) return null;
    explore(_class);
    // TODO - handle XmlElementWrapper types
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
 public synchronized void explore(Class clz) {
   if (_explored) return;
   for (Class c = clz; c != null; c = c.getSuperclass()) {
     // if class is not jaxb type bail out

     for (Field f : c.getDeclaredFields()) {
       String tn = getTypeName(f);
       if (tn != null) {
         if (DEBUG) p("exploring " + clz.getSimpleName() + " " + tn + " " + f);
         _types.put(tn, createTypeInfo(tn, new Accessor.FieldAccessor(f)));
       }
     }
     for (Method m : c.getDeclaredMethods()) {
       String tn = getTypeName(m);
       if (tn != null) {
         if (DEBUG) p("exploring " + clz.getSimpleName() + " " + tn + " " + m);
         _types.put(tn, createTypeInfo(tn, new Accessor.MethodAccessor(m)));
       }
     }
   }
   _explored = true;
 }

  public static final String getTypeName(Field f) {
    if (Modifier.isStatic(f.getModifiers())) return null;
    Class clz = f.getType();
    Type gclz = f.getGenericType();
    Annotation[] annos = f.getAnnotations();
    /*
    // check default,
    if (_accessType == XmlAccessType.FIELD) {
      return true;
    } else if (_accessType == XmlAccessType.NONE) {
      // check if the field is annotated
      return (getTfieldypeName(f.getAnnotations()) != null);
    }
    */

    return getTypeName(f.getAnnotations(), f.getName());
  }

  public static final String getTypeName(Method m) {
    if (Modifier.isStatic(m.getModifiers())) return null;
    return getTypeName(m.getAnnotations(), m.getName());
  }

  public static void p(String msg) {
    System.out.println("==== " + msg);
  }

  public static TypeInfo createRoot(String name, Class<?> clz) {
    // root is always a composite type
    // FIXME assert its a JAXB type
    XmlRootElement root = (XmlRootElement) clz.getAnnotation(XmlRootElement.class);
    if (root == null) throw new IllegalArgumentException("Not a JAXB type: " + clz);
    if (name == null) name = getRootName(clz);
    return new TypeInfo(name, clz, null);
  }

  public static TypeInfo createTypeInfo(String name, Accessor accessor) {
    if (Collection.class.isAssignableFrom(accessor.getType())) {
      // collection type
      return new IteratableTypeInfo(name, accessor);
    }
    return new TypeInfo(name, accessor.getType(), accessor);
  }

  public static TypeInfo create(XmlAccessorType access, Annotation[] annos, Class type, Type gtype) {
    String name = getTypeName(annos, null);
    if (name == null) return null;
    if (java.util.Collection.class.isAssignableFrom(type)) {
      // iteratable type
    }
    return null;
  }

  protected static String getRootName(Class cls) {
    XmlRootElement root = (XmlRootElement) cls.getAnnotation(XmlRootElement.class);
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

}
