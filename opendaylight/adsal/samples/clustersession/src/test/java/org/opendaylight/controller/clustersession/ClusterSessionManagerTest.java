package org.opendaylight.controller.clustersession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Session;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.clustersession.impl.ClusterSessionServiceImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FrameworkUtil.class})
public class ClusterSessionManagerTest {
  static ClusterSessionManager manager = null;
  static ClusterSessionServiceImpl sessionService = null;
  private static final String SESSION_CACHE = "customSessionManager.sessionData";
  static ConcurrentMap<String, ClusterSessionData> sessions = new ConcurrentHashMap<String, ClusterSessionData>();
  private String sessionId = "1234567";
  final String AUTH_TYPE = "FORM";
  final String ATTRIBUTE_NAME = "AuthType";
  final int SESSION_ID_LENGTH = 7;
  @SuppressWarnings("unchecked")
  @BeforeClass
  public static void init(){
    Bundle bundle = mock(Bundle.class);
    BundleContext context = mock(BundleContext.class);
    IClusterGlobalServices clusterGlobalService = mock(IClusterGlobalServices.class);
    ServiceReference<IClusterGlobalServices> serviceReference = mock(ServiceReference.class);
    PowerMockito.mockStatic(FrameworkUtil.class);
    when(FrameworkUtil.getBundle(ClusterSessionManager.class)).thenReturn(bundle);
    when(bundle.getBundleContext()).thenReturn(context);
    when(context.getService(serviceReference)).thenReturn(clusterGlobalService);
    when((ConcurrentMap<String, ClusterSessionData>)clusterGlobalService.getCache(SESSION_CACHE)).thenReturn(sessions);
    Context containerContext = mock(Context.class);
    manager = new ClusterSessionManager();
    manager.setContainer(containerContext);
    try {
      manager.startInternal();
    } catch (LifecycleException e) {
    }
    sessionService = (ClusterSessionServiceImpl) manager.getSessionService();
    sessionService.addingService(serviceReference);
  }

  @Test
  public void checkSessionManagerCreated(){
    assertEquals("session manager info does not match", "ClusterSessionManager/1.0", manager.getInfo());
    assertEquals("session manager name does not match", "ClusterSessionManager", manager.getName());
  }

  @Test
  public void testCreateEmptySession(){
    Session session = manager.createEmptySession();
    assertEquals("session manager does not match", manager, session.getManager());
  }

  @Test
  public void testCreateRandomSessionId(){
    Session session = manager.createSession(null);
    assertEquals("Session should be valid", true, session.isValid());
    manager.remove(session);
  }

  @Test
  public void testCreateSession(){
    Session session = manager.createSession(sessionId);
    assertEquals("Session should be valid", true, session.isValid());
    assertEquals("Session id does not match", sessionId, session.getId());
    manager.remove(session);
  }

  @Test
  public void testReCreateSession(){
    Session session = manager.createSession(sessionId);
    assertEquals("Session should be valid", true, session.isValid());
    assertEquals("Session id does not match", sessionId, session.getId());
    manager.createSession(sessionId);
    manager.remove(session);
  }

  @Test
  public void testSessionCRUD() throws IOException{
    Session foundSession = manager.findSession(sessionId);
    assertNull("Session should not exist here", foundSession);
    Session session = manager.createSession(sessionId);
    manager.add(session);
    foundSession = manager.findSession(sessionId);
    assertEquals("Session was not found, id does not match", sessionId, foundSession.getId());
    manager.remove(session);
    foundSession = manager.findSession(sessionId);
    assertEquals("Session was not removed", null, foundSession);
  }

  @Test
  public void testExpireSession() throws IOException{
    Session session = manager.createSession(sessionId);
    session.setAuthType(AUTH_TYPE);
    manager.add(session);
    Session foundSession = manager.findSession(sessionId);
    assertEquals("Session was not found", sessionId, foundSession.getId());
    manager.expireSession(sessionId);
    foundSession = manager.findSession(sessionId);
    assertEquals("Session was not expired", null, foundSession);
  }

  @Test
  public void testFindSessions(){
    Session session = manager.createSession(sessionId);
    session.setAuthType(AUTH_TYPE);
    manager.add(session);
    Session[] sessions = manager.findSessions();
    assertEquals("Session array size does not match", 1, sessions.length);
    assertEquals("Session array size does not match", sessionId, sessions[0].getId());
    manager.remove(session);
  }

  @Test
  public void testGetSession(){
    ClusterSession session = (ClusterSession) manager.createSession(sessionId);
    session.setAttribute(ATTRIBUTE_NAME, AUTH_TYPE);
    manager.add(session);
    HashMap<String, String> sessionAttributes = manager.getSession(sessionId);
    assertNotNull("Session attribute should not be null", sessionAttributes);
    assertEquals("Session attribute size does not match", 1, sessionAttributes.size());
    assertEquals("Session attribute size does not match", AUTH_TYPE, sessionAttributes.get(ATTRIBUTE_NAME));
    manager.remove(session);
  }

  @AfterClass
  public static void cleanup(){
    try {
      manager.stopInternal();
    } catch (LifecycleException e) {
    }
  }

}