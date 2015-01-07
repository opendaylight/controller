/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.opendaylight.datasand.agents;

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

import org.opendaylight.datasand.codec.ThreadPool;
import org.opendaylight.datasand.codec.TypeDescriptorsContainer;
import org.opendaylight.datasand.network.IFrameListener;
import org.opendaylight.datasand.network.NetworkID;
import org.opendaylight.datasand.network.NetworkNode;
import org.opendaylight.datasand.network.Packet;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class AutonomousAgentManager extends Thread implements IFrameListener {

    private NetworkNode networkNode = new NetworkNode(this);
    private Map<NetworkID, AutonomousAgent> networkIdToAgent = new HashMap<NetworkID, AutonomousAgent>();
    private Map<String, NetworkID> handlerNameToNetworkID = new HashMap<String, NetworkID>();
    private ThreadPool threadPool = new ThreadPool(20, "Handlers Threads", 2000);
    private Object agentsSyncObject = new Object();
    private Map<Integer, Set<AutonomousAgent>> multicasts = new HashMap<Integer, Set<AutonomousAgent>>();
    private boolean running = true;
    private TypeDescriptorsContainer typeContainer = null;

    public AutonomousAgentManager(TypeDescriptorsContainer _container) {
        this.typeContainer = _container;
        this.setName("Autonomous Agent Manager - " + networkNode.toString());
        this.start();
        //Sleep for 100 to allow the threads to start up
        try{Thread.sleep(100);}catch(Exception err){}
    }

    public TypeDescriptorsContainer getTypeDescriptorsContainer(){
        return this.typeContainer;
    }

    protected Object getSyncObject() {
        return this.agentsSyncObject;
    }

    public void shutdown() {
        networkNode.shutdown();
        this.running = false;
    }

    public void run() {
        while (running) {
            try {
                boolean addedTask = false;
                for (AutonomousAgent h : networkIdToAgent.values()) {
                    if (!h.working) {
                        h.checkForRepetitive();
                        if (h.incoming.size() > 0) {
                            h.pop();
                            threadPool.addTask(h);
                            addedTask = true;
                        }
                    }
                }
                if (!addedTask) {
                    synchronized (agentsSyncObject) {
                        try {
                            agentsSyncObject.wait(5000);
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                }
            } catch (ConcurrentModificationException err) {
                err.printStackTrace();
            }
        }
    }

    public NetworkNode getNetworkNode() {
        return this.networkNode;
    }

    public AutonomousAgent createHanlder(String className, ClassLoader cl) {
        return null;
    }

    public void addAgent(AutonomousAgent h) {
        this.networkIdToAgent.put(h.getAgentID(), h);
    }

    public void registerAgent(AutonomousAgent h) {
        networkIdToAgent.put(h.getAgentID(), h);
        handlerNameToNetworkID.put(h.getName(), h.getAgentID());
    }

    public AutonomousAgent getHandlerByID(NetworkID id) {
        return this.networkIdToAgent.get(id);
    }

    public AutonomousAgent getHandlerByName(String name) {
        NetworkID id = this.handlerNameToNetworkID.get(name);
        if (id != null) {
            return this.networkIdToAgent.get(id);
        }
        return null;
    }

    @Override
    public void process(Packet frame) {
        AutonomousAgent h = networkIdToAgent.get(frame.getDestination());
        if (h != null) {
            h.addFrame(frame);
            synchronized (agentsSyncObject) {
                agentsSyncObject.notifyAll();
            }
        }
    }

    public void messageWasEnqueued(){
        synchronized (agentsSyncObject) {
            agentsSyncObject.notifyAll();
        }
    }

    @Override
    public void processDestinationUnreachable(Packet frame) {
        AutonomousAgent h = networkIdToAgent.get(frame.getDestination());
        if (h != null) {
            h.addFrame(frame);
            synchronized (agentsSyncObject) {
                agentsSyncObject.notifyAll();
            }
        }
    }

    @Override
    public void processBroadcast(Packet frame) {
        for (AutonomousAgent agent : networkIdToAgent.values()) {
            agent.addFrame(frame);
        }
        synchronized (agentsSyncObject) {
            agentsSyncObject.notifyAll();
        }
    }

    @Override
    public void processMulticast(Packet frame) {
        int multicastGroupID = frame.getDestination().getPort();
        Set<AutonomousAgent> handlers = this.multicasts.get(multicastGroupID);
        if (handlers != null) {
            for (AutonomousAgent h : handlers) {
                h.addFrame(frame);
                synchronized (agentsSyncObject) {
                    agentsSyncObject.notifyAll();
                }
            }
        }
    }

    private static final String getClassNameFromEntryName(String entryName) {
        String result = replaceAll(entryName, "/", ".");
        int index = result.lastIndexOf(".");
        return result.substring(0, index);
    }

    public List<NetworkID> installJar(String jarFileName) {

        List<NetworkID> result = new ArrayList<NetworkID>();

        File f = new File(jarFileName);
        if (f.exists()) {
            try {
                JarInputStream in = new JarInputStream(new FileInputStream(f));
                JarEntry e = (JarEntry) in.getNextEntry();
                while (e != null) {
                    if (e.getName().indexOf("Handler") != -1) {
                        String className = getClassNameFromEntryName(e.getName());
                        try {
                            @SuppressWarnings("deprecation")
                            URLClassLoader cl = new URLClassLoader(new URL[] { f.toURL() }, this.getClass().getClassLoader());
                            /*
                             * @TODO ClassLoaders
                             */
                            // ModelClassLoaders.getInstance().addClassLoader(cl);
                            Class<?> handlerClass = cl.loadClass(className);
                            AutonomousAgent newHandler = (AutonomousAgent) handlerClass.getConstructor(new Class[] {NetworkID.class,AutonomousAgentManager.class })
                                    .newInstance(
                                            new Object[] {
                                                    this.networkNode
                                                            .getLocalHost(),
                                                    this });
                            registerAgent(newHandler);
                            newHandler.start();
                            result.add(newHandler.getAgentID());
                            cl.close();
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }
                    e = (JarEntry) in.getNextEntry();
                }
                in.close();
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
        return null;
    }

    public void registerForMulticast(int multicastGroup, AutonomousAgent h) {
        Set<AutonomousAgent> handlers = this.multicasts.get(multicastGroup);
        if (handlers == null) {
            handlers = new HashSet<AutonomousAgent>();
            this.multicasts.put(multicastGroup, handlers);
        }
        handlers.add(h);
    }

    public static String replaceAll(String src, String that, String withThis) {
        StringBuffer buff = new StringBuffer();
        int index0 = 0;
        int index1 = src.indexOf(that);
        if (index1 == -1)
            return src;
        while (index1 != -1) {
            buff.append(src.substring(index0, index1));
            buff.append(withThis);
            index0 = index1 + that.length();
            index1 = src.indexOf(that, index0);
        }
        buff.append(src.substring(index0));
        return buff.toString();
    }

    public void runSideTask(ISideTask task) {
        this.threadPool.addTask(task);
    }
}
