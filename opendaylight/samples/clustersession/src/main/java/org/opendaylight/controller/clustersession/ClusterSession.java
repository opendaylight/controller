package org.opendaylight.controller.clustersession;

import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.catalina.Manager;
import org.apache.catalina.SessionListener;
import org.apache.catalina.session.StandardSession;
import org.opendaylight.controller.clustersession.service.ClusterSessionService;

public class ClusterSession extends StandardSession implements Serializable {

  private static final long serialVersionUID = 1L;

  private transient ClusterSessionService sessionService;

  public ClusterSession(Manager manager, ClusterSessionService sessionService) {
    super(manager);
    this.sessionService = sessionService;
  }

  public void setSessionService(ClusterSessionService sessionService){
    this.sessionService = sessionService;
  }

  @Override
  public void setAuthType(String authType) {
    super.setAuthType(authType);
    sessionService.updateSession(this);
  }

  @Override
  public void setCreationTime(long time) {
    super.setCreationTime(time);
    sessionService.updateSession(this);
  }

  @Override
  public void setMaxInactiveInterval(int interval) {
    super.setMaxInactiveInterval(interval);
    sessionService.updateSession(this);
  }

  @Override
  public void setNew(boolean isNew) {
    super.setNew(isNew);
    sessionService.updateSession(this);
  }

  @Override
  public void setPrincipal(Principal principal) {
    super.setPrincipal(principal);
    sessionService.updateSession(this);
  }

  @Override
  public void setValid(boolean isValid) {
    super.setValid(isValid);
    sessionService.updateSession(this);
  }

  @Override
  public void access() {
    super.access();
    sessionService.updateSession(this);
  }

  @Override
  public void endAccess() {
    super.endAccess();
    sessionService.updateSession(this);
  }

  @Override
  public void removeAttribute(String name, boolean notify) {
    super.removeAttribute(name, notify);
    sessionService.updateSession(this);
  }

  @Override
  public void setAttribute(String name, Object value, boolean notify) {
    super.setAttribute(name, value, notify);
    sessionService.updateSession(this);
  }

  @Override
  public void recycle() {
    super.recycle();
    sessionService.updateSession(this);
  }

  @Override
  public void removeNote(String name) {
    super.removeNote(name);
    sessionService.updateSession(this);
  }

  @Override
  public void addSessionListener(SessionListener listener) {
    super.addSessionListener(listener);
    sessionService.updateSession(this);
  }

  @Override
  public void removeSessionListener(SessionListener listener) {
    super.removeSessionListener(listener);
    sessionService.updateSession(this);
  }

  @Override
  public void setNote(String name, Object value) {
    super.setNote(name, value);
    sessionService.updateSession(this);
  }

  /*
   * Certain fields inside Standard session are not serialized, We need to process them here
   */
   public void afterDeserialization(){
    if (listeners == null){
      listeners = new ArrayList<SessionListener>();
    }
    if (notes == null){
      notes = new ConcurrentHashMap<String, Object>();
    }
    if(support == null){
      support = new PropertyChangeSupport(this);
    }
   }

   @Override
   public String toString() {
     StringBuilder sb = new StringBuilder();
     sb.append("ClusterSession[");
     sb.append(id);
     sb.append(", isNew : ");
     sb.append(isNew);
     sb.append(", isValid : ");
     sb.append(isValid);
     sb.append("]");
     return sb.toString();
   }

   /*
    * These methods are added for deserialization purpose
    */

   public void setAuthTypeInternal(String authType){
     this.authType = authType;
   }

   public void setPrincipalInternal(Principal principal){
     this.principal = principal;
   }

   public void setNoteInternal(String name, Object value) {
     notes.put(name, value);
   }
}
