package org.opendaylight.controller.sal.restconf.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.*;

import org.junit.Test;

import com.google.gson.stream.JsonReader;

public class YangAndXmlToJsonConversionJsonReaderTest {

    @Test
    public void simpleYangTypesWithJsonReaderTest() {
        String jsonOutput;
        jsonOutput = TestUtils.readJsonFromFile("/yang-to-json-conversion/simple-yang-types/xml/awaited_output.json",
                false);

//        jsonOutput = TestUtils.convertXmlDataAndYangToJson("/yang-to-json-conversion/simple-yang-types/xml/data.xml",
//                "/yang-to-json-conversion/simple-yang-types");
        
        StringReader strReader = new StringReader(jsonOutput);
        JsonReader jReader = new JsonReader(strReader);
        try {
            checkCont1(jReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkCont1(JsonReader jReader) throws IOException {
        jReader.beginObject();
        assertNotNull("cont1 is missing.", jReader.hasNext());
        jReader.nextName();
        checkCont1Elements(jReader, prepareInputData(jReader), "cont1/");
        jReader.endObject();

    }

    private Map<String, String> prepareInputData(JsonReader jReader) {
        Map<String, String> dataMap = new HashMap<>();
        dataMap.put("cont1/lf11", "lf");
        dataMap.put("cont1/lflst11.1", "55");
        dataMap.put("cont1/lflst11.2", "56");
        dataMap.put("cont1/lflst11.3", "57");
        dataMap.put("cont1/lflst12.1", "lflst12 str1");
        dataMap.put("cont1/lflst12.2", "lflst12 str2");
        dataMap.put("cont1/lflst12.3", "lflst12 str3");

        dataMap.put("cont1/lst11.1/lf111", "140");
        dataMap.put("cont1/lst11.1/lf112", "lf112 str");
        dataMap.put("cont1/lst11.1/cont111/lf1111", "lf1111 str");
        dataMap.put("cont1/lst11.1/cont111/lflst1111.1", "2048");
        dataMap.put("cont1/lst11.1/cont111/lflst1111.2", "1024");
        dataMap.put("cont1/lst11.1/cont111/lflst1111.3", "4096");
        dataMap.put("cont1/lst11.1/cont111/lst1111.1/lf1111A", "lf1111A str11");
        dataMap.put("cont1/lst11.1/cont111/lst1111.1/lf1111B", "4");
        dataMap.put("cont1/lst11.1/cont111/lst1111.2/lf1111A", "lf1111A str12");
        dataMap.put("cont1/lst11.1/cont111/lst1111.2/lf1111B", "7");
        dataMap.put("cont1/lst11.1/lst111.1/lf1111", "65");
        dataMap.put("cont1/lst11.1/lst112.1/lf1121", "lf1121 str11");

        dataMap.put("cont1/lst11.2/lf111", "141");
        dataMap.put("cont1/lst11.2/lf112", "lf112 str2");
        dataMap.put("cont1/lst11.2/cont111/lf1111", "lf1111 str2");
        dataMap.put("cont1/lst11.2/cont111/lflst1111.1", "2049");
        dataMap.put("cont1/lst11.2/cont111/lflst1111.2", "1025");
        dataMap.put("cont1/lst11.2/cont111/lflst1111.3", "4097");
        dataMap.put("cont1/lst11.2/cont111/lst1111.1/lf1111A", "lf1111A str21");
        dataMap.put("cont1/lst11.2/cont111/lst1111.1/lf1111B", "5");
        dataMap.put("cont1/lst11.2/cont111/lst1111.2/lf1111A", "lf1111A str22");
        dataMap.put("cont1/lst11.2/cont111/lst1111.2/lf1111B", "8");
        dataMap.put("cont1/lst11.2/lst111.1/lf1111", "55");
        dataMap.put("cont1/lst11.2/lst111.2/lf1111", "56");
        dataMap.put("cont1/lst11.2/lst112.1/lf1121", "lf1121 str21");
        dataMap.put("cont1/lst11.2/lst112.2/lf1121", "lf1121 str22");

        return dataMap;

    }

    private void checkCont1Elements(JsonReader jReader, Map<String, String> dataMap, String pthPref) throws IOException {
        Set<String> keys = new HashSet<>();
        jReader.beginObject();
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf11")) {
                assertEquals("Key " + keyName + " has incorrect value.", dataMap.get(pthPref + keyName),
                        jReader.nextString());
                keys.add(keyName);
            } else if (keyName.equals("lflst11")) {
                checkLflstValues(jReader, pthPref + keyName, dataMap);
                keys.add(keyName);
            } else if (keyName.equals("lflst12")) {
                checkLflstValues(jReader, pthPref + keyName, dataMap);
                keys.add(keyName);
            } else if (keyName.equals("lst11")) {
                checkLst11(jReader, pthPref + keyName, dataMap);
                keys.add(keyName);
            } else {
                assertTrue("Key " + keyName + " doesn't exists in yang file.", false);
            }
        }
        jReader.endObject();
        assertEquals("Incorrect number of keys in cont1", 4, keys.size());

    }

    private void checkLst11(JsonReader jReader, String pthPref, Map<String, String> dataMap) throws IOException {
        jReader.beginArray();

        int arrayLength = 0;
        while (jReader.hasNext()) {
            checkLst11Elements(jReader, pthPref + "." + ++arrayLength + "/", dataMap);
        }
        jReader.endArray();
        assertEquals("Incorrect number of items in lst11 array.", 2, arrayLength);
    }

    private void checkLst11Elements(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginObject();
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf111")) {
                assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());
            } else if (keyName.equals("lf112")) {
                assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());
            } else if (keyName.equals("cont111")) {
                checkCont111(jReader, pthPref + keyName, data);
            } else if (keyName.equals("lst111")) {
                checkLst111(jReader, pthPref + keyName, data);
            } else if (keyName.equals("lst112")) {
                checkLst112(jReader, pthPref + keyName, data);
            } else {
                assertTrue("Key " + keyName + " doesn't exists in yang file.", false);
            }
        }
        jReader.endObject();
    }

    private void checkLst112(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginArray();
        int arrayIndex = 0;
        while (jReader.hasNext()) {
            checkLst112Elements(jReader, pthPref + "." + ++arrayIndex + "/", data);
        }
        jReader.endArray();
    }

    private void checkLst112Elements(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginObject();
        if (jReader.hasNext()) {
            String keyName = jReader.nextName();
            assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());
        }
        jReader.endObject();

    }

    private void checkLst111(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginArray();
        int arrayIndex = 0;
        while (jReader.hasNext()) {
            checkLst111Elements(jReader, pthPref + "." + ++arrayIndex + "/", data);
        }
        jReader.endArray();
    }

    private void checkLst111Elements(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginObject();
        if (jReader.hasNext()) {
            String keyName = jReader.nextName();
            assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());
        }
        jReader.endObject();
    }

    private void checkCont111(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginObject();
        checkCont111Elements(jReader, pthPref + "/", data);
        jReader.endObject();
    }

    private void checkCont111Elements(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf1111")) {
                assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());
            } else if (keyName.equals("lflst1111")) {
                checkLflstValues(jReader, pthPref + keyName, data);
            } else if (keyName.equals("lst1111")) {
                checkLst1111(jReader, pthPref + keyName, data);
            }
        }

    }

    private void checkLst1111(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginArray();
        int arrayIndex = 0;
        while (jReader.hasNext()) {
            checkLst1111Elements(jReader, pthPref + "." + ++arrayIndex + "/", data);
        }
        jReader.endArray();
    }

    private void checkLst1111Elements(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginObject();
        while (jReader.hasNext()) {
            String keyName = jReader.nextName();
            if (keyName.equals("lf1111A")) {
                assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());

            } else if (keyName.equals("lf1111B")) {
                assertEquals("Incorrect value for key " + keyName, data.get(pthPref + keyName), jReader.nextString());
            }
        }
        jReader.endObject();
    }

    private void checkLflstValues(JsonReader jReader, String pthPref, Map<String, String> data) throws IOException {
        jReader.beginArray();
        int arrayIndex = 1;
        String keyValue = null;
        List<String> searchedValues = new ArrayList<>();
        while ((keyValue = data.get(pthPref + "." + arrayIndex++)) != null) {
            searchedValues.add(keyValue);
        }

        while (jReader.hasNext()) {
            String value = jReader.nextString();
            assertTrue("Value " + value + " of lflst " + pthPref + " wasn't found", searchedValues.contains(value));
        }

        jReader.endArray();
    }



}
