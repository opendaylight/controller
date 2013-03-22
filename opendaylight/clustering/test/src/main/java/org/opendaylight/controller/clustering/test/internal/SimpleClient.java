
/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.clustering.test.internal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.CacheListenerAddException;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustering.services.IGetUpdates;
import org.opendaylight.controller.clustering.services.IListenRoleChange;
import org.opendaylight.controller.clustering.services.ListenRoleChangeAddException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleClient implements CommandProvider {
    protected static Logger logger = LoggerFactory
            .getLogger(SimpleClient.class);
    IClusterServices icluster;
    DoListenRoleChanged doListen;

    public void _tbegin(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        try {
            this.icluster.tbegin();
            ci.println("Transaction Open "
                    + this.icluster.tgetTransaction().toString());
        } catch (Exception e) {
            ci.println("Caught exception during transaction begin: " + e);
        }
    }

    public void _tcommit(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        try {
            ci.println("Committing transaction ....."
                    + this.icluster.tgetTransaction().toString());
            this.icluster.tcommit();
            ci.println("Transaction Committed");
        } catch (Exception e) {
            ci.println("Caught exception during transaction commit: " + e);
        }
    }

    public void _trollback(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        try {
            ci.println("Rolling back transaction ....."
                    + this.icluster.tgetTransaction().toString());
            this.icluster.trollback();
            ci.println("Transaction Rolled Back");
        } catch (Exception e) {
            ci.println("Caught exception during transaction rollback: " + e);
        }
    }

    public void _cacheinfo(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        if (!this.icluster.existCache(containerName, cacheName)) {
            ci.println("\tCache " + cacheName + " doesn't exists");
            return;
        }
        ci.println("\tInfo for cache " + cacheName + " on container "
                + containerName);
        Properties p = this.icluster.getCacheProperties(containerName,
                cacheName);
        if (p != null) {
            for (String key : p.stringPropertyNames()) {
                ci.println("\t\t" + key + " = " + p.getProperty(key));
            }
        }
    }

    public void _setLogLevel(CommandInterpreter ci) {
        String loggerName = ci.nextArgument();
        if (loggerName == null) {
            ci.println("Logger Name not supplied");
            return;
        }
        String loggerLevel = ci.nextArgument();
        if (loggerLevel == null) {
            ci.println("Logger Level not supplied");
            return;
        }

        ch.qos.logback.classic.Logger l = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(loggerName);
        ch.qos.logback.classic.Level level = ch.qos.logback.classic.Level
                .toLevel(loggerLevel);
        if (level == null) {
            ci.println("Level not understood");
            return;
        }
        l.setLevel(level);
    }

    private String retrieveLogLevel(ch.qos.logback.classic.Logger l) {
        if (l == null) {
            return ("Logger not supplied");
        }
        ch.qos.logback.classic.Level level = l.getLevel();
        if (level == null) {
            return ("Logger " + l.getName() + " at unknown level");
        } else {
            return ("Logger " + l.getName() + " at level " + l.getLevel()
                    .toString());
        }
    }

    public void _getLogLevel(CommandInterpreter ci) {
        String loggerName = ci.nextArgument();
        ch.qos.logback.classic.LoggerContext lc = (ch.qos.logback.classic.LoggerContext) LoggerFactory
                .getILoggerFactory();
        if (lc != null) {
            for (ch.qos.logback.classic.Logger l : lc.getLoggerList()) {
                if (loggerName == null || l.getName().startsWith(loggerName)) {
                    ci.println(retrieveLogLevel(l));
                }
            }
        }
    }

    public void _create(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        try {
            if (cacheName.startsWith("T-")) {
                this.icluster.createCache(containerName, cacheName, EnumSet
                        .of(IClusterServices.cacheMode.TRANSACTIONAL));
            } else {
                this.icluster.createCache(containerName, cacheName, EnumSet
                        .of(IClusterServices.cacheMode.NON_TRANSACTIONAL));
            }
        } catch (CacheExistException ce) {
            ci
                    .println("\nCache already exits - destroy and recreate if needed");
            return;
        } catch (CacheConfigException cfe) {
            ci.println("\nCache configured with contrasting parameters");
            return;
        }

        if (this.icluster.existCache(containerName, cacheName)) {
            ci.println(cacheName + " has been created on container "
                    + containerName);
        }
    }

    public void _destroy(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        if (this.icluster.existCache(containerName, cacheName)) {
            this.icluster.destroyCache(containerName, cacheName);
            ci.println(cacheName + " has been destroyed");
        }
    }

    public void _listen(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        try {
            this.icluster.addListener(containerName, cacheName,
                    new LoggingListener());
            ci.println("cache " + cacheName + " on container " + containerName
                    + " is begin monitored for updates");
        } catch (CacheListenerAddException clae) {
            ci.println("Couldn't attach the listener to cache " + cacheName
                    + " on container " + containerName);
        }
    }

    public void _unlisten(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }

        Set<IGetUpdates<?, ?>> listeners = this.icluster.getListeners(
                containerName, cacheName);
        for (IGetUpdates<?, ?> l : listeners) {
            this.icluster.removeListener(containerName, cacheName, l);
        }
        ci.println(cacheName + " is no longer being monitored for updates");
    }

    public void _listcaches(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }

        // For user's convenience, let's return the sorted cache list
        List<String> sortedCacheList = new ArrayList<String>(this.icluster
                .getCacheList(containerName));
        java.util.Collections.sort(sortedCacheList);
        for (String cacheName : sortedCacheList) {
            ci.println("\t" + cacheName);
        }
    }

    public void _put(CommandInterpreter ci) {
        ConcurrentMap<Integer, StringContainer> c;
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        String sKey = ci.nextArgument();
        String sValue = ci.nextArgument();
        if (sKey == null) {
            ci.println("Key not supplied");
            return;
        }
        if (sValue == null) {
            ci.println("Value not supplied");
            return;
        }
        Integer key = null;
        try {
            key = Integer.valueOf(sKey);
        } catch (NumberFormatException nfe) {
            ci.println("Key is not a valid integer: " + sKey);
        }

        c = (ConcurrentMap<Integer, StringContainer>) this.icluster.getCache(
                containerName, cacheName);
        if (c != null) {
            ci.println("\nAdd mapping " + key + " = " + sValue);
            c.put(key, new StringContainer(sValue));
        } else {
            ci.println("Cache " + cacheName + " on container " + containerName
                    + " not existant!");
        }
    }

    public void _remove(CommandInterpreter ci) {
        ConcurrentMap<Integer, StringContainer> c;
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        String sKey = ci.nextArgument();
        if (sKey == null) {
            ci.println("Key not supplied");
            return;
        }
        Integer key = null;
        try {
            key = Integer.valueOf(sKey);
        } catch (NumberFormatException nfe) {
            ci.println("Key is not a valid integer: " + sKey);
        }
        c = (ConcurrentMap<Integer, StringContainer>) this.icluster.getCache(
                containerName, cacheName);
        if (c != null) {
            ci.println("\nDelete key " + key);
            c.remove(key);
        } else {
            ci.println("Cache " + cacheName + " on container " + containerName
                    + " not existant!");
        }
    }

    public void _dumper(CommandInterpreter ci) {
        ConcurrentMap c;
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        c = (ConcurrentMap) this.icluster.getCache(containerName, cacheName);
        if (c != null) {
            for (Object e : c.entrySet()) {
                Map.Entry entry = (Map.Entry) e;
                Object v = entry.getValue();
                String res = "<NOT KNOWN>";
                if (v != null) {
                    res = v.toString();
                }
                ci.println("Element " + entry.getKey() + "(hashCode="
                        + entry.getKey().hashCode() + ") has value = (" + res
                        + ")");
            }
        } else {
            ci.println("Cache " + cacheName + " on container " + containerName
                    + " not existant!");
        }
    }

    public void _get(CommandInterpreter ci) {
        ConcurrentMap<Integer, StringContainer> c;
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        String sKey = ci.nextArgument();
        if (sKey == null) {
            ci.println("Key not supplied");
            return;
        }
        Integer key = null;
        try {
            key = Integer.valueOf(sKey);
        } catch (NumberFormatException nfe) {
            ci.println("Key is not a valid integer: " + sKey);
        }
        c = (ConcurrentMap<Integer, StringContainer>) this.icluster.getCache(
                containerName, cacheName);
        if (c != null) {
            ci.println("\nGet key (" + key + ")=(" + c.get(key) + ")");
        } else {
            ci.println("Cache " + cacheName + " on container " + containerName
                    + " not existant!");
        }
    }

    public void _getRole(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String role = "Active";
        if (this.icluster.amIStandby()) {
            role = "Standby";
        }
        ci.println("My role is: " + role);
    }

    public void _getActive(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        ci.println("Current active address is "
                + this.icluster.getActiveAddress());
    }

    public void _listenActive(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        this.doListen = new DoListenRoleChanged();
        try {
            this.icluster.listenRoleChange(this.doListen);
        } catch (ListenRoleChangeAddException e) {
            ci.println("Exception while registering the listener");
            return;
        }
        ci.println("Register listenRoleChanges");
    }

    public void _unlistenActive(CommandInterpreter ci) {
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        if (this.doListen != null) {
            this.icluster.unlistenRoleChange(this.doListen);
            ci.println("Unregistered Active notifications");
        }
    }

    class DoListenRoleChanged implements IListenRoleChange {
        public void newActiveAvailable() {
            logger.debug("New Active is available");
        }
    }

    public void _putComplex(CommandInterpreter ci) {
        ConcurrentMap<StringContainer, ComplexContainer> c;
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        String key = ci.nextArgument();
        if (key == null) {
            ci.println("Key not supplied (String)");
            return;
        }
        String valueIdentity = ci.nextArgument();
        if (valueIdentity == null) {
            ci.println("Value for Identity not supplied (String)");
            return;
        }
        String sValueState = ci.nextArgument();
        if (sValueState == null) {
            ci.println("Value for State not supplied (Integer)");
            return;
        }
        Integer valueState = null;
        try {
            valueState = Integer.valueOf(sValueState);
        } catch (NumberFormatException nfe) {
            ci.println("Value State is not a valid integer: " + sValueState);
            return;
        }
        c = (ConcurrentMap<StringContainer, ComplexContainer>) this.icluster
                .getCache(containerName, cacheName);
        if (c != null) {
            c.put(new StringContainer(key), new ComplexContainer(valueIdentity,
                    valueState));
            ci.println("\nPut in key (" + key + ")={String:" + valueIdentity
                    + ",Integer:" + valueState + "}");
        } else {
            ci.println("Cache " + cacheName + " on container " + containerName
                    + " not existant!");
        }
    }

    public void _updateComplex(CommandInterpreter ci) {
        ConcurrentMap<StringContainer, ComplexContainer> c;
        if (this.icluster == null) {
            ci.println("\nNo Clustering services available");
            return;
        }
        String containerName = ci.nextArgument();
        if (containerName == null) {
            ci.println("containerName not supplied");
            return;
        }
        String cacheName = ci.nextArgument();
        if (cacheName == null) {
            ci.println("Cache not supplied");
            return;
        }
        String key = ci.nextArgument();
        if (key == null) {
            ci.println("Key not supplied (String)");
            return;
        }
        String valueIdentity = ci.nextArgument();
        if (valueIdentity == null) {
            ci.println("Value for Identity not supplied (String)");
            return;
        }
        c = (ConcurrentMap<StringContainer, ComplexContainer>) this.icluster
                .getCache(containerName, cacheName);
        if (c != null) {
            StringContainer k = new StringContainer(key);
            ComplexContainer v = c.get(k);
            if (v != null) {
                v.setIdentity(valueIdentity);
                ci.println("\nUpdate key (" + key + ")={String:"
                        + valueIdentity + "}");

                // IMPORTANT ON UPDATING ANY FIELD OF THE CHILD MAKE
                // SURE TO PUT THE NEW VALUE IN THE CACHE ELSE THE
                // VALUE WILL NOT PROPAGATE!!
                c.put(k, v);
            } else {
                ci.println("\nCannot Update key (" + key
                        + ") doesn't exist in the database");
            }
        } else {
            ci.println("Cache " + cacheName + " on container " + containerName
                    + " not existant!");
        }
    }

    public void setIClusterServices(IClusterServices i) {
        this.icluster = i;
        logger.debug("IClusterServices set");
    }

    public void unsetIClusterServices(IClusterServices i) {
        if (this.icluster == i) {
            this.icluster = null;
            logger.debug("IClusterServices UNset");
        }
    }

    public void startUp() {
        logger.debug("Started clustering test plugin");
    }

    public void shutDown() {
        logger.debug("Stopped clustering test plugin");
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---Clustering Service Testing---\n");
        help.append("\tput              - Put a key,value in the cache\n");
        help.append("\tremove           - Delete a key from the cache\n");
        help.append("\tget              - Get a key from the cache\n");
        help.append("\tdumper           - Dump the cache\n");
        help
                .append("\tcacheinfo        - Dump the configuration for a cache\n");
        help.append("\ttbegin           - Transaction begin\n");
        help.append("\ttcommit          - Transaction Commit\n");
        help.append("\ttrollback        - Transaction Rollback\n");
        help.append("\tlistcaches       - List all the Caches\n");
        help.append("\tlisten           - Listen to cache updates\n");
        help.append("\tunlisten         - UNListen to cache updates\n");
        help.append("\tlistenActive     - Listen to Active updates\n");
        help.append("\tunlistenActive   - UNListen to Active updates\n");
        help.append("\tdestroy          - Destroy a cache\n");
        help.append("\tcreate           - Create a cache\n");
        help.append("\tgetRole          - Tell if active or standby\n");
        help.append("\tgetActive        - Report the IP address of Active\n");
        help
                .append("\tputComplex       - Fill a more complex data structure\n");
        help
                .append("\tupdateComplex    - Update the value of a more complex data structure\n");
        help
                .append("\tgetLogLevel      - Get the loglevel for the logger specified\n");
        help
                .append("\tsetLogLevel      - Set the loglevel for the logger specified\n");
        return help.toString();
    }
}
