package org.opendaylight.controller.northbound.commons.query;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.controller.northbound.commons.types.StringList;

/**
 */

@XmlRootElement(name="book")
public class BookBean {

    @XmlElement(name="name")
    private String _name; // simple type

    private String _isbn; // method annotation

    @XmlElement(name="author")
    private PersonBean _author; // composite type

    @XmlElementWrapper//for XMLWrapper iterative composite types
    @XmlElement(name="review")
    private final List<ReviewBean> reviews = new ArrayList<ReviewBean>();

    @XmlElement
    private List<String> soldBy; //Iterative Type

    @XmlElementWrapper(name="test")
    @XmlElement
    private final List<StringList> testList = new ArrayList<StringList>(); //XMLWrapper list of list

    @XmlElementWrapper(name="parent")
    @XmlElement(name="child")
    private final List<WrapperList> wrapperList = new ArrayList<WrapperList>(); // XMLWrapper of XMLWrapper

    public BookBean(){}

    public BookBean(String name, String id, PersonBean person) {
        _name = name;
        _isbn = id;
        _author = person;
        soldBy = new ArrayList<String>();
    }

    public BookBean addReview(ReviewBean review) {
        reviews.add(review);
        return this;
    }

    public void setSellerInfo(List<String> sellers) {
        soldBy = new ArrayList<String>(sellers);
    }

    public void addWrapperList(WrapperList list){
        wrapperList.add(list);
    }

    public void addToTestList(StringList testList){
        this.testList.add(testList);
    }
    public String getName() {
        return "1"+_name;
    }

    @XmlElement(name="isbn")
    public String get_isbn() {
        return "pre"+_isbn;
    }

    public PersonBean getauthor() {
        return _author;
    }

    @Override
    public String toString() {
        return "BookBean [_name=" + _name + ", _isbn=" + _isbn + ", _author="
                + _author + ", reviews=" + reviews + ", soldBy=" + soldBy
                + ", testList=" + testList + ", wrapperList=" + wrapperList + "]";
    }

}

class WrapperList {
  @XmlElementWrapper(name="items")
  @XmlElement
  public List<String> item = new ArrayList<String>();
}
