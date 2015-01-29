package org.opendaylight.controller.md.sal.dom.xsql;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.model.api.AugmentationSchema;
import org.opendaylight.yangtools.yang.model.api.ContainerSchemaNode;
import org.opendaylight.yangtools.yang.model.api.DataNodeContainer;
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.opendaylight.yangtools.yang.model.api.Status;
import org.opendaylight.yangtools.yang.model.util.Uint16;
import org.opendaylight.yangtools.yang.model.util.Uint32;
import org.opendaylight.yangtools.yang.model.util.Uint64;
import org.opendaylight.yangtools.yang.model.util.Uint8;

public class XSQLODLUtils {

    private static Map<Class<?>, Class<?>> types =
        new ConcurrentHashMap<Class<?>, Class<?>>();

    static {
        types.put(QName.class, QName.class);
        types.put(SchemaPath.class, SchemaPath.class);
        types.put(Status.class, Status.class);
    }

    public static boolean isColumnType(Class<?> cls) {
        return types.containsKey(cls);
    }

    public static String getTableName(Object odlNode) {
        if (odlNode instanceof Module) {
            return ((Module) odlNode).getNamespace().toString();
        } else if (odlNode instanceof DataSchemaNode) {
            SchemaPath p = ((DataSchemaNode) odlNode).getPath();
            return extractTableName(p);
        } else {
            int i = 0;
        }
        return null;
    }

    public static String extractTableName(SchemaPath path) {
        List<QName> lst = path.getPath();
        StringBuffer name = new StringBuffer();
        int i = 0;
        for (QName q : lst) {
            name.append(q.getLocalName());
            i++;
            if (i < lst.size()) {
                name.append("/");
            }
        }
        return name.toString();
    }

    public static String getBluePrintName(Object odlNode){
        if (odlNode instanceof Module) {
            return ((Module) odlNode).getNamespace().toString();
        } else if (odlNode instanceof DataSchemaNode) {
            SchemaPath p = ((DataSchemaNode) odlNode).getPath();
            return extractTableName(p);
        }
        return null;
    }

    public static String getODLNodeName(Object odlNode) {
        if (odlNode instanceof Module) {
            return ((Module) odlNode).getNamespace().toString();
        } else if (odlNode instanceof DataSchemaNode) {
            SchemaPath p = ((DataSchemaNode) odlNode).getPath();
            List<QName> lst = p.getPath();
            return lst.get(lst.size() - 1).toString();
        }
        return null;
    }

    public static List<QName> getPath(Object odlNode) {
        return ((DataSchemaNode) odlNode).getPath().getPath();
    }


    public static String getODLTableName(Object odlNode) {
        if (odlNode instanceof Module) {
            return ((Module) odlNode).getNamespace().toString();
        } else if (odlNode instanceof DataSchemaNode) {
            return ((DataSchemaNode) odlNode).getPath().toString();
        }
        return null;
    }

    public static String getNodeNameFromDSN(Object o) {
        DataSchemaNode node = (DataSchemaNode) o;
        String nodeName = node.getQName().toString();
        int index = nodeName.lastIndexOf(")");
        return nodeName.substring(index + 1);
    }

    public static boolean isModule(Object o) {
        if (o instanceof Module) {
            return true;
        }
        return false;
    }

    public static boolean createOpenDaylightCache(XSQLBluePrint bluePrint,Object module) {
        XSQLBluePrintNode node = new XSQLBluePrintNode(module, 0,null);
        bluePrint.addToBluePrintCache(node,null);
        collectODL(bluePrint, node, ((Module) module).getChildNodes(), 1);
        return true;
    }

