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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
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

public class SanityIT {
  private static final int OF_PORT = 6633;
  private static final int HTTP_PORT = 8080;
  private static final int JMX_PORT = 1088;
  private static final String CTRL_HOME = System.getProperty("ctrl.home");
  private static final long TIMEOUT_MILLIS =
          Integer.getInteger("sanitytest.timeout.secs", 3*60) * 1000L;
  private static final String JMX_URL =
          "service:jmx:rmi:///jndi/rmi://localhost:" + JMX_PORT + "/jmxrmi";

  private static final Set<String> bundles =
          Collections.synchronizedSet(new HashSet<String>());
  private static final Set<String> fragments =
          Collections.synchronizedSet(new HashSet<String>());

  private static volatile AtomicLong lastNotification = new AtomicLong(-1L);

  @BeforeClass
  public static void loadBundles() throws IOException {
      Assert.assertNotNull(CTRL_HOME);
      File ctrlHome = new File(CTRL_HOME);
      log("Processing jars in installalation: " + ctrlHome);
      if (!ctrlHome.exists() || !ctrlHome.isDirectory()) {
          throw new FileNotFoundException("Cannot determine controller home: "
                  + ctrlHome);
      }
      processBundles(new File(ctrlHome, "lib"));
      processBundles(new File(ctrlHome, "plugins"));
      log("Bundles found in installation:   " + bundles.size());
      log("Fragments found in installation: " + fragments.size());
  }

  @Test
  public void sanityTest() throws Exception {
    // wait for http, jmx & of ports to open
    long timeElapsedMillis = waitForListening(OF_PORT, false, 0L);
    timeElapsedMillis += waitForListening(JMX_PORT, false, timeElapsedMillis);
    timeElapsedMillis += waitForListening(HTTP_PORT, false, timeElapsedMillis);

    // open jmx connection
    JMXServiceURL serviceUrl = new JMXServiceURL(JMX_URL);
    JMXConnector jmxConnector = JMXConnectorFactory.connect(serviceUrl, null);
    MBeanServerConnection conn = jmxConnector.getMBeanServerConnection();
    ObjectName fmkName = new ObjectName("org.ow2.chameleon:type=framework");
    OSGiPlatformMXBean fmkBean= JMX.newMBeanProxy(conn, fmkName,
              OSGiPlatformMXBean.class, true);
    // add a notification listener to capture bundle events.
    conn.addNotificationListener(fmkName, new NotificationListener() {
        @Override
        public synchronized void handleNotification(Notification n, Object handback) {
            // record the notification time
            lastNotification.set(System.currentTimeMillis());
            /*
            p("Notification: message: " + n.getMessage());
            p("Notification: type: " + n.getType());
            p("Notification: source: " + n.getSource());
            p("Notification: userdata: " + n.getUserData());
            p("Notification: ts: " + n.getTimeStamp());
            */
            BundleMXBean bundleBean = (BundleMXBean) handback;
            handleBundleEvent(bundleBean);
        }
      }, null, null);

    // wait till the notifications die down or all bundles reach expected state
    log("Waiting for bundle notifications to complete ...");
    while (bundles.size() > 0 || fragments.size() > 0) {
        timeElapsedMillis += 1000L;
        Thread.sleep(1000);
        long lat = lastNotification.get();
        // If we dont see a notification in 15secs, we can move ahead
        if (lat > -1 && System.currentTimeMillis() - lat > 15000L) {
            log("Bundle notifications complete.");
            break;
        }

        if (timeElapsedMillis > TIMEOUT_MILLIS) {
            Assert.fail("TIMEOUT waiting for system to reach expected state.");
        }
    }

    // process all bundles
    ObjectName allBundlesName = new ObjectName("org.ow2.chameleon:*");
    Set<ObjectName> bundleNames = conn.queryNames(allBundlesName, null);
    for (ObjectName bundleName : bundleNames) {
        if ("bundle".equals(bundleName.getKeyProperty("type"))) {
            BundleMXBean bundleBean = JMX.newMBeanProxy(conn, bundleName,
                    BundleMXBean.class, true);
            handleBundleEvent(bundleBean);
        } else {
            // for framework type. Ignored for now.
        }
    }
    if (bundles.size() == 0 && fragments.size() == 0) {
        log("All bundles which have reached expected state.");
    } else {
        log("Some bundles have not reached expected state: ");
        for (String s : bundles) log("Bundle: " + s);
        for (String s : fragments) log("Fragment: " + s);

    }
    Assert.assertTrue(bundles.size() == 0 && fragments.size() == 0);
  }

  private static String stateToString(int state) {
      switch (state) {
      case Bundle.ACTIVE:
          return "ACTIVE";
      case Bundle.INSTALLED:
          return "INSTALLED";
      case Bundle.RESOLVED:
          return "RESOLVED";
      case Bundle.UNINSTALLED:
          return "UNINSTALLED";
      case Bundle.STARTING:
          return "STARTING";
      case Bundle.STOPPING:
          return "STOPPING";
      default:
          return "Not CONVERTED: state value is " + state;
      }
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
      /*
      p("+++++++++++++ " + name + " ---- " + stateToString(state) + " " +
              bundles.size() + "|" + fragments.size());
              */
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



    public static long waitForListening(int port, boolean isHTTP, long timeElapsedMillis)
            throws InterruptedException {
        long sleepTimeMillis = 500L; // 0.5 secs

        while (timeElapsedMillis < TIMEOUT_MILLIS) {
            long timeRemaining = TIMEOUT_MILLIS - timeElapsedMillis;
            sleepTimeMillis *= 2;
            long toSleep = (sleepTimeMillis > timeRemaining)
                    ? timeRemaining : sleepTimeMillis;
            Thread.sleep(toSleep);
            timeElapsedMillis += toSleep;
            if (isHTTP ? connectHTTP(port) : connectTCP(port)) {
              log("Port is open: " + port);
              return timeElapsedMillis;
            }
        }
        throw new IllegalStateException("Timeout waiting for port: " + port);
    }

    private static void log(String msg) {
        System.out.println("[SanityTest] " + msg);
    }

    public static boolean connectTCP(int port) {
      try {
        Socket sock = new Socket((String)null, port);
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
        try {
            URL url = new URL("http", "localhost", port, "/");
            HttpURLConnection con;
            con = (HttpURLConnection) url.openConnection();
            return (con.getResponseCode() > 0);
        } catch (IOException e) {
            return false;
        }
    }

}

