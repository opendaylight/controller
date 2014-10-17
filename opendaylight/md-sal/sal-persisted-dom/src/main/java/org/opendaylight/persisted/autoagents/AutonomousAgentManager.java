package org.opendaylight.persisted.autoagents;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.opendaylight.persisted.net.IFrameListener;
import org.opendaylight.persisted.net.NetworkID;
import org.opendaylight.persisted.net.NetworkNode;
import org.opendaylight.persisted.net.Packet;

public class AutonomousAgentManager extends Thread implements IFrameListener{
	
	private NetworkNode networkNode = new NetworkNode(this);
	private Map<NetworkID, AutonomousAgent> networkIdToHandler = new HashMap<NetworkID, AutonomousAgent>();
	private Map<String,NetworkID> handlerNameToNetworkID = new HashMap<String,NetworkID>();
	private ThreadPool threadPool = new ThreadPool(20, "Handlers Threads", 2000);
	private Object handlersSyncObject = new Object();
	private Map<Integer,Set<AutonomousAgent>> multicasts = new HashMap<Integer,Set<AutonomousAgent>>();
	
	public AutonomousAgentManager(){
		this.start();
	}
	
	protected Object getSyncObject(){
		return this.handlersSyncObject;
	}
	
	public void run(){
		while(true){
			try{
				boolean addedTask = false;
				for(AutonomousAgent h:networkIdToHandler.values()){					
					if(!h.working){
						h.checkForRepetitive();
						if(h.incoming.size()>0){;
							h.pop();
							threadPool.addTask(h);
							addedTask = true;
						}
					}
				}
				if(!addedTask){
					synchronized(handlersSyncObject){
						try{handlersSyncObject.wait(5000);}catch(Exception err){err.printStackTrace();}
					}
				}
			}catch(ConcurrentModificationException err){
				err.printStackTrace();
			}
		}
	}
	
	public NetworkNode getNetworkNode(){
		return this.networkNode;
	}
	
	public AutonomousAgent createHanlder(String className,ClassLoader cl){
		return null;
	}
	
	public void addHandler(AutonomousAgent h){
		this.networkIdToHandler.put(h.getHandlerID(), h);
	}
	
	public void registerHandler(AutonomousAgent h){
		networkIdToHandler.put(h.getHandlerID(), h);
		handlerNameToNetworkID.put(h.getName(), h.getHandlerID());
	}
	
	public AutonomousAgent getHandlerByID(NetworkID id){
		return this.networkIdToHandler.get(id);
	}
	
	public AutonomousAgent getHandlerByName(String name){
		NetworkID id = this.handlerNameToNetworkID.get(name);
		if(id!=null){
			return this.networkIdToHandler.get(id);
		}
		return null;
	}
	
	@Override
	public void process(Packet frame) {
		AutonomousAgent h = networkIdToHandler.get(frame.getDestination());
		if(h!=null){
			h.addFrame(frame);			
			synchronized(handlersSyncObject){
				handlersSyncObject.notifyAll();
			}
		}
	}

	@Override
	public void processDestinationUnreachable(Packet frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processBroadcast(Packet frame) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processMulticast(Packet frame) {
		int multicastGroupID = frame.getDestination().getPort();
		Set<AutonomousAgent> handlers = this.multicasts.get(multicastGroupID);
		if(handlers!=null){
			for(AutonomousAgent h:handlers){
				h.addFrame(frame);			
				synchronized(handlersSyncObject){
					handlersSyncObject.notifyAll();
				}		
			}
		}
	}
	
	private static final String getClassNameFromEntryName(String entryName){
		String result = replaceAll(entryName, "/", ".");
		int index = result.lastIndexOf(".");
		return result.substring(0,index);
	}
	
	public List<NetworkID> installJar(String jarFileName){
		
		List<NetworkID> result = new ArrayList<NetworkID>();
		
		File f = new File(jarFileName);
		if(f.exists()){
			try{
				JarInputStream in = new JarInputStream(new FileInputStream(f));
				JarEntry e = (JarEntry)in.getNextEntry();
				while(e!=null){
					if(e.getName().indexOf("Handler")!=-1){
						String className = getClassNameFromEntryName(e.getName());
						try{
							URLClassLoader cl = new URLClassLoader(new URL[]{f.toURL()},this.getClass().getClassLoader());
							/*
							 * @TODO ClassLoaders   
							 */
							//ModelClassLoaders.getInstance().addClassLoader(cl);
							Class handlerClass = cl.loadClass(className);
							AutonomousAgent newHandler = (AutonomousAgent)handlerClass.getConstructor(new Class[]{NetworkID.class,AutonomousAgentManager.class}).newInstance(new Object[]{this.networkNode.getLocalHost(),this});
							registerHandler(newHandler);
							newHandler.start();
							result.add(newHandler.getHandlerID());
						}catch(Exception err){err.printStackTrace();}
					}
					e = (JarEntry)in.getNextEntry();
				}
			}catch(Exception err){
				err.printStackTrace();
			}
		}
		return null;
	}
	
	public void registerForMulticast(int multicastGroup,AutonomousAgent h){
		Set<AutonomousAgent> handlers = this.multicasts.get(multicastGroup);
		if(handlers == null){
			handlers = new HashSet<AutonomousAgent>();
			this.multicasts.put(multicastGroup, handlers);
		}
		handlers.add(h);
	}	
	
	public static String replaceAll(String src,String that,String withThis){
		StringBuffer buff = new StringBuffer();
		int index0 = 0;
		int index1 = src.indexOf(that);
		if(index1==-1) return src;
		while(index1!=-1){
			buff.append(src.substring(index0,index1));
			buff.append(withThis);
			index0 = index1+that.length();
			index1 = src.indexOf(that,index0);
		}
		buff.append(src.substring(index0));
		return buff.toString();
	}
	
	public void runSideTask(ISideTask task){
		this.threadPool.addTask(task);
	}
}
