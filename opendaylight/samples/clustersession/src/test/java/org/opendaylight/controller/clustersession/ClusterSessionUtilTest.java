package org.opendaylight.controller.clustersession;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SavedRequest;
import org.apache.catalina.realm.GenericPrincipal;
import org.junit.Test;
import org.opendaylight.controller.clustersession.impl.ClusterSessionServiceImpl;
import org.opendaylight.controller.clustersession.service.ClusterSessionService;


public class ClusterSessionUtilTest {

  final String AUTH_TYPE = "FORM";
  final String ADMIN = "admin";
  final String REQUEST_URI = "/test";
  final String BLANK = "";
  final String HEADER_NAME = "ContentType";
  final String HEADER_VALUE = "JSON";
  final long creationTime = 54545454L;
  final int interval = 0;
  ClusterSessionManager manager = new ClusterSessionManager();
  ClusterSessionService sessionService = new ClusterSessionServiceImpl(manager);

  @Test
  public void testNullSerializableClusterSession() {
    ClusterSessionData sessionData = ClusterSessionUtil.getSerializableSession(null);
    assertEquals("Session data should be null for null session", null, sessionData);
  }

  @Test
  public void testSerializableClusterSession() {
    ClusterSession customSession = createClusterSesion();
    ClusterSessionData sessionData = ClusterSessionUtil.getSerializableSession(customSession);
    assertEquals("Session authentication type not valid", AUTH_TYPE, sessionData.getAuthType());
    assertEquals("Session username does not match", ADMIN, sessionData.getUserName());
    assertEquals("Session password does not match", ADMIN, sessionData.getPassword());
    assertEquals("Session prinicpal does not match", ADMIN, sessionData.getSession().getPrincipal().getName());
  }

  @Test
  public void testNullDeserialzableclusterSession() {
    ClusterSession session =  ClusterSessionUtil.getDeserializedSession(null, sessionService, manager);
    assertEquals("Session should be null for null session data", null, session);
  }

  @Test
  public void testDeserializableClusterSesion() {
    ClusterSession customSession = createClusterSesion();
    ClusterSessionData sessionData = ClusterSessionUtil.getSerializableSession(customSession);
    customSession = sessionData.getSession();
    customSession.setAuthType(AUTH_TYPE);
    customSession.setNote(Constants.FORM_PRINCIPAL_NOTE, BLANK);
    customSession.setNote(Constants.FORM_REQUEST_NOTE, BLANK);
    ClusterSession session = ClusterSessionUtil.getDeserializedSession(sessionData, sessionService, manager);
    assertEquals("Session authentication type not valid", AUTH_TYPE, session.getAuthType());
    assertEquals("prinicpal name is not valid", ADMIN, session.getPrincipal().getName());
    SavedRequest savedRequest = (SavedRequest)session.getNote(Constants.FORM_REQUEST_NOTE);
    assertEquals("saved request uri does not match", REQUEST_URI, savedRequest.getRequestURI());
    assertEquals("saved request header does not match", HEADER_VALUE, savedRequest.getHeaderValues(HEADER_NAME).next());
    assertEquals("saved request header does not match", Locale.ENGLISH, savedRequest.getLocales().next());
    String username = (String)session.getNote(Constants.FORM_USERNAME);
    assertEquals("username does not match", ADMIN, username);
    String password = (String)session.getNote(Constants.FORM_PASSWORD);
    assertEquals("password does not match", ADMIN, password);
    assertEquals("session manager does not match", manager, session.getManager());
    assertEquals("session creation time does not match", creationTime, session.getCreationTime());
    assertEquals("session man inactive interval does not match", interval, session.getMaxInactiveInterval());
    assertEquals("is session new does not match", true, session.isNew());
    assertEquals("is session valid does not match", true, session.isValid());
  }

  @Test
  public void testSerializationtoFile(){
    ClusterSession customSession = createClusterSesion();
    ClusterSessionData sessionData = ClusterSessionUtil.getSerializableSession(customSession);
    try(
        OutputStream file = new FileOutputStream("sessionData.ser");
        OutputStream buffer = new BufferedOutputStream(file);
        ObjectOutput output = new ObjectOutputStream(buffer);
        ){
      output.writeObject(sessionData);
    }
    catch(IOException ex){
      fail("IO exception while serializing object to a file.");
    }
    try(
        InputStream file = new FileInputStream("sessionData.ser");
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream (buffer);
        ){
      //deserialize the session
      ClusterSessionData recovedSession = (ClusterSessionData)input.readObject();
      //display its data
      ClusterSession session = ClusterSessionUtil.getDeserializedSession(recovedSession, sessionService, manager);
      assertEquals("Session authentication type not valid", AUTH_TYPE, session.getAuthType());
      assertEquals("prinicpal name is not valid", ADMIN, session.getPrincipal().getName());
      SavedRequest savedRequest = (SavedRequest)session.getNote(Constants.FORM_REQUEST_NOTE);
      assertEquals("saved request uri is not valid", REQUEST_URI, savedRequest.getRequestURI());
      assertEquals("saved request header does not match", HEADER_VALUE, savedRequest.getHeaderValues(HEADER_NAME).next());
      assertEquals("saved request header does not match", Locale.ENGLISH, savedRequest.getLocales().next());
      String username = (String)session.getNote(Constants.FORM_USERNAME);
      assertEquals("username does not match", ADMIN, username);
      String password = (String)session.getNote(Constants.FORM_PASSWORD);
      assertEquals("password does not match", ADMIN, password);
    }
    catch(ClassNotFoundException ex){
      fail("Exception in object deserialization from file");
    }
    catch(IOException ex){
      fail("Exception in object deserialization from file");
    }
    File serializedFile = new File("sessionData.ser");
    serializedFile.delete();
  }

  private ClusterSession createClusterSesion(){
    ClusterSession clusterSession = new ClusterSession(manager, sessionService);
    clusterSession.setAuthType(AUTH_TYPE);
    clusterSession.setCreationTime(creationTime);
    clusterSession.setMaxInactiveInterval(interval);
    clusterSession.setNew(true);
    clusterSession.setValid(true);
    List<String> roles = new ArrayList<String>();
    roles.add(ADMIN);
    GenericPrincipal principal = new GenericPrincipal(ADMIN, ADMIN, roles);
    clusterSession.setPrincipal(principal);
    clusterSession.setNote(Constants.FORM_PRINCIPAL_NOTE, principal);
    SavedRequest savedRequest = new SavedRequest();
    savedRequest.setRequestURI(REQUEST_URI);
    savedRequest.addHeader(HEADER_NAME, HEADER_VALUE);
    savedRequest.addLocale(Locale.ENGLISH);
    clusterSession.setNote(Constants.FORM_REQUEST_NOTE, savedRequest);
    clusterSession.setNote(Constants.FORM_USERNAME, ADMIN);
    clusterSession.setNote(Constants.FORM_PASSWORD, ADMIN);
    return clusterSession;
  }
}
