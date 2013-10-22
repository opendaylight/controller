/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.distribution.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.ow2.chameleon.management.beans.BundleMXBean;
import org.ow2.chameleon.management.beans.OSGiPlatformMXBean;

/**
 * This integration test assumes a running local controller. The test does the
 * following:
 * 1) Wait for HTTP, JMX and OF ports to open
 * 2) Establishes a JMX connection and registers a bundle notification listener
 * 3) Waits till all bundles reach expected states or the timeout period elapses
 */
public class SanityIT {
  private static final int OF_PORT = Integer.getInteger("ctrl.of.port", 6633);
  private static final int HTTP_PORT = Integer.getInteger("ctrl.http.port", 8080);
  private static final int JMX_PORT = Integer.getInteger("ctrl.jmx.port", 1088);
  private static final int RMI_PORT = Integer.getInteger("ctrl.rmi.port", 1099);
  private static final String CTRL_HOST = System.getProperty("ctrl.host", "127.0.0.1");
  private static final String CTRL_HOME = System.getProperty("ctrl.home");
  private static final long TIMEOUT_MILLIS =
          Integer.getInteger("ctrl.start.timeout", 3*60) * 1000L;
  private static final String JMX_URL =
          "service:jmx:rmi:///jndi/rmi://" + CTRL_HOST + ":" + JMX_PORT + "/jmxrmi";

  private static final Set<String> bundles =
          Collections.synchronizedSet(new HashSet<String>());
  private static final Set<String> fragments =
          Collections.synchronizedSet(new HashSet<String>());

  @BeforeClass
  public static void loadBundles() throws IOException {
      log("         ctrl.home: " + CTRL_HOME);
      log("         ctrl.host: " + CTRL_HOST);
      log("ctrl.start.timeout: " + TIMEOUT_MILLIS);
      log("           jmx.url: " + JMX_URL);

      Assert.assertNotNull(CTRL_HOME);
      File ctrlHome = new File(CTRL_HOME);
      Assert.assertTrue(ctrlHome.exists() && ctrlHome.isDirectory());
      processBundles(new File(ctrlHome, "lib"));
      processBundles(new File(ctrlHome, "plugins"));
      log("Bundles found in installation:   " + bundles.size());
      log("Fragments found in installation: " + fragments.size());
  }

