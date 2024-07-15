package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static com.google.wear.watchface.dfx.memory.ResourceConfigTable.findConfigsForResources;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

@RunWith(JUnit4.class)
public class ResourceConfigTableTest {

    private static final EvaluationSettings evaluationSettings = new EvaluationSettings("", "");
    private static final VariantConfigValue TEST_VARIANT =
            VariantConfigValue.active(evaluationSettings);

    @Test
    public void findConfigsForResources_groupsSingleNestedList() throws Exception {
        Document document = readDocument("/NestedLists.xml");

        List<Set<UserConfigKey>> groupsForDoc =
                findConfigsForResources(
                                document,
                                TEST_VARIANT,
                                new HashMap<>(),
                                getTestEvaluationSettings())
                        .joinRelatedUserConfigKeys();

        assertEquals(
                ImmutableList.of(ImmutableSet.of(new UserConfigKey("l1"), new UserConfigKey("l2"))),
                groupsForDoc);
    }

    @Test
    public void findConfigsForResources_groupsSingleUserConfig() throws Exception {
        Document document = readDocument("/LinearCombinations.xml");

        List<Set<UserConfigKey>> groupsForDoc =
                findConfigsForResources(
                                document,
                                TEST_VARIANT,
                                new HashMap<>(),
                                getTestEvaluationSettings())
                        .joinRelatedUserConfigKeys();

        assertEquals(
                ImmutableList.of(
                        ImmutableSet.of(new UserConfigKey("l1")),
                        ImmutableSet.of(new UserConfigKey("l2")),
                        ImmutableSet.of(new UserConfigKey("b1"))),
                groupsForDoc);
    }

    @Test
    public void findConfigsForResources_handlesVariantCorrectly_ACTIVE() throws Exception {
        Document document = readDocument("/NestedVariantWithList.xml");

        List<Set<UserConfigKey>> groupsForActive =
                findConfigsForResources(
                                document,
                                VariantConfigValue.active(evaluationSettings),
                                new HashMap<>(),
                                getTestEvaluationSettings())
                        .joinRelatedUserConfigKeys();

        assertEquals(ImmutableList.of(ImmutableSet.of(new UserConfigKey("l1"))), groupsForActive);
    }

    @Test
    public void findConfigsForResources_handlesVariantCorrectly_AMBIENT() throws Exception {
        Document document = readDocument("/NestedVariantWithList.xml");

        List<Set<UserConfigKey>> groupsForAmbient =
                findConfigsForResources(
                                document,
                                VariantConfigValue.ambient(evaluationSettings),
                                new HashMap<>(),
                                getTestEvaluationSettings())
                        .joinRelatedUserConfigKeys();

        assertEquals(ImmutableList.of(ImmutableSet.of(new UserConfigKey("l2"))), groupsForAmbient);
    }

    @Test
    public void findConfigsForResources_groupsListsUnitedByImage() throws Exception {
        Document document = readDocument("/MultipleListsReferenceSameImage.xml");
        List<Set<UserConfigKey>> groupsForDoc =
                findConfigsForResources(
                                document,
                                TEST_VARIANT,
                                new HashMap<>(),
                                getTestEvaluationSettings())
                        .joinRelatedUserConfigKeys();

        assertEquals(
                ImmutableList.of(ImmutableSet.of(new UserConfigKey("l1"), new UserConfigKey("l2"))),
                groupsForDoc);
    }

    @Test
    public void findConfigsForResources_handlesTTFs() throws Exception {
        Document document = readDocument("/TTFFont.xml");
        Map<String, DrawableResourceDetails> resourceDetailsMap =
                ImmutableMap.<String, DrawableResourceDetails>builder()
                        .put("list1-font1.ttf", DrawableResourceDetails.builder().build())
                        .put("list1-font2.ttf", DrawableResourceDetails.builder().build())
                        .put("list1-font3.ttf", DrawableResourceDetails.builder().build())
                        .put("bool1-font1.ttf", DrawableResourceDetails.builder().build())
                        .put("bool1-font2.ttf", DrawableResourceDetails.builder().build())
                        .build();
        List<Set<UserConfigKey>> groupsForDoc =
                findConfigsForResources(
                                document,
                                TEST_VARIANT,
                                resourceDetailsMap,
                                getTestEvaluationSettings())
                        .joinRelatedUserConfigKeys();

        assertThat(groupsForDoc)
                .containsExactly(
                        ImmutableSet.of(new UserConfigKey("l1")),
                        ImmutableSet.of(new UserConfigKey("b1")));
    }

    private Document readDocument(String documentPath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(documentPath)) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

    private static EvaluationSettings getTestEvaluationSettings() {
        return new EvaluationSettings("", "");
    }
}
