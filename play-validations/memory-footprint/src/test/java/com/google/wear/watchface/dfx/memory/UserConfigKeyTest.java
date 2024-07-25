package com.google.wear.watchface.dfx.memory;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@RunWith(JUnit4.class)
public class UserConfigKeyTest {

    @Test
    public void buildConfigSet_noConfigurationKeys() {
        SizedIterator<UserConfigSet> configSetsIterator =
                UserConfigKey.buildConfigSets(Collections.emptyList());

        assertEquals(0, configSetsIterator.getSize());
        assertEquals(Collections.emptyList(), iteratorToList(configSetsIterator));
    }

    @Test
    public void buildConfigSet_createsAllConfigCombinations() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        List<UserConfigKey> testKeys = new LinkedList<>();
        testKeys.add(buildListConfigKey(document, "l1", "l1-1", "l1-2", "l1-3"));
        testKeys.add(buildListConfigKey(document, "l2", "l2-1", "l2-2", "l2-3"));
        testKeys.add(buildListConfigKey(document, "l3", "l3-1", "l3-2", "l3-3"));

        SizedIterator<UserConfigSet> configSetsIterator = UserConfigKey.buildConfigSets(testKeys);

        assertEquals(27, configSetsIterator.getSize());
    }

    @Test
    public void buildConfigSet_handlesEmptyConfigurations() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        List<UserConfigKey> testKeys = new LinkedList<>();
        testKeys.add(buildListConfigKey(document, "l1", "l1-1", "l1-2", "l1-3"));
        testKeys.add(buildListConfigKey(document, "l2"));
        testKeys.add(buildListConfigKey(document, "l3", "l3-1", "l3-2", "l3-3"));

        SizedIterator<UserConfigSet> configSetsIterator = UserConfigKey.buildConfigSets(testKeys);

        assertEquals(9, configSetsIterator.getSize());
    }

    private <T> List<T> iteratorToList(Iterator<T> iterator) {
        return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .collect(Collectors.toList());
    }

    private static UserConfigKey buildListConfigKey(
            Document document, String listId, String... options) {
        Element node = document.createElement("ListConfiguration");
        for (String s : options) {
            Element listOption = document.createElement("ListOption");
            listOption.setAttribute("id", s);
            node.appendChild(listOption);
        }
        return new UserConfigKey(listId, node);
    }
}
