/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 */
package org.opendaylight.controller.clustersession;

import java.io.IOException;
import java.util.HashMap;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.Session;
import org.apache.catalina.session.ManagerBase;
import org.apache.catalina.util.SessionIdGenerator;
import org.opendaylight.controller.clustersession.impl.ClusterSessionServiceImpl;
import org.opendaylight.controller.clustersession.service.ClusterSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * ClusterSession Manager is a custom session manager, that is used to persist session data
 * across cluster of a storage such as infinispan or memcache
 * @author harman singh
 *
 */
public class ClusterSessionManager extends ManagerBase{
  /**
   * Has this component been _started yet?
   */
  protected boolean started = false;

  protected ClusterSessionService sessionService;

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSessionManager.class);
  /**
   * The descriptive information about this implementation.
   */
  protected static final String INFO = "ClusterSessionManager/1.0";

  /**
   * The descriptive name of this Manager implementation (for logging).
   */
  protected static final String NAME = "ClusterSessionManager";

  public ClusterSessionManager(){
    sessionService = new ClusterSessionServiceImpl(this);
  }

  /**
   * Return descriptive information about this Manager implementation and
   * the corresponding version number, in the format
   * <code>&lt;description&gt;/&lt;version&gt;</code>.
   */
  @Override
  public String getInfo(){
    return INFO;
  }

  /**
   * Return the descriptive short name of this Manager implementation.
   */
  @Override
  public String getName(){
    return NAME;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void load() throws ClassNotFoundException, IOException {
    // We are not persisting any session in database, infinispan does not persist data.
    // loading of persisted session is not required.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unload() throws IOException {
    // We are not persisting any session in database, infinispan does not persist data.
    // unloading of session to persistence layer is not required.
  }

  /**
   * Start this component and implement the requirements
   * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
   *
   * @exception LifecycleException if this component detects a fatal error
   *  that prevents this component from being used
   */
  @Override
  protected synchronized void startInternal() throws LifecycleException {
    sessionIdGenerator = new SessionIdGenerator();
    sessionIdGenerator.setJvmRoute(getJvmRoute());
    sessionIdGenerator.setSecureRandomAlgorithm(getSecureRandomAlgorithm());
    sessionIdGenerator.setSecureRandomClass(getSecureRandomClass());
    sessionIdGenerator.setSecureRandomProvider(getSecureRandomProvider());
    sessionIdGenerator.setSessionIdLength(getSessionIdLength());
    sessionService.startInternal(sessionIdGenerator);
    setState(LifecycleState.STARTING);
  }

  /**
   * Stop this component and implement the requirements
   * of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
   *
   * @exception LifecycleException if this component detects a fatal error
   *  that prevents this component from being used
   */
  @Override
  protected synchronized void stopInternal() throws LifecycleException {
    setState(LifecycleState.STOPPING);

    // Expire all active sessions
    Session sessions[] = findSessions();
    for (int i = 0; i < sessions.length; i++) {
      Session session = sessions[i];
      try {
        if (session.isValid()) {
          session.expire();
        }
      } catch (Exception e) {
        LOGGER.warn(e.toString());
      } finally {
        // Measure against memory leaking if references to the session
        // object are kept in a shared field somewhere
        session.recycle();
      }
    }
    // Require a new random number generator if we are restarted
    super.stopInternal();
    sessionService.stopInternal();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void expireSession(final String sessionId){
    LOGGER.debug("SESSION EXPIRE : ", sessionId);
    sessionService.expireSession(sessionId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(final Session session){
    LOGGER.debug("SESSION REMOVE : ", session.getId());
    sessionService.removeSession(session.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(Session session, boolean update) {
    sessionService.removeSession(session.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session findSession(final String id) throws IOException{
    return sessionService.findSession(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session createSession(final String sessionId){
    LOGGER.debug("SESSION CREATE : ", sessionId);
    if(sessionId != null){
      Session session = sessionService.findSession(sessionId);
      if(session != null){
        return session;
      }
    }
    return sessionService.createSession(sessionId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Session createEmptySession(){
    return sessionService.createEmptySession();
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public void add(Session session){
    LOGGER.debug("SESSION ADD : ", session.getId());
    sessionService.addSession((ClusterSession)session);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public HashMap<String, String> getSession(String sessionId){
    return sessionService.getSession(sessionId);
  }
  /**
   * {@inheritDoc}
   */
  @Override
  public Session[] findSessions() {
    return sessionService.findSessions();
  }

  public ClusterSessionService getSessionService() {
    return sessionService;
  }

  public void setSessionService(ClusterSessionService sessionService) {
    this.sessionService = sessionService;
  }

}
