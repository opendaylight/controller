package org.opendaylight.datastore.ispn.impl;

import org.infinispan.tree.Fqn;
import org.infinispan.tree.Node;
import org.infinispan.tree.TreeCache;
import org.opendaylight.datastore.ispn.model.Person;
import org.opendaylight.datastore.ispn.util.TreeCacheManager;

import javax.transaction.TransactionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * FamilyTree helps in  determine whether treating a tree structure with each leaf element as a node is more performant
 * in ISPN Tree Cache Vs treating a tree structure with each leaf element as a property-value pair to the container
 * it belongs which becomes a node
 *
 * The sample Family tree schema looks like
      Person
             --name:attribute
              --age: attribute
          list children
                  child 1
                        --name:attribute
                        --age: attribute
                  list grand-children
                          grand child 1
                                    -- name:attribute
                                    -- age:attribute

                          grand child 2
                                    --name:attribute
                                     --age: attribute

                   child 2
                          --name:attribute
                         --age: attribute
                   list grand-children
                           grand child 3
                                   -- name:attribute
                                   -- age:attribute

                          grand child 4
                                    --name:attribute
                                    --age: attribute


 *
 * @author: syedbahm
 * Date: 4/30/14
 */

public class FamilyTree {

  public static final String ID_2 = "2";
  public static final String ID_1 = "1";
  public static final String ID_3 = "3";
  public static final String ID_4 = "4";
  private final TreeCacheManager tcm;

  public FamilyTree() {
    tcm = TreeCacheManager.get();
  }


  public Person createFamilyClone(int familyNumber) {

    Person.Builder firstGrandChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_1 + "/grandchildren/grandchild" + ID_1)
        .setAge(8)
        .setName("grandchild" + ID_1);


