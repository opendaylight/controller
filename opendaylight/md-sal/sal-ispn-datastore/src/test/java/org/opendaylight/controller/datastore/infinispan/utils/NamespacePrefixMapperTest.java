package org.opendaylight.controller.datastore.infinispan.utils;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

public class NamespacePrefixMapperTest {
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testFromInstanceIdentifier() throws Exception {
        String output = new NamespacePrefixMapper().fromInstanceIdentifier("(abcdefgh)lmn/(fqrstkak)grm");
        Assert.assertEquals("(1)lmn/(2)grm", output);
    }

    @Test
    public void testFromFqn() throws Exception {
        final NamespacePrefixMapper mapper = new NamespacePrefixMapper();
        mapper.fromInstanceIdentifier("(abcdeddd)lmn/(fqrstggg)grm");
        String output = mapper.fromFqn("(1)lmn/(2)grm");
        Assert.assertEquals("(abcdeddd)lmn/(fqrstggg)grm", output);
    }

    @Test
    public void testFromFqnReturnsStringAsIsForUnknownNamespaces(){
        final NamespacePrefixMapper mapper = new NamespacePrefixMapper();

        String output = mapper.fromFqn("(1)lmn/(2)grm");
        Assert.assertEquals("(1)lmn/(2)grm" , output);

        mapper.fromInstanceIdentifier("(abcdeddd)lmn");
        output = mapper.fromFqn("(1)lmn/(2)grm");
        Assert.assertEquals("(abcdeddd)lmn/(2)grm" , output);

    }

    @Test
    public void testFromFqnAvoidNumberFormatException(){
        final NamespacePrefixMapper mapper = new NamespacePrefixMapper();
        String output = mapper.fromFqn("(baba)lmn/(2)grm");
        Assert.assertEquals("(baba)lmn/(2)grm" , output);
    }

    @Test
    public void testFromFqnReplaceMultipleOccurrences() throws Exception {
        final NamespacePrefixMapper mapper = new NamespacePrefixMapper();
        mapper.fromInstanceIdentifier("(abcdeddd)lmn/(fqrstggg)grm");
        String output = mapper.fromFqn("(1)lmn/(2)grm/(1)foo");
        Assert.assertEquals("(abcdeddd)lmn/(fqrstggg)grm/(abcdeddd)foo", output);
    }

}
