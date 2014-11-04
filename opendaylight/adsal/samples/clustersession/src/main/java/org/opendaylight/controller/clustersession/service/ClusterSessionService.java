package org.opendaylight.controller.clustersession.service;

import java.util.HashMap;

import org.apache.catalina.Session;
import org.apache.catalina.util.SessionIdGenerator;
import org.opendaylight.controller.clustersession.ClusterSession;

/**
 * A service to handle session persistence and retrieval in any data store
 *
 * @author harman singh
 *
 */
public interface ClusterSessionService {

  /**
   * This method performs all startup operations
   */
  void startInternal(SessionIdGenerator sessionIdGenerator);

  /**
   * Method to perform all clean up operations
   */
  void stopInternal();

  /**
   * Find Session object based on provided session id from persistance
   * @param id
   * @return an instance of Session
   */
  Session findSession(final String id);

  /**
   * Get an array of session objects available in storage
   */
  Session[] findSessions();

  /**
   * Remove a session object from persistence
   * @param id of session object need to be removed
   */
  void removeSession(final String id);

  /**
   * Expire and remove a session object from persistence
   * @param id of session object need to be expired
   */
  void expireSession(final String id);

  /**
   * Create a session object based on session id, if session is not present
   * use random session id
   * @param sessionId
   * @return an instance of Session
   */
  Session createSession(final String sessionId);

  /**
   * Add a session object in persistence
   * @param session an instance of ClusterSession
   */
  void addSession(final ClusterSession session);

  /**
   * Create an empty Session object
   * @return session object
   */
  Session createEmptySession();

  /**
   * Fetch attributes of Session object fetched by supplied session id
   * @param sessionId
   * @return
   */
  HashMap<String, String> getSession(String sessionId);

  /**
   * update the session object in persistence
   * @param session
   */
  void updateSession(final ClusterSession session);

}