  @Test
  public void sanityTest() throws Exception {
    // wait for http, jmx & of ports to open
    long startTime = System.currentTimeMillis();
    waitForListening(OF_PORT, false, startTime);
    waitForListening(JMX_PORT, false, startTime);
    waitForListening(HTTP_PORT, false, startTime);

    // open jmx connection
    JMXServiceURL serviceUrl = new JMXServiceURL(JMX_URL);
    JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
    final MBeanServerConnection conn = jmxConnector.getMBeanServerConnection();

    ObjectName fmkName = new ObjectName("org.ow2.chameleon:type=framework");
    OSGiPlatformMXBean fmkBean= JMX.newMBeanProxy(conn, fmkName,
              OSGiPlatformMXBean.class, true);
    conn.addNotificationListener(fmkName, new NotificationListener() {

        @Override
        public void handleNotification(Notification n, Object handback) {
            try {
                //log("Notification: source: " + n.getSource());
                ObjectName bundleName = new ObjectName(
                        "org.ow2.chameleon:type=bundle,id=" + n.getUserData());
                BundleMXBean bundleBean = JMX.newMXBeanProxy(conn,
                        bundleName, BundleMXBean.class, true);
                //log("Bundle state change: " + bundleBean.getSymbolicName() +
                //        " : " + stateToString(bundleBean.getState()));
                handleBundleEvent(bundleBean);
                // if its a system bundle, notify the main thread
                if (bundleBean.getBundleId() == 0 &&
                        bundleBean.getState() == Bundle.ACTIVE)
                {
                    synchronized(SanityIT.this) {
                        SanityIT.this.notify();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }, null, null);

    log("Waiting for system bundle to start... (times out in: " + TIMEOUT_MILLIS + "ms)");
    long timeElapsed = System.currentTimeMillis() - startTime;
    synchronized(this) {
       this.wait(TIMEOUT_MILLIS - timeElapsed);
    }
    log("System bundle started. Revalidating bundle states for all bundles...");

    // Sometimes, the system bundle starts earlier than other bundles(?). The
    // following loop will cover that case.
    do {
        Thread.sleep(2000); // 2s seems appropriate given the default timeout
        ObjectName allBundlesName = new ObjectName("org.ow2.chameleon:*");
        Set<ObjectName> bundleNames = conn.queryNames(allBundlesName, null);
        for (ObjectName bundleName : bundleNames) {
            if ("bundle".equals(bundleName.getKeyProperty("type"))) {
                BundleMXBean bundleBean = JMX.newMBeanProxy(conn, bundleName,
                        BundleMXBean.class, true);
                handleBundleEvent(bundleBean);
            }
        }
        int remainingBundles = bundles.size() + fragments.size();
        if (remainingBundles == 0) {
            break;
        }
        log("Bundles not in expected states: " + remainingBundles + " Waiting...");
    } while(System.currentTimeMillis() - startTime < TIMEOUT_MILLIS);
    try {
        jmxConnector.close();
    } catch (Exception ignore) {
        // dont want the tests to fail in case we can't close jmx connection
        ignore.printStackTrace();
    }
    if (bundles.size() + fragments.size() == 0) {
        log("All bundles have reached expected state");
    } else {
        log("Timeout waiting for bundles to reach expected state: ");
        for (String s : bundles) log("Bundle: " + s);
        for (String s : fragments) log("Fragment: " + s);

    }
    Assert.assertTrue(bundles.size() == 0 && fragments.size() == 0);
  }

  private synchronized static void handleBundleEvent(BundleMXBean bundleBean) {
      String name = bundleBean.getSymbolicName();
      int state = bundleBean.getState();
      // BUG in BundleMXBeanImpl - can't get bundle headers :(
      // String fragHost = bundleBean.getBundleHeaders().get(Constants.FRAGMENT_HOST);
      if (bundles.contains(name) && (state == Bundle.RESOLVED || state == Bundle.ACTIVE)) {
          bundles.remove(name);
      } else if (fragments.contains(name) && state == Bundle.RESOLVED) {
          fragments.remove(name);
      }
  }


  public static void processBundles(File dir) throws IOException {
      if (!dir.exists()) throw new FileNotFoundException("Cannot find dir:" + dir);
      dir.listFiles(new FileFilter() {

        @Override
        public boolean accept(File file) {
            //p("---- " + file);
            if (file.getName().endsWith(".jar")) {
                try {
                    JarFile jar = new JarFile(file, false);
                    Attributes jarAttributes = jar.getManifest().getMainAttributes();
                    String bundleName = jarAttributes.getValue(Constants.BUNDLE_SYMBOLICNAME);
                    String fragHost = jarAttributes.getValue(Constants.FRAGMENT_HOST);
                    if (bundleName == null) {
                        log("Found a non bundle file:" + file);
                        return false;
                    } else {
                        int idx = bundleName.indexOf(';');
                        if (idx > -1) {
                            bundleName = bundleName.substring(0, idx);
                        }
                    }
                    if (bundleName.equals("org.eclipse.equinox.launcher")) {
                        // launcher jar is not a bundle
                        return false;
                    }

                    if (fragHost == null) {
                        if (!bundles.add(bundleName)) {
                            throw new IllegalStateException(
                                    "Found duplicate bundles with same symbolic name: "
                                            + bundleName);
                        }
                    } else {
                        //p(">>> " + fragHost);
                        // fragments attaching to framework can't be detected
                        if (fragHost.contains("extension:=\"framework\"")) return false;
                        if (!fragments.add(bundleName)) {
                            throw new IllegalStateException(
                                    "Found duplicate fragments with same symbolic name: "
                                            + bundleName);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Error processing bundles", e);
                }
            }
            return false;
        }
      });
  }

    public static long waitForListening(int port, boolean isHTTP, long beginTime)
            throws InterruptedException {
        long timeElapsedMillis = System.currentTimeMillis() - beginTime;
        long sleepTimeMillis = 500L; // 0.5 secs

        while (timeElapsedMillis < TIMEOUT_MILLIS) {
            long timeRemaining = TIMEOUT_MILLIS - timeElapsedMillis;
            sleepTimeMillis *= 2; // exponential backoff
            long toSleep = (sleepTimeMillis > timeRemaining)
                    ? timeRemaining : sleepTimeMillis;
            Thread.sleep(toSleep);
            timeElapsedMillis = System.currentTimeMillis() - beginTime;
            if (isHTTP ? connectHTTP(port) : connectTCP(port)) {
              log("Port is open: " + port);
              return timeElapsedMillis;
            }
        }
        throw new IllegalStateException("Timeout waiting for port: " + port);
    }

    private static void log(String msg) {
        System.out.format("[SanityIT] [%s] %s %s", new Date().toString(), msg,
                System.lineSeparator());
    }

    public static boolean connectTCP(int port) {
      String host = CTRL_HOST.length() == 0 ? null : CTRL_HOST;
      try {
        Socket sock = new Socket(host, port);
        sock.getPort();
        try {
          sock.close();
        } catch (IOException ignore) {
          // Can't close socket. Ingore and let downstream validate health
        }
        return true;
      } catch (IOException ioe) {
        return false;
      }
    }

    public static boolean connectHTTP(int port) {
        String host = CTRL_HOST.length() == 0 ? "localhost" : CTRL_HOST;
        try {
            URL url = new URL("http", host, port, "/");
            HttpURLConnection con;
            con = (HttpURLConnection) url.openConnection();
            return (con.getResponseCode() > 0);
        } catch (IOException e) {
            return false;
        }
    }

    private String stateToString(int state) {
        switch (state) {
            case Bundle.ACTIVE: return "ACTIVE";
            case Bundle.INSTALLED: return "INSTALLED";
            case Bundle.RESOLVED: return "RESOLVED";
            case Bundle.UNINSTALLED: return "UNINSTALLED";
            case Bundle.STARTING: return "STARTING";
            case Bundle.STOPPING: return "STOPPING";
            default: return "UNKNOWN: " + state;
        }
    }




}
