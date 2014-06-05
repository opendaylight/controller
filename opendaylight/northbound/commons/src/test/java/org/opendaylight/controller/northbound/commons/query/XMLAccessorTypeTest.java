package org.opendaylight.controller.northbound.commons.query;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Assert;
import org.junit.Test;

public class XMLAccessorTypeTest {

  @Test
  public void testPublicAccessType() {
    // create bean
    List<TestBean> testList = new ArrayList<TestBean>();
    testList.add(new TestBean("John", "Scott"));
    testList.add(new TestBean("Foo", "Bar"));
    QueryContextTest.p(QueryContextTest.toXml(testList.get(0)));
    QueryContext queryContext = new QueryContextImpl();
    Assert.assertNotNull(queryContext);
    Query<TestBean> query = queryContext.createQuery("test.firstName==Foo",
        TestBean.class);
    Assert.assertNotNull(query);

    List<TestBean> found = query.filter(testList);
    Assert.assertNotNull(found);
    Assert.assertEquals(1, found.size());
    Assert.assertEquals("Foo", found.get(0).firstName);
  }

  @Test
  public void testFieldAccessType() {
    // create bean
    List<FieldAccessBean> testList = new ArrayList<FieldAccessBean>();
    testList.add(new FieldAccessBean("John", "Scott"));
    testList.add(new FieldAccessBean("Foo", "Bar"));
    testList.get(0).setTestField("fieldaccess");

    QueryContextTest.p(QueryContextTest.toXml(testList.get(0)));
    QueryContext queryContext = new QueryContextImpl();
    Assert.assertNotNull(queryContext);
    Query<FieldAccessBean> query = queryContext.createQuery(
        "test.testField==fieldaccess", FieldAccessBean.class);
    Assert.assertNotNull(query);

    List<FieldAccessBean> found = query.filter(testList);
    Assert.assertNotNull(found);
    Assert.assertEquals(1, found.size());
    Assert.assertEquals("John", found.get(0).firstName);
  }

  @Test
  public void testPropertyAccessType() {
    // TODO
  }

  @Test
  public void testNoneAccessType() {
    // TODO
  }

}

// default ( public memeber )
@XmlRootElement(name = "test")
class TestBean {

  public String firstName;
  public String lastName;
  private String testField;

  public TestBean() {
  }

  public TestBean(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }
}

// default ( public memeber )
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "test")
class FieldAccessBean {

  public String firstName;
  public String lastName;
  private String testField;

  public FieldAccessBean() {
  }

  public FieldAccessBean(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public void setTestField(String testField) {
    this.testField = testField;
  }
}
