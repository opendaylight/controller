package org.opendaylight.controller.clustersession;

import java.io.Serializable;
import java.util.Arrays;

public class ClusterSessionData implements Serializable{

  private static final long serialVersionUID = 1L;

  private ClusterSession session;

  private byte[] principalData;

  private byte[] savedRequestData;

  private byte[] savedPrincipalData;

  private String authType;

  private String userName;

  private String password;

  public ClusterSession getSession() {
    return session;
  }

  public void setSession(final ClusterSession session) {
    this.session = session;
  }

  public byte[] getPrincipalData() {
    return principalData;
  }

  public void setPrincipalData(final byte[] principalData) {
    this.principalData = Arrays.copyOf(principalData, principalData.length);
  }

  public String getAuthType() {
    return authType;
  }

  public void setAuthType(String authType) {
    this.authType = authType;
  }

  public byte[] getSavedRequestData() {
    return savedRequestData;
  }

  public void setSavedRequestData(byte[] savedRequestData) {
    this.savedRequestData = Arrays.copyOf(savedRequestData, savedRequestData.length);
  }

  public byte[] getSavedPrincipalData() {
    return savedPrincipalData;
  }

  public void setSavedPrincipalData(byte[] savedPrincipalData) {
    this.savedPrincipalData = Arrays.copyOf(savedPrincipalData, savedPrincipalData.length);
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
