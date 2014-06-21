package org.opendaylight.controller.md.sal.dom.xsql;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XSQLBluePrint implements DatabaseMetaData{
	
	/* for pure java interface analysis
	public static Class<?> BASE_INTERFACE = DataObject.class;
	public static Class<?> STOP_INTERFACE = DataObject.class;
	public static Class<?> ROOT_INTERFACE = DataObject.class;
	public static Method keyMethod = null;
	
	public static Object getKey(Object myModelObject){
		try{
			if(keyMethod==null){
				return myModelObject.toString();
			}else
				return keyMethod.invoke(myModelObject, (Object[])null);
		}catch(Exception err){
			XSQLAdapter.log(err);
		}
		return "NoKey";
	}
	
	public static Class getObjectClass(Object obj){
		if(obj==null)
			return null;
		//if the elements are dynamic proxy, this method should be override with 
		//how to get the dynamic proxy interface class from the Proxy object
		return obj.getClass();
		
	}*/
	
	public static final String CACHE_FILE_NAME = "BluePrintCache.dat"; 
	
	private Map<String,XSQLBluePrintNode> cache = new HashMap<String, XSQLBluePrintNode>();
	private boolean cacheLoadedSuccessfuly = false;
	private DatabaseMetaData myProxy = null;
	
	public static final String replaceAll(String source,String toReplace,String withThis){
		int index = source.indexOf(toReplace);
		int index2 = 0;
		StringBuffer result = new StringBuffer();
		while(index!=-1){
			result.append(source.substring(index2,index));
			result.append(withThis);
			index2 = index+toReplace.length();
			index = source.indexOf(toReplace,index2);
		}
		if(index2<source.length()){
			result.append(source.substring(index2));
		}
		return result.toString();
	}

	public XSQLBluePrint(){
	}
	
	private class NQLBluePrintProxy implements InvocationHandler{
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			System.out.println("Method "+method);
			return method.invoke(XSQLBluePrint.this, args);
		}		
	}
	
	public Map<String,XSQLBluePrintNode> getCache(){
		return cache;
	}
	
	public DatabaseMetaData getProxy(){
		if(myProxy==null)
			try{
				myProxy = (DatabaseMetaData)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{DatabaseMetaData.class},new NQLBluePrintProxy());
			}catch(Exception err){
				err.printStackTrace();
			}
		return myProxy;
	}
	
	public void loadBluePrintCache(String hostName) {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(hostName+"-"+CACHE_FILE_NAME));
			cache = (Map)in.readObject();
			in.close();
			cacheLoadedSuccessfuly = true;
		} catch (Exception err) {
			//err.printStackTrace();
		}
	}
		
	public XSQLBluePrintNode getBluePrintNode(String nodeName){
		if(nodeName.indexOf(".")!=-1){
			nodeName = nodeName.substring(nodeName.lastIndexOf(".")+1);
		}
		XSQLBluePrintNode node = cache.get(nodeName);
		
		if(node!=null)
			return node;

		for(XSQLBluePrintNode n:cache.values()){
			if(n.getName().toLowerCase().equals(nodeName.toLowerCase())){
				return n;
			}
		}
		
		for(XSQLBluePrintNode n:cache.values()){
			if(n.getName().toLowerCase().indexOf(nodeName.toLowerCase())!=-1){
				return n;
			}
		}
		return null;
	}
	
	
	public boolean isCacheLoaded(){
		return cacheLoadedSuccessfuly;
	}
	
	public XSQLBluePrintNode getNQLBluePrintNode(String nodeName){
		if(nodeName.indexOf(".")!=-1){
			return (XSQLBluePrintNode)cache.get(nodeName.substring(nodeName.lastIndexOf(".")+1));
		}
		return (XSQLBluePrintNode)cache.get(nodeName);
	}
	
	public Class getInterface(String name){
		XSQLBluePrintNode node = getNQLBluePrintNode(name);
		if(node!=null)
			return node.getInterface();
		return null;
	}
	
	public int getLevel(String name){
		XSQLBluePrintNode node = getNQLBluePrintNode(name);
		if(node!=null)
			return node.getLevel();
		return -1;		
	}
	
	public Class findInterface(String name){
		for(Iterator iter=cache.entrySet().iterator();iter.hasNext();){
			Map.Entry entry = (Map.Entry)iter.next();
			String intName = (String)entry.getKey();
			XSQLBluePrintNode node = (XSQLBluePrintNode)entry.getValue();
			if(intName.toLowerCase().indexOf(name.toLowerCase())!=-1){
				return node.getInterface();
			}
		}
		return null;
	}
	
	public void saveBluePrintCache(String hostName){
		try{
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(hostName+"-"+CACHE_FILE_NAME));
			out.writeObject(cache);
			out.close();
		}catch(Exception err){
			err.printStackTrace();
		}
	}
	
	
	private static Map<Class,Set<Class>> superClassMap = new HashMap<Class, Set<Class>>();
	
	public static Set<Class> getInheritance(Class myObjectClass,Class returnType){
		
		if(returnType!=null && myObjectClass.equals(returnType)){
			return new HashSet<Class>();
		}
		Set<Class> result = superClassMap.get(myObjectClass);
		if(result!=null) return result;
		result = new HashSet<Class>();
		superClassMap.put(myObjectClass, result);
		if(returnType!=null){
			if(!returnType.equals(myObjectClass)){
				Class mySuperClass = myObjectClass.getSuperclass();
				while(mySuperClass!=null){
					result.add(mySuperClass);
					mySuperClass = mySuperClass.getSuperclass();
				}
				result.addAll(collectInterfaces(myObjectClass));
			}
		}
		return result;
	}
	
	public static Set<Class> collectInterfaces(Class cls){
		Set<Class> result = new HashSet();
		Class myInterfaces[] = cls.getInterfaces();
		if(myInterfaces!=null){
			for(Class in:myInterfaces){
				result.add(in);
				result.addAll(collectInterfaces(in));
			}
		}
		return result;
	}
	
	/*
	private BluePrintNode addClassToCache(Class cls,Set beenHere){
		if(STOP_INTERFACE.equals(cls)) return null;
		
		BluePrintNode parent = null;
		if(cls!=null && !(STOP_INTERFACE.equals(cls.getInterfaces()[0]))){			
			parent = addClassToCache(cls.getInterfaces()[0],beenHere);
		}		
		
		BluePrintNode n = (BluePrintNode)cache.get(cls.getSimpleName());
		if(n==null && parent!=null){
			n = new BluePrintNode(cls, parent.getLevel());
		}
		if(n!=null && parent!=null){
			Set<BluePrintRelation> rs = parent.getClonedParents();
			for(BluePrintRelation r:rs){
				try{
					Method m = r.getParent().getInterface().getMethod("get"+r.getProperty(), null);
					if(m.getReturnType().isArray()){
						createBluePrintCache(null,m.getReturnType().getComponentType(),cls,"get"+r.getProperty(), r.getParent(), beenHere, n.getLevel());						
					}else{
						createBluePrintCache(null,m.getReturnType(),cls,"get"+r.getProperty(), r.getParent(), beenHere, n.getLevel());
					}
				}catch(Exception err){
					err.printStackTrace();
				}
			}
		}
		return n;
	}*/
	
	/* when loading from a jar file
	public ClassLoader createClassLoader(File f,List<URL> urls, boolean isRoot){
		if(urls==null){
			urls = new LinkedList<URL>();
		}
		File files[] = f.listFiles();
		for(File sub:files){
			if(sub.isDirectory()){
				createClassLoader(sub, urls, false);
			}else
			if(sub.getName().endsWith(".jar")){
				try{urls.add(sub.toURL());}catch(Exception err){};
			}
		}
		if(isRoot)
			return new URLClassLoader(urls.toArray(new URL[urls.size()]));
		return null;
	}*/
	
	/* when loading from a directory
	public void fillBluePrintFromDirectory(File dir, Map<String,XSQLBluePrintNode> bpNodes,String srcStart){
		File files[] = dir.listFiles();
		for(File f:files){
			if(f.isDirectory()){
				fillBluePrintFromDirectory(f, bpNodes,srcStart);
			}else
			if(f.getName().endsWith(".java") && f.getPath().indexOf("toaster")!=-1){
				String clsName = null;
				try{
					if(f.getPath().indexOf("toaster")!=-1){
						int x = 9;
					}
					int srcIndex = f.getPath().indexOf(srcStart);
					if(srcIndex==-1) continue;
					clsName = replaceAll(f.getPath().substring(srcIndex,f.getPath().length()-5),"/",".");
					Class cls = Class.forName(clsName);
					if(XSQLBluePrint.BASE_INTERFACE.isAssignableFrom(cls)){
						XSQLBluePrintNode node = new XSQLBluePrintNode(cls, 0);
						bpNodes.put(clsName,node);
					}
				}catch(Error e){
					if(f.getPath().indexOf("toaster")!=-1){
						System.err.println("Error instantiating class "+clsName);
						e.printStackTrace();
					}
				}catch(ClassNotFoundException cnf){
				}catch(Exception err){
					err.printStackTrace();
				}
			}
		}
		
	}*/
	
	/*
	public static void main(String args[]){
		XSQLBluePrint bp = new XSQLBluePrint();
		Map<String,XSQLBluePrintNode>  bNodes= new HashMap<String,XSQLBluePrintNode>();
		bp.fillBluePrintFromDirectory(new File("/home/saichler/ODL/controller"), bNodes,"org/opendaylight/");
		for(String s:bNodes.keySet()){
			System.out.println(s);
		}
		System.out.println("Done");
		System.exit(0);
	}*/
	
	/*
	public void fillBluePrintFromJar(String jarName,Set beenHere){
		try{
			ZipInputStream in = new ZipInputStream(new FileInputStream(jarName));
			ZipEntry entry = in.getNextEntry();
			List<BluePrintNode> discoveredClasses = new LinkedList<BluePrintNode>();
			while(entry!=null){
				String fileName = entry.getName();
				if(fileName.endsWith("class")){					
					String className = replaceAll(fileName, "/", ".");
					className = replaceAll(className, "\\", ".");
					className = className.substring(0,className.lastIndexOf("."));
					if(!className.endsWith("Impl") && !(className.toLowerCase().indexOf("client")!=-1)){
						Class cls = getClass().getClassLoader().loadClass(className);
						if(STOP_INTERFACE.isAssignableFrom(cls) && !STOP_INTERFACE.equals(cls)){
							BluePrintNode node = new BluePrintNode(cls, 0);
							cache.put(cls.getSimpleName(),node);						
							discoveredClasses.add(node);
						}
					}
				}
				entry = in.getNextEntry();
			}
			in.close();
			for(BluePrintNode node:discoveredClasses){
				createBluePrintCache(node);
			}
		}catch(Exception err){
			err.printStackTrace();
		}
	}*/
	/*
	public void createBluePrintCache(BluePrintNode node){
		Method methods[] = node.getInterface().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().startsWith("get")) {
				String mName = methods[i].getName();
				if(types.containsKey(methods[i].getReturnType())){
					node.addColumn(mName);
				}else
				if (methods[i].getReturnType().isArray() && isModelClass(methods[i].getReturnType().getComponentType())) {
					BluePrintNode cNode = (BluePrintNode)cache.get(methods[i].getReturnType().getComponentType().getSimpleName());
					if(cNode!=null)
						cNode.addParent(node, methods[i].getName().substring(3));
				} else if (isModelClass(methods[i].getReturnType())) {
					BluePrintNode cNode = (BluePrintNode)cache.get(methods[i].getReturnType().getSimpleName());
					if(cNode!=null)
						cNode.addParent(node, methods[i].getName().substring(3));					
				}else{
					node.addColumn(mName);					
				}
			}
		}
		if(node.getInterface().getInterfaces()!=null && node.getInterface().getInterfaces().length>0){
			BluePrintNode javaParent = (BluePrintNode)cache.get(node.getInterface().getInterfaces()[0].getSimpleName());
			if(javaParent!=null){
				javaParent.addInheritingNode(node);
				for(BluePrintRelation rel:javaParent.getParents()){
					node.addParent(rel.getParent(), rel.getProperty());
				}
			}
		}
	}*/
	
	/*
	public boolean isModelObject(Object obj){
		if(obj instanceof DataNodeContainer)
			return true;
		if(obj instanceof SchemaNode)
			return true;		
		return isModelClass(obj.getClass());
	}
	
	public boolean isModelClass(Class c){
		if(c==null)
			return false;
		if(c.getPackage().getName().indexOf("yang")!=-1){
			return true;
		}
		return false;
	}*/
			
	public void addToBluePrintCache(String cacheKey,XSQLBluePrintNode blNode){
		this.cache.put(cacheKey, blNode);
	}
		
	/* for java interface analysis 
	public void createBluePrintCache(Object element,Class returnType,String propertyName,Method m,XSQLBluePrintNode myParent,Set beenHere,int level){
		if(myParent!=null){
			String uniqueKey = null;
			if(element!=null)
				uniqueKey = myParent.toString()+propertyName+element.toString();
			else
				uniqueKey = myParent.toString()+propertyName+returnType.getName();

			if(beenHere.contains(uniqueKey))
					return;
			beenHere.add(uniqueKey);
		}
		
		Class eClass = null;
		if(element!=null){
			eClass = element.getClass();
		}else
			eClass = returnType;
		
		XSQLBluePrintNode node = (XSQLBluePrintNode)cache.get(eClass.getSimpleName());
		//Set<Class> supers = getInheritance(eClass, returnType);		
		if(node == null){
			node = new XSQLBluePrintNode(eClass,level);
			cache.put(node.toString(), node);
		}
		
		if(myParent!=null){
			node.addParent(myParent, propertyName.substring(3));
			/*
			for(Class sp:supers){				
				BluePrintNode spNode = (BluePrintNode)cache.get(sp.getSimpleName());
				if(spNode==null){
					spNode = new BluePrintNode(sp,level);
					createBluePrintCache(spNode);
					cache.put(sp.getSimpleName(), spNode);
				}
				spNode.addParent(myParent, propertyName.substring(3));
			}*/
		//}
		/*
		Method methods[] = eClass.getMethods();
		for (int i = 0; i < methods.length; i++) {
			methods[i].setAccessible(true);
			if(methods[i].getName().equals("getClass"))
				continue;
						
			//if(methods[i].getName().equals("getType") || 
			  // methods[i].getName().startsWith("is")){				
				//node.addColumn(methods[i].getName());				
			//}else
			if(methods[i].getName().startsWith("is") && methods[i].getParameterTypes().length==0) {
				String mName = methods[i].getName();				
				node.addColumn(mName);				
			}else
			if(methods[i].getName().startsWith("get") && methods[i].getParameterTypes().length==0) {
				String mName = methods[i].getName();
				
				//if(mName.equals("getParent")) continue;
				
				Class itemType = methods[i].getReturnType();
				
				if(XSQLODLUtils.isColumnType(itemType)){
					node.addColumn(mName);					
				}else
				if(!itemType.isArray() && !itemType.isPrimitive() && isModelClass(itemType)){
					try{
						Object item = methods[i].invoke(element, null);
						level++;						
						createBluePrintCache(item,item.getClass(), mName,methods[i], node, beenHere, level);
						level--;			
					}catch(Exception err){
						level++;
						createBluePrintCache(null, itemType, methods[i].getName(), methods[i], node, beenHere, level);
						level--;
					}
				}else
					/*
				if(Map.class.isAssignableFrom(methods[i].getReturnType())){
					if(methods[i].getParameterTypes()==null || methods[i].getParameterTypes().length==0){
						try{
							Map map = (Map)methods[i].invoke(element, null);							
							if(map!=null && map.size()>0){
								Object item = map.values().iterator().next();
								if(isModelClass(item.getClass())){
									for(Object subE:map.values()){
										level++;
										createBluePrintCache(subE,item.getClass(), mName,methods[i], node, beenHere, level);
										level--;
									}
								}
							}
						}catch(Exception err){
							Adapter.log("Class="+returnType+" Method="+methods[i].getName());
							Adapter.log(err);
						}												
					}
				}else*//*
				if(List.class.isAssignableFrom(methods[i].getReturnType())){
					try{
						if(element!=null){
							List lst = (List)methods[i].invoke(element, null);							
							if(lst!=null && lst.size()>0){
								Object item = lst.get(0);
								if(isModelClass(item.getClass())){
									for(Object subE:lst){
										level++;
										createBluePrintCache(subE,item.getClass(), mName,methods[i], node, beenHere, level);
										level--;
									}
								}
							}else{
								Type rType = methods[i].getGenericReturnType();
								if(rType instanceof ParameterizedType){
								    ParameterizedType type = (ParameterizedType) rType;
								    Type[] typeArguments = type.getActualTypeArguments();
								    for(Type typeArgument : typeArguments){
								        Class typeArgClass = (Class) typeArgument;
								        if(isModelClass(typeArgClass)){
											level++;								        	
											createBluePrintCache(null,typeArgClass, mName,methods[i], node, beenHere, level);
											level--;											
								        }
								    }
								}																	
							}
						}else{
							Type rType = methods[i].getGenericReturnType();
							if(rType instanceof ParameterizedType){
							    ParameterizedType type = (ParameterizedType) rType;
							    Type[] typeArguments = type.getActualTypeArguments();
							    for(Type typeArgument : typeArguments){
							        Class typeArgClass = (Class) typeArgument;
							        if(isModelClass(typeArgClass)){
										level++;								        	
										createBluePrintCache(null,typeArgClass, mName,methods[i], node, beenHere, level);
										level--;											
							        }
							    }
							}								
						}
					}catch(Exception err){
						XSQLAdapter.log(err);
					}						
				}else					
				if(Set.class.isAssignableFrom(methods[i].getReturnType())){
					try{
						if(element!=null){
							Set lst = (Set)methods[i].invoke(element, null);							
							if(lst!=null && lst.size()>0){
								Object item = lst.iterator().next();
								if(isModelClass(item.getClass())){
									for(Object subE:lst){
										level++;
										createBluePrintCache(subE,item.getClass(), mName,methods[i], node, beenHere, level);
										level--;
									}
								}
							}else{
						        Class typeArgClass = getMethodReturnTypeFromGeneric(methods[i]);
						        if(isModelClass(typeArgClass)){
									level++;								        	
									createBluePrintCache(null,typeArgClass, mName,methods[i], node, beenHere, level);
									level--;											
						        }
							}
						}else{
					        Class typeArgClass = getMethodReturnTypeFromGeneric(methods[i]);
					        if(isModelClass(typeArgClass)){
								level++;								        	
								createBluePrintCache(null,typeArgClass, mName,methods[i], node, beenHere, level);
								level--;											
					        }
						}
					}catch(Exception err){
						XSQLAdapter.log("Class="+returnType.getName()+" Method="+methods[i].getName());
						XSQLAdapter.log(err);
					}						
				}else{
					node.addColumn(mName);
				}
			}
		}
	}*/
	
	public Class getGenericType(ParameterizedType type){
	    Type[] typeArguments = type.getActualTypeArguments();	    
	    for(Type typeArgument : typeArguments){
	    	if(typeArgument instanceof ParameterizedType){
	    		ParameterizedType pType = (ParameterizedType)typeArgument;
	    		return (Class)pType.getRawType();
	    	}else
	    	if(typeArgument instanceof Class){
	    		return (Class) typeArgument;  
	    	}
	    }		
	    return null;
	}
	
	public Class getMethodReturnTypeFromGeneric(Method m){
		Type rType = m.getGenericReturnType();
		if(rType instanceof ParameterizedType){
			return getGenericType((ParameterizedType)rType);
		}								
		return null;		
	}
	/*
	public void createBluePrintCache(Object myModelObject,Class returnType,Class myModelInterface,String propertyName,BluePrintNode myParent,Set beenHere,int level) throws IOException,IllegalAccessException,InvocationTargetException{
		if(myParent!=null){
			if(myModelObject!=null){
				String uniqueKey = myParent.toString()+propertyName+getKey(myModelObject);			
				if(beenHere.contains(uniqueKey))
						return;
				beenHere.add(uniqueKey);
			}else{
				if(beenHere.contains(myParent.toString()+propertyName+myModelInterface.getSimpleName()))
						return;
				beenHere.add(myParent.toString()+propertyName+myModelInterface.getSimpleName());
			}
		}
				
		BluePrintNode node = (BluePrintNode)cache.get(myModelInterface.getSimpleName());		
		if(node == null){
			node = new BluePrintNode(myModelInterface,level);
			cache.put(node.toString(), node);
		}
		if(myParent!=null){
			node.addParent(myParent, propertyName.substring(3));
		}
		
		Set<Class> supers = getInheritance(myModelInterface, returnType);		
		for(Class sp:supers){				
			BluePrintNode spNode = (BluePrintNode)cache.get(sp.getSimpleName());
			if(spNode==null){
				spNode = new BluePrintNode(sp,level);
				cache.put(sp.getSimpleName(), spNode);
			}
			spNode.addParent(myParent, propertyName.substring(3));
			for(BluePrintRelation r:spNode.getParents()){
				node.addParent(r.getParent(), r.getProperty());
			}
		}
				
		Method methods[] = myModelInterface.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().startsWith("get") && methods[i].getParameterTypes().length==0) {
				methods[i].setAccessible(true);
				String mName = methods[i].getName();
				if(List.class.isAssignableFrom(methods[i].getReturnType())){
					List l = (List)methods[i].invoke(myModelObject, null);
					if(l.size()>0){
						Object o = l.iterator().next();
						if(isModelObject(o)){
							for(Object oo:l){
								createBluePrintCache(oo,methods[i].getReturnType().getComponentType(),getObjectClass(oo),methods[i].getName(),node,beenHere,level);
							}
						}
					}
				}else				
				if(Set.class.isAssignableFrom(methods[i].getReturnType())){
					Set s = (Set)methods[i].invoke(myModelObject, null);
					if(s.size()>0){
						Object o = s.iterator().next();
						if(isModelObject(o)){
							for(Object oo:s){
								createBluePrintCache(oo,methods[i].getReturnType().getComponentType(),getObjectClass(oo),methods[i].getName(),node,beenHere,level);
							}
						}
					}
				}else
				if (methods[i].getReturnType().isArray() && isModelClass(methods[i].getReturnType().getComponentType())) {
					if(myModelObject!=null){
						Object children[] = (Object[]) methods[i].invoke(myModelObject, null);
						if (children == null)
							continue;
						for (int j = 0; j < children.length; j++) {
							level++;
							createBluePrintCache(children[j],methods[i].getReturnType().getComponentType(),getObjectClass(children[j]),methods[i].getName(),node,beenHere,level);
							level--;
						}
					}else{
						level++;						
						createBluePrintCache(null,methods[i].getReturnType().getComponentType(),methods[i].getReturnType().getComponentType(),methods[i].getName(),node,beenHere,level);
						level--;
					}
				} else if (isModelClass(methods[i].getReturnType())) {
					if(myModelObject!=null){
						Object sonImo = null;
						try{
							sonImo = methods[i].invoke(myModelObject, null);
						}catch(Exception err){
							Adapter.log("Class "+returnType+" method"+methods[i].getName());
							Adapter.log(err);
						}
						if (sonImo == null)
							continue;
						level++;
						createBluePrintCache(sonImo,methods[i].getReturnType(),getObjectClass(sonImo),methods[i].getName(),node,beenHere,level);
					}else{
						level++;
						createBluePrintCache(null,methods[i].getReturnType(),methods[i].getReturnType(),methods[i].getName(),node,beenHere,level);						
					}
					level--;
				}else{
					node.addColumn(mName);					
				}
			}
		}
	}*/
					
	public List<String> getAllTableNames(){
		List<String> names = new ArrayList<String>();
		for(XSQLBluePrintNode n:this.cache.values()){
			if(!n.isModule() && !n.getColumns().isEmpty()){
				names.add(n.getTableName());
			}
		}
		return names;
	
	}
	
	public List<String> getInterfaceNames(XSQLBluePrintNode node){
		Set<XSQLBluePrintNode> children = node.getChildren();
		List<String> names = new ArrayList<String>();
		for(XSQLBluePrintNode n:children){
			if(!n.isModule() && !n.getColumns().isEmpty()){
				names.add(n.toString());
			}
			names.addAll(getInterfaceNames(n));
		}
		return names;
	}
	
	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		return false;
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException {		
		return true;
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern,
			String typeNamePattern, String attributeNamePattern)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema,
			String table, int scope, boolean nullable) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCatalogSeparator() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema,
			String table, String columnNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog,
			String parentSchema, String parentTable, String foreignCatalog,
			String foreignSchema, String foreignTable) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		return 0;
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		return "VNE Query Language";
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return "0.1";
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDriverMajorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getDriverMinorVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getDriverName() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDriverVersion() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema, String table)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getExtraNameCharacters() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern,
			String functionNamePattern, String columnNamePattern)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern,
			String functionNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getIdentifierQuoteString() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema, String table)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getJDBCMajorVersion() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getJDBCMinorVersion() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxConnections() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxIndexLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxStatements() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxTableNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxUserNameLength() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getNumericFunctions() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern,
			String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern,
			String procedureNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSchemaTerm() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSearchStringEscape() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSQLKeywords() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getSQLStateType() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getStringFunctions() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern,
			String typeNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern,String tableNamePattern, String[] types) throws SQLException {
		return new TablesResultSet(this);
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTimeDateFunctions() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern,
			String typeNamePattern, int[] types) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getURL() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUserName() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema,
			String table) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsConvert() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsConvert(int fromType, int toType)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions()
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly()
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsGroupBy() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level)
			throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsTransactions() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsUnion() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean supportsUnionAll() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean usesLocalFiles() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}
		
}
