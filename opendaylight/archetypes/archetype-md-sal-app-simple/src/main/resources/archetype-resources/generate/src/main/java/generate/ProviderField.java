
package ${package}.generate;

public class ProviderField {
  
  private String name;
  
  private String method;

  public ProviderField(String name, String method) {
    this.name = name;
    this.method = method;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

}
