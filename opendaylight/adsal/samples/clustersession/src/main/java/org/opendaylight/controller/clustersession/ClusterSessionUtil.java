/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 */
package org.opendaylight.controller.clustersession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.catalina.authenticator.Constants;
import org.apache.catalina.authenticator.SavedRequest;
import org.apache.catalina.ha.session.SerializablePrincipal;
import org.apache.catalina.realm.GenericPrincipal;
import org.opendaylight.controller.clustersession.service.ClusterSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ClusterSessionUtil will be used to convert ClusterSession object into ClusterSessionData object,
 * which is serializable and can be passed for storage. This class also perform deserialization to
 * create ClusterSession object
 * @author harman singh
 *
 */

public class ClusterSessionUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterSessionUtil.class);

  private ClusterSessionUtil() {

  }

  /**
   * Serialize the ClusterSession object to provide ClusterSessionData object,
   * that will be used for storage like in inifinispan or memcache etc.
   * @param session an instance of ClusterSession
   * @return an instance of ClusterSessionData
   */
  public static ClusterSessionData getSerializableSession(ClusterSession session) {
    if(session == null){
      return null;
    }
    ClusterSessionData sessionData = new ClusterSessionData();
    sessionData.setSession(session);
    sessionData.setAuthType(session.getAuthType());
    sessionData.setPrincipalData(serializePrincipal(session.getPrincipal()));
    sessionData.setSavedRequestData(serializeSavedRequest(session.getNote(Constants.FORM_REQUEST_NOTE)));
    Principal notePrincipal = (Principal) session.getNote(Constants.FORM_PRINCIPAL_NOTE);
    byte[] principalBytes = serializePrincipal(notePrincipal);
    sessionData.setSavedPrincipalData(principalBytes);
    if(session.getPrincipal() == null && notePrincipal != null){
      sessionData.setPrincipalData(principalBytes);
    }
    sessionData.setUserName((String) session.getNote(Constants.FORM_USERNAME));
    sessionData.setPassword((String) session.getNote(Constants.FORM_PASSWORD));
    return sessionData;
  }

  /**
   * Deserialize the ClusterSessionData object that usually comes from storage
   * to provide ClusterSession object,
   * that will be used by Session Manager
   * @param sessionData an instance of ClusterSessionData
   * @param sessionService an instance of ClusterSessionService
   * @param manager an instance of ClusterSessionManager
   * @return an instance of ClusterSession
   */

  public static ClusterSession getDeserializedSession(ClusterSessionData sessionData, ClusterSessionService sessionService,
      ClusterSessionManager manager) {
    if(sessionData == null){
      return null;
    }
    ClusterSession session = sessionData.getSession();
    session.afterDeserialization();
    session.setManager(manager);
    session.setSessionService(sessionService);
    if(sessionData.getAuthType() != null) {
      session.setAuthTypeInternal(sessionData.getAuthType());
    }
    if(sessionData.getPrincipalData() != null && sessionData.getPrincipalData().length > 0){
      session.setPrincipalInternal(deserializePrincipal(sessionData.getPrincipalData()));
    }
    if(sessionData.getSavedPrincipalData() != null && sessionData.getSavedPrincipalData().length > 0){
      session.setNoteInternal(Constants.FORM_PRINCIPAL_NOTE, deserializePrincipal(sessionData.getSavedPrincipalData()));
    }
    if(sessionData.getSavedRequestData() != null && sessionData.getSavedRequestData().length > 0){
      session.setNoteInternal(Constants.FORM_REQUEST_NOTE, deserializeSavedRequest(sessionData.getSavedRequestData()));
    }
    if(sessionData.getUserName() != null){
      session.setNoteInternal(Constants.FORM_USERNAME, sessionData.getUserName());
    }
    if(sessionData.getPassword() != null){
      session.setNoteInternal(Constants.FORM_PASSWORD, sessionData.getPassword());
    }
    return session;
  }

  private static byte[] serializePrincipal(final Principal principal){
    if(principal == null) {
      return new byte[0];
    }
    ByteArrayOutputStream bos = null;
    ObjectOutputStream oos = null;
    try {
      bos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(bos);
      SerializablePrincipal.writePrincipal((GenericPrincipal) principal, oos );
      oos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException( "Non-serializable object", e);
    } finally {
      closeSilently(bos);
      closeSilently(oos);
    }
  }

  private static byte[] serializeSavedRequest(final Object obj) {
    if(obj == null) {
      return new byte[0];
    }
    final SavedRequest savedRequest = (SavedRequest) obj;
    ByteArrayOutputStream bos = null;
    ObjectOutputStream oos = null;
    try {
      bos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(bos);
      oos.writeObject(savedRequest.getContentType());
      oos.writeObject(getHeaders(savedRequest));
      oos.writeObject(newArrayList(savedRequest.getLocales()));
      oos.writeObject(savedRequest.getMethod());
      oos.writeObject(savedRequest.getQueryString());
      oos.writeObject(savedRequest.getRequestURI());
      oos.writeObject(savedRequest.getDecodedRequestURI());
      oos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      throw new IllegalArgumentException( "Non-serializable object", e);
    } finally {
      closeSilently(bos);
      closeSilently(oos);
    }
  }

  private static Principal deserializePrincipal(final byte[] data) {
    ByteArrayInputStream bis = null;
    ObjectInputStream ois = null;
    try {
      bis = new ByteArrayInputStream(data);
      ois = new ObjectInputStream(bis);
      return SerializablePrincipal.readPrincipal(ois);
    } catch (IOException e) {
      throw new IllegalArgumentException( "Could not deserialize principal", e);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException( "Could not deserialize principal", e);
    } finally {
      closeSilently(bis);
      closeSilently(ois);
    }
  }

  @SuppressWarnings("unchecked")
  private static SavedRequest deserializeSavedRequest(final byte[] data) {
    ByteArrayInputStream bis = null;
    ObjectInputStream ois = null;
    try {
      bis = new ByteArrayInputStream(data);
      ois = new ObjectInputStream(bis);
      final SavedRequest savedRequest = new SavedRequest();
      savedRequest.setContentType((String) ois.readObject());
      setHeaders(savedRequest, (Map<String, List<String>>) ois.readObject());
      setLocales(savedRequest, (List<Locale>) ois.readObject());
      savedRequest.setMethod((String) ois.readObject());
      savedRequest.setQueryString((String) ois.readObject());
      savedRequest.setRequestURI((String) ois.readObject());
      savedRequest.setDecodedRequestURI((String) ois.readObject());
      return savedRequest;
    } catch (final IOException e) {
      throw new IllegalArgumentException( "Could not deserialize SavedRequest", e );
    } catch (final ClassNotFoundException e) {
      throw new IllegalArgumentException( "Could not deserialize SavedRequest", e );
    } finally {
      closeSilently(bis);
      closeSilently(ois);
    }
  }

  private static void setLocales(final SavedRequest savedRequest, final List<Locale> locales) {
    if(locales != null && !locales.isEmpty()) {
      for (final Locale locale : locales) {
        savedRequest.addLocale(locale);
      }
    }
  }

  private static <T> List<T> newArrayList(final Iterator<T> iter) {
    if(!iter.hasNext()) {
      return Collections.emptyList();
    }
    final List<T> result = new ArrayList<T>();
    while (iter.hasNext()) {
      result.add(iter.next());
    }
    return result;
  }

  private static Map<String, List<String>> getHeaders(final SavedRequest obj) {
    final Map<String, List<String>> result = new HashMap<String, List<String>>();
    final Iterator<String> namesIter = obj.getHeaderNames();
    while (namesIter.hasNext()) {
      final String name = namesIter.next();
      final List<String> values = new ArrayList<String>();
      result.put(name, values);
      final Iterator<String> valuesIter = obj.getHeaderValues(name);
      while (valuesIter.hasNext()) {
        final String value = valuesIter.next();
        values.add(value);
      }
    }
    return result;
  }

  private static void setHeaders(final SavedRequest obj, final Map<String, List<String>> headers) {
    if(headers != null) {
      for (final Entry<String, List<String>> entry : headers.entrySet()) {
        final List<String> values = entry.getValue();
        for (final String value : values) {
          obj.addHeader(entry.getKey(), value);
        }
      }
    }
  }

  private static void closeSilently(final OutputStream os) {
    if (os != null) {
      try {
        os.close();
      } catch (final IOException f) {
        LOGGER.debug("Exception occurred while closing output stream",  f.toString());
      }
    }
  }

  private static void closeSilently(final InputStream is) {
    if (is != null) {
      try {
        is.close();
      } catch (final IOException f) {
        LOGGER.debug("Exception occurred while closing input stream", f.toString());
      }
    }
  }
}