    Person.Builder secondGrandChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_1 + "/grandchildren/grandchild" + ID_2)
        .setAge(7)
        .setName("grandchild" + ID_2);

    Person.Builder thirdGrandChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_2 + "/grandchildren/grandchild" + ID_3)
        .setAge(4)
        .setName("grandchild" + ID_3);


    Person.Builder fourthGrandChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_2 + "/grandchildren/grandchild" + ID_4)
        .setAge(3)
        .setName("grandchild" + ID_4);

    Person.Builder secondChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_2)
        .setAge(23)
        .setName("child" + ID_2)
        .addChild(thirdGrandChildBuilder.build())
        .addChild(fourthGrandChildBuilder.build());

    Person.Builder firstChildBuilder = Person.builder()
        .setPath("/person" + familyNumber + "/children/child" + ID_1)
        .setAge(25)
        .setName("child" + ID_1)
        .addChild(firstGrandChildBuilder.build())
        .addChild(secondGrandChildBuilder.build());

    Person.Builder personBuilder = Person.builder()
        .setName("person" + familyNumber)
        .setAge(50)
        .setPath("/person" + familyNumber)
        .addChild(firstChildBuilder.build())
        .addChild(secondChildBuilder.build());

    return personBuilder.build();
  }

  /*
      Based on the supplied count prepares the number of families
   */
  Map<Integer, Person> prepareFamilyObjects(int numberOfFamilies) {
    Map<Integer, Person> familyMap = new HashMap<Integer, Person>();
    for (int i = 1; i <= numberOfFamilies; ++i) {
      familyMap.put(i, this.createFamilyClone(i));
    }
    return familyMap;
  }

  /*
    Helper method when writing the Tree in dense fashion in TreeCache
     - looks for comments of next method to understand dense tree
   */
  void writePersonToCacheDenseMode(final Person person, final TreeCache tc) {

    tc.put(Fqn.fromString(person.getPath() + "/name"), "name", person.getName());
    tc.put(Fqn.fromString(person.getPath() + "/age"), "age", person.getAge().toString());
    for (Person child : person.getChildren().values()) {
      writePersonToCacheDenseMode(child, tc);
    }

  }


  /*Dense tree is defined as having one node for each attribute element in the tree cache -- the tree structure
    looks like below in tree cache for dense tree

    /person1/name
    /person1/age
    /person1/children/child1/name
    /person1/children/child1/age
    /person1/children/child1/grandchildren/grandchild1/name
    /person1/children/child1/grandchildren/grandchild1/age

  */

  void writeDenseTree(int numberOfFamilies) throws Exception {
    long startTimeIncludingFamilyCreation = System.currentTimeMillis();
    TreeCache<String, String> tc = tcm.getCache("familyDenseTree");
    TransactionManager tm = tc.getCache().getAdvancedCache().getTransactionManager();
    Map<Integer, Person> familyMap = prepareFamilyObjects(numberOfFamilies);
    long startTime, endTime;
    startTime = System.currentTimeMillis();
    System.out.println("Writing " + numberOfFamilies + " families to TreeCache(excluding family object creation) started@" + startTime);
    for (int i = 1; i <= numberOfFamilies; ++i) {
      tm.begin();
      writePersonToCacheDenseMode(familyMap.get(i), tc);
      tm.commit();
    }
    endTime = System.currentTimeMillis();
    System.out.println("Writing " + numberOfFamilies + " families to TreeCache ended@" + endTime);
    System.out.println("Total time to write " + numberOfFamilies + " families to TreeCache(excluding family object creation) in dense mode (ms):" + (endTime - startTime));
    System.out.println("Total time to write " + numberOfFamilies + " families to TreeCache(including family object creation) in dense mode (ms):"
        + (endTime - startTimeIncludingFamilyCreation));
  }
  /*
  Helper method while building the family with treecache in dense mode.
   */
  Person readCacheToPersonDenseMode(Node node) {
    Person.Builder personBuilder = Person.builder();
    if (node != null) {
      Set<Node> children = node.getChildren();
      if (!children.isEmpty()) {
        for (Object child : children) {
          Node potentialChild = (Node) child;
          if ((potentialChild.getData() == null) || (potentialChild.getData().isEmpty())) {
            Set<Node>potentialChildren = potentialChild.getChildren();
            if (potentialChildren != null) {
              for (Object nextChild : potentialChildren) {
                Person childX = readCacheToPersonDenseMode((Node) nextChild);
                personBuilder.addChild(childX);
              }
            }
          } else {

            Map map = potentialChild.getData();

            if (map.get("age") != null) {
              personBuilder.setAge(Integer.parseInt((String) map.get("age")));
            } else if (map.get("name") != null) {
              personBuilder.setName((String) map.get("name"));
            }
          }
        }
      }
      return personBuilder.setPath(node.getFqn().toString()).build();
    } else {
      System.out.println("readCacheToPersonDenseMode found to be empty");

      return null;
    }

  }

  /**
   * Reads the number of families mentioned and dumps the trees
   * to the console based on dump value
   * @param numberOfFamilies
   * @param dump
   * @throws Exception
   */
  void readDenseTree(int numberOfFamilies, boolean dump) throws Exception {

    TreeCache<String, String> tc = tcm.getCache("familyDenseTree");
    TransactionManager tm = tc.getCache().getAdvancedCache().getTransactionManager();

    long startTime, endTime;
    startTime = System.currentTimeMillis();
    System.out.println("Reading " + numberOfFamilies + " families from TreeCache(including family object creation) dense mode started@" + startTime);
    Map<Integer, Person> families = new HashMap<>();
    //ok here we will try to read each family object and create a full Person object
    for (int i = 1; i <= numberOfFamilies; i++) {
      tm.begin();
      families.put(i, this.readCacheToPersonDenseMode(tc.getNode(Fqn.fromString("/person" + i))));
      tm.commit();
    }

    endTime = System.currentTimeMillis();
    System.out.println("Reading " + numberOfFamilies + " families from TreeCache(including family object creation) dense mode ended@" + endTime);
    System.out.println("Total time taken to read " + numberOfFamilies + " families from TreeCache(including family object creation) dense mode(ms)" + (endTime - startTime));

    if (dump) {
      System.out.println("dumping the tree read in dense mode...");
      for (Person person : families.values()) {
        dump(person);
      }
    }

  }





  /*
  Helper method while writing the tree in non-dense fashion in tree cache
        refer to the next method comments to understand non-dense fashion
   */
  void writePersonToCacheNonDenseMode(final Person person, final TreeCache tc) {
    Map<String, String> data = new HashMap<>();
    data.put("name", person.getName());
    data.put("age", person.getAge().toString());

    tc.put(Fqn.fromString(person.getPath()), data);

    for (Person child : person.getChildren().values()) {
      writePersonToCacheNonDenseMode(child, tc);
    }

  }

  /*Non dense tree is defined as having one node for node element in tree while the attributes become data of the
     node
   -- the tree structure
    looks like below in tree cache for dense tree


    /person1  {{name,nameValue},{age,ageValue}}
    /person1/children/child1 {{name,nameValue},{age,ageValue}}
    /person1/children/child1/grandchildren/grandchild1 {{name,value},{age,value}}
    ...

  */
  void writeNonDenseTree(int numberOfFamilies) throws Exception {
    long startTimeIncludingFamilyCreation = System.currentTimeMillis();
    TreeCache<String, String> tc = tcm.getCache("familyNonDenseTree");
    TransactionManager tm = tc.getCache().getAdvancedCache().getTransactionManager();
    Map<Integer, Person> familyMap = prepareFamilyObjects(numberOfFamilies);
    long startTime, endTime;
    startTime = System.currentTimeMillis();
    System.out.println("Writing " + numberOfFamilies + " families to TreeCache(excluding family object creation) non-dense mode started@" + startTime);
    for (int i = 1; i <= numberOfFamilies; ++i) {
      tm.begin();
      writePersonToCacheNonDenseMode(familyMap.get(i), tc);
      tm.commit();
    }
    endTime = System.currentTimeMillis();
    System.out.println("Writing " + numberOfFamilies + " families to TreeCache ended@" + endTime);
    System.out.println("Total time to write " + numberOfFamilies + " families to TreeCache(excluding family object creation) in non-dense mode (ms):" + (endTime - startTime));
    System.out.println("Total time to write " + numberOfFamilies + " families to TreeCache(including family object creation) in non-dense mode (ms):"
        + (endTime - startTimeIncludingFamilyCreation));
  }


  /**
   * Helper method while converting a TreeCache node to person family object
   * @param node
   * @return
   */

  Person readCacheToPersonNonDenseMode(Node node) {
    Person.Builder personBuilder = Person.builder();
    if (node != null) {
      Map data = node.getData();
      if (( data!= null) || (!data.isEmpty())) {

        personBuilder.setAge(Integer.valueOf((String) data.get("age")));
        personBuilder.setName((String) data.get("name"));
        personBuilder.setPath(node.getFqn().toString());
      }
      Set<Node>children = node.getChildren();
      if (!children.isEmpty()) {
        for (Object child : children) {
          Node potentialChild = (Node) child;
          Set<Node>potentialChildren= potentialChild.getChildren();
          if (potentialChildren != null) {
            for (Object nextChild : potentialChildren) {
              Person childX = readCacheToPersonNonDenseMode((Node) nextChild);
              personBuilder.addChild(childX);
            }
          }
        }

      }
      return personBuilder.build();
    } else {
      return null;
    }
  }


  /**
   * Reads specified number of families from treecache in non-dense mode
   * and dumps the read families to console if dump is true
   * @param numberOfFamilies
   * @param dump
   * @throws Exception
   */

  void readNonDenseTree(int numberOfFamilies, boolean dump) throws Exception {
    TreeCache<String, String> tc = tcm.getCache("familyNonDenseTree");
    TransactionManager tm = tc.getCache().getAdvancedCache().getTransactionManager();

    long startTime, endTime;
    startTime = System.currentTimeMillis();
    System.out.println("Reading " + numberOfFamilies + " families from TreeCache(including family object creation) non-dense mode started@" + startTime);
    Map<Integer, Person> families = new HashMap<>();
    //ok here we will try to read each family object and create a full Person object
    for (int i = 1; i <= numberOfFamilies; i++) {
      tm.begin();
      families.put(i, this.readCacheToPersonNonDenseMode(tc.getNode(Fqn.fromString("/person" + i))));
      tm.commit();
    }
    endTime = System.currentTimeMillis();
    System.out.println("Reading " + numberOfFamilies + " families from TreeCache(including family object creation) non-dense mode ended@" + endTime);
    System.out.println("Total time taken to read " + numberOfFamilies + " families from TreeCache(including family object creation) non-dense mode (ms)" + (endTime - startTime));


    if (dump) {
      System.out.println("dumping the tree read in non dense mode...");
      for (Person person : families.values()) {
        dump(person);
      }
    }


  }
 //helper
  private void dump(Person person) {
    System.out.println(person.toString());
  }
  //helper
  private void usage() {

    StringBuffer sb = new StringBuffer();
    System.out.println(sb.append("Usage: FamilyTree <type> <operation> <count> <numOfThread> <dump>\n")
        .append("\t <type> can be dense or non-dense\n")
        .append("\t <operatoin> can be write or read \n")
        .append("\t <count> the number of families to be written or read \n")
        .append("\t <numOfThread> number of threads to use --currently ignored")
        .append("\t <dump> can be dump or no dump -- to dump or not dump the tree. ignored for write operations")
        .toString());
  }

  /**
   * Usage: FamilyTree <type> <operation> <count> <numOfThread> <dump>
   *   Refer to usage method above to get details of the expected arguments
   */

  public static void main(String args[]) {
    //here we should be able to create tree (dense, less dense) in ISPN from family objects
    try {
      FamilyTree ft = new FamilyTree();
      if (args.length < 4) {
        ft.usage();

        return;
      }
      boolean dump = false;
      if (args[1].equals("read") && args.length == 5) {
        if (args[4].equals("dump")) {
          dump = true;
        } else if (args[4].equals("nodump")) {
          dump = false;
        } else {
          ft.usage();
          return;
        }
      }

      if ((args[0].toLowerCase().equals("dense"))) {
        if (args[1].toLowerCase().equals("write")) {

          ft.writeDenseTree(new Integer(args[2]));
        } else if (args[1].toLowerCase().equals("read")) {
          ft.writeDenseTree(new Integer(args[2]));
          //start reading the data from the dense mode tree cache
          ft.readDenseTree(new Integer(args[2]), dump);
        }
      } else if ((args[0].toLowerCase().equals("non-dense"))) {
        //here we should be able to create family objects from (dense,non dense) tree in ISPN
        if (args[1].toLowerCase().equals("write")) {
          ft.writeNonDenseTree(new Integer(args[2]));
        } else if ((args[1]).toLowerCase().equals("read")) {
          ft.writeNonDenseTree(new Integer(args[2]));
          //start reading the data from the dense mode tree cache
          ft.readNonDenseTree(new Integer(args[2]), dump);
        }
      }


    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private void close() throws Exception {
    tcm.close();
  }
}
