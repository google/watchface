package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.wear.watchface.dfx.memory.DrawableNodeConfigTable.Entry;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class DrawableNodeConfigTableTest {

    private static final EvaluationSettings TEST_SETTINGS = new EvaluationSettings("", "");
    private static final VariantConfigValue TEST_VARIANT = VariantConfigValue.active(TEST_SETTINGS);

    @Test
    public void drawableNodeConfigTable_create_populatesExpectedNodesForActive() throws Exception {
        Node sceneNode = readSceneNode("/DrawableNodeConfigTableTestLayout.xml");

        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(sceneNode, VariantConfigValue.active(TEST_SETTINGS));

        assertEquals(
                ImmutableList.of(
                        new Entry(
                                findPartImage(sceneNode, "active-list1-part-image1"),
                                configSetOf("l1", "l1-1")),
                        new Entry(
                                findPartImage(sceneNode, "active-list1-part-image2"),
                                configSetOf("l1", "l1-2")),
                        new Entry(
                                findPartImage(sceneNode, "active-list1-part-image3"),
                                configSetOf("l1", "l1-3")),
                        new Entry(
                                findPartImage(sceneNode, "ambient-and-active-part-image"),
                                new UserConfigSet(Collections.emptyMap()))),
                drawableNodeConfigTable.getAllEntries());
    }

    @Test
    public void drawableNodeConfigTable_create_populatesExpectedNodesForAmbient() throws Exception {
        Node sceneNode = readSceneNode("/DrawableNodeConfigTableTestLayout.xml");

        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(
                        sceneNode, VariantConfigValue.ambient(TEST_SETTINGS));

        assertEquals(
                ImmutableList.of(
                        new Entry(
                                findPartImage(sceneNode, "ambient-list2-part-image1"),
                                configSetOf("l2", "l2-1")),
                        new Entry(
                                findPartImage(sceneNode, "ambient-list2-part-image2"),
                                configSetOf("l2", "l2-2")),
                        new Entry(
                                findPartImage(sceneNode, "ambient-list2-part-image3"),
                                configSetOf("l2", "l2-3")),
                        new Entry(
                                findPartImage(sceneNode, "ambient-and-active-part-image"),
                                new UserConfigSet(Collections.emptyMap()))),
                drawableNodeConfigTable.getAllEntries());
    }

    @Test
    public void drawableNodeConfigTable_create_handlesNestedLists() throws Exception {
        Node sceneNode = readSceneNode("/NestedLists.xml");

        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(sceneNode, TEST_VARIANT);

        assertEquals(
                ImmutableList.of(
                        new Entry(
                                findPartImage(
                                        sceneNode,
                                        "part-image-list1-option1-list2-option1-list1-option1"),
                                configSetOf("l1", "l1-1", "l2", "l2-1")),
                        new Entry(
                                findPartImage(
                                        sceneNode,
                                        "part-image-list1-option1-list2-option2-list1-option1"),
                                configSetOf("l1", "l1-1", "l2", "l2-2")),
                        new Entry(
                                findPartImage(
                                        sceneNode,
                                        "part-image-list1-option2-list2-option1-list1-option2"),
                                configSetOf("l1", "l1-2", "l2", "l2-1")),
                        new Entry(
                                findPartImage(
                                        sceneNode,
                                        "part-image-list1-option2-list2-option2-list1-option2"),
                                configSetOf("l1", "l1-2", "l2", "l2-2"))),
                drawableNodeConfigTable.getAllEntries());
    }

    @Test
    public void drawableNodeConfigTable_create_recognisesFonts() throws Exception {
        Node sceneNode = readSceneNode("/TTFFont.xml");

        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(sceneNode, TEST_VARIANT);

        assertThat(drawableNodeConfigTable.getAllEntries())
                .isEqualTo(
                        ImmutableList.of(
                                new Entry(
                                        findFontNode(sceneNode, "l1-1-text"),
                                        configSetOf("l1", "l1-1")),
                                new Entry(
                                        findFontNode(sceneNode, "l1-2-text"),
                                        configSetOf("l1", "l1-2")),
                                new Entry(
                                        findFontNode(sceneNode, "l1-3-text"),
                                        configSetOf("l1", "l1-3")),
                                new Entry(
                                        findFontNode(sceneNode, "b1-1-text"),
                                        configSetOf("b1", "TRUE")),
                                new Entry(
                                        findFontNode(sceneNode, "b1-2-text"),
                                        configSetOf("b1", "FALSE")),
                                new Entry(findFontNode(sceneNode, "free-text-2"), configSetOf())));
    }

    @Test
    public void getIndependentDrawableNodes_returnsEntriesWithNoConfigs() throws Exception {
        Node sceneNode = readSceneNode("/DrawableNodeConfigTableTestLayout.xml");
        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(sceneNode, TEST_VARIANT);

        List<Entry> independentNodes = drawableNodeConfigTable.getIndependentDrawableNodes();

        assertEquals(
                ImmutableList.of(
                        new Entry(
                                findPartImage(sceneNode, "ambient-and-active-part-image"),
                                new UserConfigSet(Collections.emptyMap()))),
                independentNodes);
    }

    @Test
    public void getDependentDrawableNodes_returnsEntriesWithConfigs() throws Exception {
        Node sceneNode = readSceneNode("/DrawableNodeConfigTableTestLayout.xml");
        DrawableNodeConfigTable drawableNodeConfigTable =
                DrawableNodeConfigTable.create(sceneNode, TEST_VARIANT);

        List<Entry> dependentNodes = drawableNodeConfigTable.getDependentDrawableNodes();

        assertEquals(
                ImmutableList.of(
                        new Entry(
                                findPartImage(sceneNode, "active-list1-part-image1"),
                                configSetOf("l1", "l1-1")),
                        new Entry(
                                findPartImage(sceneNode, "active-list1-part-image2"),
                                configSetOf("l1", "l1-2")),
                        new Entry(
                                findPartImage(sceneNode, "active-list1-part-image3"),
                                configSetOf("l1", "l1-3"))),
                dependentNodes);
    }

    @Test
    public void entry_matchesConfigSet_emptyUserConfigMatchesAnyReferenceConfigSet() {
        Entry entryWithEmptyConfig = new Entry(null, configSetOf());

        assertTrue(entryWithEmptyConfig.matchesConfigSet(configSetOf("l1", "l1-1")));
    }

    @Test
    public void entry_matchesConfigSet_subsetConfigSetMatchesReferenceConfigSet() {
        Entry entryWithEmptyConfig = new Entry(null, configSetOf("l1", "l1-1"));

        assertTrue(entryWithEmptyConfig.matchesConfigSet(configSetOf("l1", "l1-1", "l2", "l2-1")));
    }

    @Test
    public void entry_matchesConfigSet_exactMatch() {
        Entry entryWithEmptyConfig = new Entry(null, configSetOf("l1", "l1-1", "l2", "l2-1"));

        assertTrue(entryWithEmptyConfig.matchesConfigSet(configSetOf("l1", "l1-1", "l2", "l2-1")));
    }

    @Test
    public void entry_matchesConfigSet_doesNotMatchWhenReferenceIsMissingRequiredConfig() {
        Entry entryWithEmptyConfig = new Entry(null, configSetOf("l1", "l1-1", "l2", "l2-1"));

        assertFalse(entryWithEmptyConfig.matchesConfigSet(configSetOf("l1", "l1-1")));
    }

    @Test
    public void entry_matchesConfigSet_doesNotMatchWhenReferenceHasOtherOptionForSameKey() {
        Entry entryWithEmptyConfig = new Entry(null, configSetOf("l1", "l1-1"));
        assertFalse(entryWithEmptyConfig.matchesConfigSet(configSetOf("l1", "l1-2")));
    }

    private Node readSceneNode(String documentPath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(documentPath)) {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            return findSceneNode(document);
        }
    }

    private Node findFontNode(Node rootNode, String name) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (Node)
                xPath.compile(String.format("//PartText[@name='%s']/Text/Font", name))
                        .evaluate(rootNode, XPathConstants.NODE);
    }

    private Node findPartImage(Node rootNode, String name) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (Node)
                xPath.compile(String.format("//PartImage[@name='%s']", name))
                        .evaluate(rootNode, XPathConstants.NODE);
    }

    private UserConfigSet configSetOf(String... keyValues) {
        Map<UserConfigKey, UserConfigValue> configSetMap = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            configSetMap.put(
                    new UserConfigKey(keyValues[i]), new UserConfigValue(keyValues[i + 1]));
        }
        return new UserConfigSet(configSetMap);
    }
}
