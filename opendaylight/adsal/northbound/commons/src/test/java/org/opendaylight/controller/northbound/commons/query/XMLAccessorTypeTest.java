package org.opendaylight.controller.northbound.commons.query;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.junit.Assert;
import org.junit.Test;

public class XMLAccessorTypeTest {

    @Test
    public void testPublicAccessType() throws Exception {
        // create bean
        List<PublicAccessBean> testList = new ArrayList<PublicAccessBean>();
        testList.add(new PublicAccessBean("John", "Scott", "private", 1,
                "transient", "elem1"));
        testList.add(new PublicAccessBean("Foo", "Bar", "private1", 2,
                "transient1", "elem2"));
        QueryContextTest.p(QueryContextTest.toXml(testList.get(0)));

        QueryContext queryContext = new QueryContextImpl();
        Assert.assertNotNull(queryContext);
        // search for public field
        Query<PublicAccessBean> query = queryContext.createQuery(
                "publicbean.firstName==Foo", PublicAccessBean.class);
        Assert.assertNotNull(query);

        List<PublicAccessBean> found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("Foo", found.get(0).firstName);

        // search for public getter
        query = queryContext.createQuery("publicbean.privateGetterField<2",
                PublicAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).firstName);

        // test for transient field
        query = queryContext.createQuery("publicbean.transientField='trans*'",
                PublicAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(0, found.size());

        // test for private field
        query = queryContext.createQuery("publicbean.privateField==private",
                PublicAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(0, found.size());

        // test for XML Element
        query = queryContext.createQuery("publicbean.element==elem1",
                PublicAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).firstName);
    }

    @Test
    public void testFieldAccessType() throws QueryException {
        // create bean
        List<FieldAccessBean> testList = new ArrayList<FieldAccessBean>();
        testList.add(new FieldAccessBean("John", "Scott", "private", 1, "elem1"));
        testList.add(new FieldAccessBean("Foo", "Bar", "private1", 2, "elem2"));

        QueryContextTest.p(QueryContextTest.toXml(testList.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Assert.assertNotNull(queryContext);
        // test private field
        Query<FieldAccessBean> query = queryContext.createQuery(
                "field.privateField==private", FieldAccessBean.class);
        Assert.assertNotNull(query);

        List<FieldAccessBean> found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).firstName);

        // test public field
        query = queryContext.createQuery("field.firstName==Foo",
                FieldAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("Foo", found.get(0).firstName);

        // test annotated field
        query = queryContext.createQuery("field.element==elem2",
                FieldAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("Foo", found.get(0).firstName);

        // test annotated method
        query = queryContext.createQuery("field.privateGetterField==11",
                FieldAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).firstName);
    }

    @Test
    public void testPropertyAccessType() throws QueryException {
        // create bean
        List<PropertyAccessBean> testList = new ArrayList<PropertyAccessBean>();
        testList.add(new PropertyAccessBean("John", "Scott", "private", 1, "elem1",
                "transient1"));
        testList.add(new PropertyAccessBean("Foo", "Bar", "private1", 2, "elem2",
                "transient2"));

        QueryContextTest.p(QueryContextTest.toXml(testList.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Assert.assertNotNull(queryContext);
        // test public getter public field
        Query<PropertyAccessBean> query = queryContext.createQuery(
                "property.firstName==John", PropertyAccessBean.class);
        Assert.assertNotNull(query);

        List<PropertyAccessBean> found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).firstName);

        // test public field no getter
        query = queryContext.createQuery("property.lastName==Bar",
                PropertyAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(0, found.size());

        // test annotated field
        query = queryContext.createQuery("property.element==elem2",
                PropertyAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("Foo", found.get(0).firstName);

        // test annotated method
        query = queryContext.createQuery("property.field==private",
                PropertyAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).firstName);

        // test transient method
        query = queryContext.createQuery("property.transientField==transient1",
                PropertyAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(0, found.size());
    }

    @Test
    public void testNoneAccessType() throws QueryException {
        // create bean
        List<NoneAccessBean> testList = new ArrayList<NoneAccessBean>();
        testList.add(new NoneAccessBean("John", "Scott", "private"));
        testList.add(new NoneAccessBean("Foo", "Bar", "private1"));

        QueryContextTest.p(QueryContextTest.toXml(testList.get(0)));
        QueryContext queryContext = new QueryContextImpl();
        Assert.assertNotNull(queryContext);
        // test annotated field
        Query<NoneAccessBean> query = queryContext.createQuery(
                "test.firstName==John", NoneAccessBean.class);
        Assert.assertNotNull(query);

        List<NoneAccessBean> found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).getFirstName());
        // test unannotated field
        query = queryContext
                .createQuery("test.lastName==Bar", NoneAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(0, found.size());
        // test annotated method
        query = queryContext.createQuery("test.testField==private",
                NoneAccessBean.class);
        Assert.assertNotNull(query);

        found = query.find(testList);
        Assert.assertNotNull(found);
        Assert.assertEquals(1, found.size());
        Assert.assertEquals("John", found.get(0).getFirstName());

    }

}

// default ( public memeber )
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@XmlRootElement(name = "publicbean")
class PublicAccessBean {

  public String firstName;
  public String lastName;
  private String privateField;
  private int privateGetterField;
  @XmlTransient
  public String transientField;
  @XmlElement(name = "element")
  private String xmlElem;

  public PublicAccessBean() {
  }

  public PublicAccessBean(String firstName, String lastName,
      String privateField, int privateGetterField, String transientField,
      String xmlElem) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.privateField = privateField;
    this.privateGetterField = privateGetterField;
    this.transientField = transientField;
    this.xmlElem = xmlElem;
  }

  public int getPrivateGetterField() {
    return privateGetterField;
  }

  public void setPrivateGetterField(int field) {
    this.privateGetterField = field;
  }
}

// default ( public memeber )
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "field")
class FieldAccessBean {

