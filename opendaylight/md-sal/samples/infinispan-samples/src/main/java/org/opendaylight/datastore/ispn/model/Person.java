package org.opendaylight.datastore.ispn.model;

import com.google.common.base.Preconditions;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: syedbahm
 * Date: 4/30/14
 */
public class Person {
  final private String path;
  final private String name;
  final private Integer age;
  final private Map<String, Person> children;


  public Person(String path, String name, Integer age, Map<String, Person> children) {
    this.path = path;
    this.name = name;
    this.age = age;
    this.children = children;
  }

  public String getPath() {
    return path;
  }

  public String getName() {
    return name;
  }

  public Integer getAge() {
    return age;
  }


  public Map<String, Person> getChildren() {
    return children;
  }

  @Override
  public String toString() {
    return "Person{" +
        "path='" + path + '\'' +
        ", name='" + name + '\'' +
        ", age=" + age +
        ", children=" + children +
        '}';
  }


  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Person)) return false;

    Person person = (Person) o;

    if (!age.equals(person.age)) return false;
    if (!name.equals(person.name)) return false;
    if (!path.equals(person.path)) return false;
    if (children.size() != person.children.size()) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + name.hashCode();
    result = 31 * result + age.hashCode();
    result = 31 * result + children.hashCode();
    return result;
  }


  public static class Builder {
    private String path;
    private String name;
    private Integer age;
    private Map<String, Person> children = new LinkedHashMap<>();

    private Builder() {
    }


    public int getAge() {
      return age;

    }


    public Builder setPath(String path) {
      this.path = path;
      return this;
    }

    public Builder setAge(final int age) {
      this.age = age;
      return this;
    }

    public Builder setName(final String name) {
      this.name = name;
      return this;
    }


    public Builder addChild(final Person person) {

      children.put(person.getPath(), person);
      return this;
    }

    public Person build() {
      Preconditions.checkState(path != null, "path cannot be null");
      Preconditions.checkState(name != null, "name cannot  be null.");
      Preconditions.checkState(age != null,
          "age cannot be null ");
      return new Person(path, name, age, children);
    }
  }
}