    private static void collectODL(XSQLBluePrint bluePrint,
        XSQLBluePrintNode parent, Collection<DataSchemaNode> nodes, int level) {
        if (nodes == null) {
            return;
        }
        for (DataSchemaNode n : nodes) {
            if (n instanceof DataNodeContainer /*|| n instanceof LeafListSchemaNode*/|| n instanceof ListSchemaNode) {
                XSQLBluePrintNode bn = new XSQLBluePrintNode(n, level,parent);
                bn = bluePrint.addToBluePrintCache(bn,parent);
                if (n instanceof ListSchemaNode) {
                    level++;
                    collectODL(bluePrint, bn,((ListSchemaNode) n).getChildNodes(), level);
                    Set<AugmentationSchema> s = ((ListSchemaNode)n).getAvailableAugmentations();
                    if(s!=null){
                        for(AugmentationSchema as:s){
                            XSQLAdapter.log("AugL="+as.getChildNodes());
                            collectODL(bluePrint, bn,as.getChildNodes(), level);
                        }
                    }
                    level--;
                }else
                if (n instanceof DataNodeContainer) {
                    level++;
                    collectODL(bluePrint, bn,((DataNodeContainer) n).getChildNodes(), level);
                    if(n instanceof ContainerSchemaNode){
                       Set<AugmentationSchema> s = ((ContainerSchemaNode)n).getAvailableAugmentations();
                       if(s!=null){
                           for(AugmentationSchema as:s){
                               XSQLAdapter.log("AugD="+as.getChildNodes());
                               collectODL(bluePrint, bn,as.getChildNodes(), level);
                           }
                       }
                    }
                    level--;
                } else {
                    XSQLAdapter.log("Missed the following type:"+n.getClass().getName());
                }
            } else {
                if (parent != null) {
                    parent.addColumn(n, parent.getParent().getBluePrintNodeName());
                } else {
                    XSQLAdapter.log("NO Parent!");
                }
            }
        }
    }

    public static Map<String, Field> refFieldsCache =
        new HashMap<String, Field>();

    public static Field findField(Class<?> c, String name) {
        if (c == null) {
            return null;
        }
        String cacheKey = c.getName() + name;
        Field f = refFieldsCache.get(cacheKey);
        if (f != null) {
            return f;
        }

        try {
            f = c.getDeclaredField(name);
            f.setAccessible(true);
            refFieldsCache.put(cacheKey, f);
            return f;
        } catch (Exception err) {
        }

        Class<?> s = c.getSuperclass();
        if (s != null) {
            f = findField(s, name);
            if (f != null) {
                refFieldsCache.put(cacheKey, f);
            }
            return f;
        }
        return null;
    }


    public static Object get(Object o, String name) {
        try {
            Class<?> c = o.getClass();
            Field f = findField(c, name);
            return f.get(o);
        } catch (Exception err) {
            //XSQLAdapter.log(err);
        }
        return null;
    }

    public static List<Object> getMChildren(Object o) {
        Map<?, ?> children = getChildren(o);
        List<Object> result = new LinkedList<Object>();
        for (Object val : children.values()) {
            result.add((Object) val);
        }
        return result;
    }

    public static Map<?, ?> getChildren(Object o) {
        return (Map<?, ?>) get(o, "children");
    }

    public static Collection<?> getChildrenCollection(Object o) {
        Object value = get(o, "children");
        if(value==null)
            return new ArrayList();
        if(value instanceof Map)
            return ((Map<?,?>)value).values();
        else
        if(value instanceof Collection){
            return (Collection<?>)value;
        }else{
            XSQLAdapter.log("Unknown Child Value Type="+value.getClass().getName());
            return new ArrayList();
        }
    }

    public static Object getValue(Object o) {
        return get(o, "value");
    }

    public static String getNodeIdentiofier(Object o) {
        try{
            return ((PathArgument) get(o, "nodeIdentifier")).getNodeType().toString();
        }catch(Exception err){
            return null;
        }
    }

    public static String getNodeName(Object o) {
        Object nodeID = get(o, "nodeIdentifier");
        if (nodeID != null) {
            String nodeName = nodeID.toString();
            int index = nodeName.lastIndexOf(")");
            return nodeName.substring(index + 1);
        }
        return "NULL";
    }

    public static Class<?> getTypeForODLColumn(Object odlNode){
        Object type = get(odlNode,"type");
        if(type instanceof Uint32 || type instanceof Uint64){
            return long.class;
        }else
        if(type instanceof Uint16){
            return int.class;
        }else
        if(type instanceof Uint8){
            return byte.class;
        }
        return String.class;
    }

}