  public String firstName;
  public String lastName;
  private String privateField;
  private int test;
  @XmlElement(name = "element")
  private String xmlElem;

  public FieldAccessBean() {
  }

  public FieldAccessBean(String firstName, String lastName,
      String privateField, int privateGetterField, String xmlElem) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.privateField = privateField;
    this.xmlElem = xmlElem;
    this.test = privateGetterField;
  }

  public String getPrivateField() {
    return privateField;
  }

  @XmlElement(name = "privateGetterField")
  public int getPrivateGetterField() {
    return test + 10;
  }
}

// default ( public memeber )
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlRootElement(name = "property")
class PropertyAccessBean {

  public String firstName;
  public String lastName;
  private String privateField;
  private int privateGetterField;
  @XmlElement(name = "element")
  private String xmlElem;
  private String transientField;

  public PropertyAccessBean() {
  }

  public PropertyAccessBean(String firstName, String lastName,
      String privateField, int privateGetterField, String xmlElem,
      String transientField) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.privateField = privateField;
    this.privateGetterField = privateGetterField;
    this.xmlElem = xmlElem;
    this.transientField = transientField;
  }

  public int getPrivateGetterField() {
    return privateGetterField;
  }

  @XmlElement(name = "field")
  public String getPrivateField() {
    return privateField;
  }

  public String getFirstName() {
    return firstName;
  }

  @XmlTransient
  public String getTransientField() {
    return transientField;
  }
}

// default ( public memeber )
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "test")
class NoneAccessBean {
  @XmlElement
  private String firstName;
  public String lastName;
  private String testField;

  public NoneAccessBean() {
  }

  public NoneAccessBean(String firstName, String lastName, String testField) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.testField = testField;
  }

  @XmlElement(name = "testField")
  public String getTestField() {
    return testField;
  }

  public String getFirstName() {
    return firstName;
  }
}
