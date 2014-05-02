package org.opendaylight.datastore.ispn.impl;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.datastore.ispn.model.Person;
import org.opendaylight.datastore.ispn.util.TreeCacheManager;

import static org.opendaylight.datastore.ispn.impl.FamilyTree.*;


public class FamilyTreeTest {

  private static TreeCacheManager tsm = null;

  private FamilyTree ft = null;



  @org.junit.BeforeClass
  public static void beforeClass() throws Exception {
    if(tsm== null){tsm = TreeCacheManager.get();}
  }

  @org.junit.AfterClass
  public static void afterClass() throws Exception {
    if(tsm !=null) {
      tsm.close();
    }
  }

  @org.junit.Before
  public void setup () {
      ft = new FamilyTree();
  }

  private void assertPersonFields(Person person,int familyNumber){

    Person.Builder firstGrandChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_1 + "/grandchildren/grandchild" + ID_1)
        .setAge(8)
        .setName("grandchild" + FamilyTree.ID_1);


    Person.Builder secondGrandChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_1 + "/grandchildren/grandchild" + ID_2)
        .setAge(7)
        .setName("grandchild" + ID_2);
    Assert.assertEquals(person.getChildren().size(), 2);
    Assert.assertEquals(person.getName(), "person" + familyNumber);
    Assert.assertEquals(person.getAge().intValue(), 50);
    //we will assert full the first child
    Assert.assertEquals(person.getChildren().get("/person" + familyNumber + "/children/child" + ID_1)
        , Person.builder()
        .setAge(25)
        .setName("child" + ID_1)
        .setPath("/person" + familyNumber + "/children/child" + ID_1)
        .addChild(firstGrandChildBuilder.build())
        .addChild(secondGrandChildBuilder.build())
        .build());

    //we will assert the fourth grand child
    Assert.assertEquals(person.getChildren()
        .get("/person" + familyNumber + "/children/child" + ID_2)
        .getChildren().get("/person" + familyNumber + "/children/child" + ID_2 + "/grandchildren/grandchild" + ID_4)
        , Person.builder()
        .setAge(3)
        .setName("grandchild" + ID_4)
        .setPath("/person" + familyNumber + "/children/child" + ID_2 + "/grandchildren/grandchild" + ID_4)
        .build());
  }

  @Test
  public void testFamilyCloneCreation(){
    int numberOfFamilies=3;
    for(int familyNumber = 1; familyNumber <= numberOfFamilies; ++familyNumber) {

      Person person = ft.createFamilyClone(familyNumber);
      assertPersonFields(person,familyNumber);

    }

 }

 @Test
  public void writeOfDenseTreeToCache() throws Exception {
   String args[] = {"dense","write","10","1"};
   FamilyTree.main(args);

   for(int i = 1; i <=10; i++) {
     Person person = ft.readCacheToPersonDenseMode(tsm.getCache("familyDenseTree").getNode("/person"+i));
     assertPersonFields(person, i);
   }
  }




  @Test
  public void writeOfNonDenseTreeToCache() {
    String args[] = {"non-dense","write","10","1"};
    FamilyTree.main(args);
    for(int i = 1; i <=10; i++) {
      Person person = ft.readCacheToPersonNonDenseMode(tsm.getCache("familyNonDenseTree").getNode("/person"+i));
      assertPersonFields(person, i);
    }

  }

  @Test
  public void readOfDenseTreeToCache() throws Exception{
    String args[] = {"dense","read","1000","1","nodump"};
    FamilyTree.main(args);
  }


  @Test
  public void readOfNonDenseTreeToCache() throws Exception{
    String args[] = {"non-dense","read","1000","1","nodump"};
    FamilyTree.main(args);
  }


  @Test
  public void timeDenseNonDenseTreeToCache() throws Exception{
    String args[] = {"dense","read","10","1","dump"};
    FamilyTree.main(args);

    String args1[] = {"non-dense","read","10","1","dump"};
    FamilyTree.main(args1);
  }


}
