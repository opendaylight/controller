package org.opendaylight.controller.clustersession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.catalina.Context;
import org.apache.catalina.Session;
import org.apache.catalina.util.SessionIdGenerator;
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
public class ClusterSessionServiceImplTest {
  static ClusterSessionManager manager = null;
  static ClusterSessionServiceImpl sessionService = null;
  private static final String SESSION_CACHE = "customSessionManager.sessionData";
  static ConcurrentMap<String, ClusterSessionData> sessions = new ConcurrentHashMap<String, ClusterSessionData>();
  private String sessionId = "1234567";
  final String AUTH_TYPE = "FORM";
  final String ATTRIBUTE_NAME = "AuthType";

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
    sessionService = (ClusterSessionServiceImpl) manager.getSessionService();
    manager.setContainer(containerContext);
    sessionService.startInternal(new SessionIdGenerator());
    sessionService.addingService(serviceReference);
  }

  @Test
  public void testCreateEmptySession(){
    Session session = sessionService.createEmptySession();
    assertEquals("session manager does not match", manager, session.getManager());
  }

  @Test
  public void testCreateSessionwithRandomId(){
    Session session = sessionService.createSession(null);
    assertEquals("Session should be valid", true, session.isValid());
    sessionService.removeSession(session.getId());
  }

  @Test
  public void testCreateSession(){
    Session session = sessionService.createSession(sessionId);
    assertEquals("Session should be valid", true, session.isValid());
    assertEquals("Session id does not match", sessionId, session.getId());
    sessionService.removeSession(sessionId);
  }

  @Test
  public void testNullfindSession() {
    Session session = sessionService.findSession(null);
    assertNull("Session should be null", session);
  }

  @Test
  public void testSessionCRUD(){
    Session foundSession = sessionService.findSession(sessionId);
    assertNull("Session should not exist here", foundSession);
    Session session = sessionService.createSession(sessionId);
    foundSession = sessionService.findSession(sessionId);
    assertEquals("Session was not added", sessionId, foundSession.getId());
    session.setAuthType(AUTH_TYPE);
    sessionService.updateSession((ClusterSession)session);
    foundSession = sessionService.findSession(sessionId);
    assertEquals("Session was not found, id does not match", sessionId, foundSession.getId());
    assertEquals("Session was not found, auth type does match", AUTH_TYPE, foundSession.getAuthType());
    sessionService.removeSession(sessionId);
    foundSession = sessionService.findSession(sessionId);
    assertEquals("Session was not removed", null, foundSession);
  }

  @Test
  public void testExpireSession(){
    Session session = sessionService.createSession(sessionId);
    session.setAuthType(AUTH_TYPE);
    sessionService.addSession((ClusterSession)session);
    Session foundSession = sessionService.findSession(sessionId);
    assertEquals("Session was not found", sessionId, foundSession.getId());
    sessionService.expireSession(sessionId);
    foundSession = sessionService.findSession(sessionId);
    assertEquals("Session was not expired", null, foundSession);
  }

  @Test
  public void testFindSessions(){
    Session session = sessionService.createSession(sessionId);
    session.setAuthType(AUTH_TYPE);
    sessionService.addSession((ClusterSession)session);
    Session[] sessions = sessionService.findSessions();
    assertEquals("Session array size does not match", 1, sessions.length);
    assertEquals("Session array size does not match", sessionId, sessions[0].getId());
    sessionService.removeSession(sessionId);
  }

  @Test
  public void testGetSession(){
    ClusterSession session = (ClusterSession) sessionService.createSession(sessionId);
    session.setAttribute(ATTRIBUTE_NAME, AUTH_TYPE);
    HashMap<String, String> sessionAttributes = sessionService.getSession(sessionId);
    assertNotNull("Session attribute should not be null", sessionAttributes);
    assertEquals("Session attribute size does not match", 1, sessionAttributes.size());
    assertEquals("Session attribute size does not match", AUTH_TYPE, sessionAttributes.get(ATTRIBUTE_NAME));
    sessionService.removeSession(sessionId);
  }

  @Test
  public void testNullSessionCache(){
    ClusterSessionManager clustermanager = new ClusterSessionManager();
    ClusterSessionServiceImpl service = new ClusterSessionServiceImpl(clustermanager);
    Session session = service.findSession(sessionId);
    assertNull("Session should be null, as cache is null", session);
    Session[] sessions = service.findSessions();
    assertEquals("Session array should be empty", 0, sessions.length);
    service.removeSession(sessionId);
    service.expireSession(sessionId);
    session = service.createSession(sessionId);
    assertNull("Session should be null, as cache is null", session);
    service.addSession(null);
    Map<String,String> attributes = service.getSession(sessionId);
    assertNull("Attributes should be null, as cache is null", attributes);
    service.updateSession(null);
  }

  @AfterClass
  public static void cleanup(){
      sessionService.stopInternal();
  }
}