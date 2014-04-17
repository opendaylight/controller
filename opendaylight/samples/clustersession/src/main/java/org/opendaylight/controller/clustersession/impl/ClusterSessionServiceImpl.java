/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 */
package org.opendaylight.controller.clustersession.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.catalina.Session;
import org.apache.catalina.util.SessionIdGenerator;
import org.opendaylight.controller.clustering.services.CacheConfigException;
import org.opendaylight.controller.clustering.services.CacheExistException;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustering.services.IClusterServices;
import org.opendaylight.controller.clustersession.ClusterSession;
import org.opendaylight.controller.clustersession.ClusterSessionData;
import org.opendaylight.controller.clustersession.ClusterSessionManager;
import org.opendaylight.controller.clustersession.ClusterSessionUtil;
import org.opendaylight.controller.clustersession.service.ClusterSessionService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation to persist and retrieve session data from infinispan cache
 * @author harman singh
 *
 */
public class ClusterSessionServiceImpl implements ClusterSessionService,
  ServiceTrackerCustomizer<IClusterGlobalServices, IClusterGlobalServices>{

  private IClusterGlobalServices clusterGlobalServices = null;
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSessionServiceImpl.class);
  private ConcurrentMap<String, ClusterSessionData> sessions = null;
  private static final String SESSION_CACHE = "customSessionManager.sessionData";
  private ClusterSessionManager manager = null;
  private SessionIdGenerator sessionIdGenerator = null;
  private BundleContext context = null;
  private ServiceTracker<IClusterGlobalServices, IClusterGlobalServices> clusterTracker;
  public ClusterSessionServiceImpl(ClusterSessionManager manager) {
    this.manager = manager;
  }
  /**
   * This method initialize the cluster service of opendaylight and
   * create a cache map in infinispan
   */

  @Override
  public void startInternal(SessionIdGenerator sessionIdGenerator){
    this.sessionIdGenerator = sessionIdGenerator;
    context = FrameworkUtil.getBundle(ClusterSessionManager.class).getBundleContext();
    getClusterService();
    createCache();
  }

  /**
   * Removes the cluster service tracker while shut down
   */
  @Override
  public void stopInternal(){
    if(clusterTracker != null){
      clusterTracker.close();
    }
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public Session findSession(final String id){
    if(id == null) {
      return null;
    }
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return null;
    }
    ClusterSessionData sessionData = sessions.get(id);
    if(sessionData != null) {
      LOGGER.debug("SESSION FOUND : ", id);
    } else {
      LOGGER.debug("SESSION NOTFOUND : ", id);
    }
    return ClusterSessionUtil.getDeserializedSession(sessionData, this, this.manager);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session[] findSessions() {
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return new Session[0];
    }
    Collection<ClusterSessionData> sessionDataList = sessions.values();
    ArrayList<ClusterSession> sessionList = new ArrayList<ClusterSession>();
    for(ClusterSessionData sessionData : sessionDataList){
      sessionList.add(ClusterSessionUtil.getDeserializedSession(sessionData, this, this.manager));
    }
    return sessionList.toArray(new Session[0]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeSession(final String id){
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return;
    }
    sessions.remove(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void expireSession(final String id){
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return;
    }
    ClusterSessionData sessionData = sessions.get(id);
    if(sessionData != null) {
      sessionData.getSession().expire();
      removeSession(id);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session createSession(final String sessionId){
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return null;
    }
    Session session = createEmptySession();
    session.setNew(true);
    session.setValid(true);
    session.setCreationTime(System.currentTimeMillis());
    String id = sessionId;
    if (id == null) {
      id = generateSessionId();
    }
    session.setId(id);
    return session;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addSession(final ClusterSession session){
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return;
    }
    ClusterSessionData sessionData = ClusterSessionUtil.getSerializableSession(session);
    sessions.put(session.getId(), sessionData);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session createEmptySession(){
    return getNewSession();
  }

  /**
   * Returns information about the session with the given session id.
   *
   * <p>The session information is organized as a HashMap, mapping
   * session attribute names to the String representation of their values.
   *
   * @param sessionId Session id
   *
   * @return HashMap mapping session attribute names to the String
   * representation of their values, or null if no session with the
   * specified id exists, or if the session does not have any attributes
   */
  public HashMap<String, String> getSession(String sessionId) {
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return null;
    }
    ClusterSessionData sessionData = sessions.get(sessionId);
    if (sessionData == null) {
      return null;
    }
    ClusterSession s = ClusterSessionUtil.getDeserializedSession(sessionData, this, this.manager);
    Enumeration<String> ee = s.getAttributeNames();
    if (ee == null || !ee.hasMoreElements()) {
      return null;
    }
    HashMap<String, String> map = new HashMap<String, String>();
    while (ee.hasMoreElements()) {
      String attrName = ee.nextElement();
      map.put(attrName, s.getAttribute(attrName).toString());
    }
    return map;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateSession(ClusterSession session) {
    if(sessions == null) {
      LOGGER.debug("Session cache not present, try to create.");
      createCache();
      return;
    }
    if(session.getId() != null && sessions.get(session.getId()) != null){
      ClusterSessionData sessionData = ClusterSessionUtil.getSerializableSession(session);
      sessions.put(session.getId(), sessionData);
    }
  }

  @Override
  public IClusterGlobalServices addingService(ServiceReference<IClusterGlobalServices> reference) {
      if (clusterGlobalServices == null) {
        this.clusterGlobalServices = context.getService(reference);
        createCache();
        return clusterGlobalServices;
      }
      return null;
  }

  @Override
  public void modifiedService(ServiceReference<IClusterGlobalServices> reference, IClusterGlobalServices service) {
    // This method is added from ServiceTracker interface, We don't have to modify service.
  }

  @Override
  public void removedService(ServiceReference<IClusterGlobalServices> reference, IClusterGlobalServices service) {
      if (clusterGlobalServices == service) {
          clusterGlobalServices = null;
      }
  }

  /*
   * Return an instance of Standard Session object with current session manager
   */
  private ClusterSession getNewSession() {
    return new ClusterSession(this.manager, this);
  }

  /*
   * Generate and return a new session identifier.
   */
  private String generateSessionId() {
    String result = null;
    do {
      result = sessionIdGenerator.generateSessionId();
    } while (sessions.containsKey(result));
    return result;
  }

  private void createCache() {
    allocateCache();
    retrieveCache();
  }

  /*
   * This is a fragment bundle, so We can't use Activator to set Service.
   * This is the alternative to get registered clustered service
   */
  private void getClusterService(){
    if (context != null) {
      clusterTracker = new ServiceTracker<>(context, IClusterGlobalServices.class, this);
      clusterTracker.open();
    }
  }

  /*
   * Allocate space in infinispan to persist session data
   */
  private void allocateCache() {
    if (clusterGlobalServices == null) {
      LOGGER.trace("un-initialized clusterGlobalService, can't create cache");
      return;
    }
    try {
      clusterGlobalServices.createCache(SESSION_CACHE,
          EnumSet.of(IClusterServices.cacheMode.SYNC , IClusterServices.cacheMode.TRANSACTIONAL));

    } catch (CacheConfigException cce) {
      LOGGER.error("Cache configuration invalid - check cache mode", cce.toString());
    } catch (CacheExistException ce) {
      LOGGER.debug("Skipping cache creation as already present", ce.toString());
    }
  }

  /*
   * Fetch cached session data map object from infinispan
   */
  @SuppressWarnings("unchecked")
  private void retrieveCache(){
    if (clusterGlobalServices == null) {
      LOGGER.trace("un-initialized clusterGlobalService, can't retrieve cache");
      return;
    }
    sessions = (ConcurrentMap<String, ClusterSessionData>)clusterGlobalServices.getCache(SESSION_CACHE);
    if(sessions == null){
      LOGGER.warn("Failed to get session cache");
    }
  }
}