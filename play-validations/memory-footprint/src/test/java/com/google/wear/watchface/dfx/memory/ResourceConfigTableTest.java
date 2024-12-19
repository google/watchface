package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.ResourceConfigTable.fromDrawableNodeConfigTable;
import static com.google.wear.watchface.dfx.memory.WatchFaceDocuments.findSceneNode;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;

@RunWith(JUnit4.class)
public class ResourceConfigTableTest {

    private static final EvaluationSettings evaluationSettings = new EvaluationSettings("");
    private static final VariantConfigValue TEST_VARIANT =
            VariantConfigValue.active(evaluationSettings);

    private DrawableNodeConfigTable testDrawableNodeConfigTable;
    private WatchFaceResourceCollector testResourceCollector;
    private VariantConfigValue testVariant;

    @Test
    public void fromDrawableNodeConfigTable_groupsSingleNestedList() throws Exception {
        initialize("/NestedLists.xml");

        List<Set<UserConfigKey>> groupsForDoc =
                fromDrawableNodeConfigTable(
                                testDrawableNodeConfigTable, testResourceCollector, testVariant)
                        .joinRelatedUserConfigKeys();

        assertEquals(
                ImmutableList.of(ImmutableSet.of(new UserConfigKey("l1"), new UserConfigKey("l2"))),
                groupsForDoc);
    }

    @Test
    public void fromDrawableNodeConfigTable_groupsSingleUserConfig() throws Exception {
        initialize("/LinearCombinations.xml");

        List<Set<UserConfigKey>> groupsForDoc =
                fromDrawableNodeConfigTable(
                                testDrawableNodeConfigTable, testResourceCollector, testVariant)
                        .joinRelatedUserConfigKeys();

        assertEquals(
                ImmutableList.of(
                        ImmutableSet.of(new UserConfigKey("l1")),
                        ImmutableSet.of(new UserConfigKey("l2")),
                        ImmutableSet.of(new UserConfigKey("b1"))),
                groupsForDoc);
    }

    @Test
    public void fromDrawableNodeConfigTable_handlesVariantCorrectly_ACTIVE() throws Exception {
        initialize("/NestedVariantWithList.xml", VariantConfigValue.active(evaluationSettings));

        List<Set<UserConfigKey>> groupsForActive =
                fromDrawableNodeConfigTable(
                                testDrawableNodeConfigTable, testResourceCollector, testVariant)
                        .joinRelatedUserConfigKeys();

        assertEquals(ImmutableList.of(ImmutableSet.of(new UserConfigKey("l1"))), groupsForActive);
    }

    @Test
    public void fromDrawableNodeConfigTable_handlesVariantCorrectly_AMBIENT() throws Exception {
        initialize("/NestedVariantWithList.xml", VariantConfigValue.ambient(evaluationSettings));

        List<Set<UserConfigKey>> groupsForAmbient =
                fromDrawableNodeConfigTable(
                                testDrawableNodeConfigTable, testResourceCollector, testVariant)
                        .joinRelatedUserConfigKeys();

        assertEquals(ImmutableList.of(ImmutableSet.of(new UserConfigKey("l2"))), groupsForAmbient);
    }

    @Test
    public void fromDrawableNodeConfigTable_groupsListsUnitedByImage() throws Exception {
        initialize("/MultipleListsReferenceSameImage.xml");

        List<Set<UserConfigKey>> groupsForDoc =
                fromDrawableNodeConfigTable(
                                testDrawableNodeConfigTable, testResourceCollector, testVariant)
                        .joinRelatedUserConfigKeys();

        assertEquals(
                ImmutableList.of(ImmutableSet.of(new UserConfigKey("l1"), new UserConfigKey("l2"))),
                groupsForDoc);
    }

    @Test
    public void fromDrawableNodeConfigTable_handlesTTFs() throws Exception {
        initialize(
                "/TTFFont.xml",
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("list1-font1.ttf", DrawableResourceDetails.builder().build())
                        .put("list1-font2.ttf", DrawableResourceDetails.builder().build())
                        .put("list1-font3.ttf", DrawableResourceDetails.builder().build())
                        .put("bool1-font1.ttf", DrawableResourceDetails.builder().build())
                        .put("bool1-font2.ttf", DrawableResourceDetails.builder().build())
                        .build());

        List<Set<UserConfigKey>> groupsForDoc =
                fromDrawableNodeConfigTable(
                                testDrawableNodeConfigTable, testResourceCollector, testVariant)
                        .joinRelatedUserConfigKeys();

        assertThat(groupsForDoc)
                .containsExactly(
                        ImmutableSet.of(new UserConfigKey("l1")),
                        ImmutableSet.of(new UserConfigKey("b1")));
    }

    private void initialize(
            String watchFacePath, Map<String, DrawableResourceDetails> resourceDetailsMap)
            throws Exception {
        initialize(watchFacePath, TEST_VARIANT, resourceDetailsMap);
    }

    private void initialize(String watchFacePath, VariantConfigValue variant) throws Exception {
        initialize(watchFacePath, variant, Collections.emptyMap());
    }

    private void initialize(String watchFacePath) throws Exception {
        initialize(watchFacePath, TEST_VARIANT, Collections.emptyMap());
    }

    private void initialize(
            String watchFacePath,
            VariantConfigValue variant,
            Map<String, DrawableResourceDetails> resourceDetailsMap)
            throws Exception {
        Document testDocument = readDocument(watchFacePath);
        testDrawableNodeConfigTable =
                DrawableNodeConfigTable.create(findSceneNode(testDocument), variant);
        testResourceCollector =
                new WatchFaceResourceCollector(
                        testDocument, resourceDetailsMap, evaluationSettings);
        testVariant = variant;
    }

    private Document readDocument(String documentPath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(documentPath)) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }
}
