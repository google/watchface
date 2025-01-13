package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class VariantConfigValueTest {
    private static final EvaluationSettings TEST_SETTINGS = new EvaluationSettings("");
    private static final VariantConfigValue AMBIENT = VariantConfigValue.ambient(TEST_SETTINGS);
    private static final VariantConfigValue ACTIVE = VariantConfigValue.active(TEST_SETTINGS);

    @Test
    public void ambient_isNodeSkipped_skipsNodeWithAlphaEqualTo0() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "always_disabled");

        assertThat(AMBIENT.isNodeSkipped(group)).isTrue();
    }

    @Test
    public void active_isNodeSkipped_skipsNodeWithAlphaEqualTo0() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "always_disabled");

        assertThat(ACTIVE.isNodeSkipped(group)).isTrue();
    }

    @Test
    public void ambient_isNodeSkipped_doesNotSkipNodeWithoutAlpha() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "always_enabled_no_alpha");

        assertThat(AMBIENT.isNodeSkipped(group)).isFalse();
    }

    @Test
    public void active_isNodeSkipped_doesNotSkipNodeWithoutAlpha() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "always_enabled_no_alpha");

        assertThat(ACTIVE.isNodeSkipped(group)).isFalse();
    }

    @Test
    public void ambient_isNodeSkipped_doesNotSkipNodeDisabledInActive() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "disabled_in_active");

        assertThat(AMBIENT.isNodeSkipped(group)).isFalse();
    }

    @Test
    public void active_isNodeSkipped_skipsNodeDisabledInActive() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "disabled_in_active");

        assertThat(ACTIVE.isNodeSkipped(group)).isTrue();
    }

    @Test
    public void ambient_isNodeSkipped_skipsNodeDisabledInAmbient() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "disabled_in_ambient");

        assertThat(AMBIENT.isNodeSkipped(group)).isTrue();
    }

    @Test
    public void active_isNodeSkipped_doesNotSkipNodeDisabledInAmbient() throws Exception {
        Document watchFaceDocument = readDocument("/VariantSkippingNodes.xml");
        Node group = getGroupByName(watchFaceDocument, "disabled_in_ambient");

        assertThat(ACTIVE.isNodeSkipped(group)).isFalse();
    }

    @Test
    public void active_isNodeSkipped_ambientConditionExpression() throws Exception {
        Document watchFaceDocument = readDocument("/AmbientCondition.xml");
        Node enabledInAmbient =
                getInnerGroupByName(watchFaceDocument, "Compare", "enabled_in_ambient")
                        .getParentNode();
        Node enabledInActive =
                getInnerGroupByName(watchFaceDocument, "Default", "enabled_in_active")
                        .getParentNode();

        assertThat(ACTIVE.isNodeSkipped(enabledInAmbient)).isTrue();
        assertThat(ACTIVE.isNodeSkipped(enabledInActive)).isFalse();
    }

    @Test
    public void ambient_isNodeSkipped_ambientConditionExpression() throws Exception {
        Document watchFaceDocument = readDocument("/AmbientCondition.xml");
        Node enabledInAmbient =
                getInnerGroupByName(watchFaceDocument, "Compare", "enabled_in_ambient")
                        .getParentNode();
        Node enabledInActive =
                getInnerGroupByName(watchFaceDocument, "Default", "enabled_in_active")
                        .getParentNode();

        assertThat(AMBIENT.isNodeSkipped(enabledInAmbient)).isFalse();
        assertThat(AMBIENT.isNodeSkipped(enabledInActive)).isTrue();
    }

    @Test
    public void isNodeSkipped_nonAmbientConditionExpression() throws Exception {
        Document watchFaceDocument = readDocument("/IsDeviceLockedCondition.xml");
        Node enabledWhenLocked =
                getInnerGroupByName(watchFaceDocument, "Compare", "enabled_when_locked")
                        .getParentNode();
        Node enabledWhenUnlocked =
                getInnerGroupByName(watchFaceDocument, "Default", "enabled_when_unlocked")
                        .getParentNode();

        // Non-ambient expressions should not be skipped.
        assertThat(ACTIVE.isNodeSkipped(enabledWhenLocked)).isFalse();
        assertThat(AMBIENT.isNodeSkipped(enabledWhenLocked)).isFalse();
        assertThat(ACTIVE.isNodeSkipped(enabledWhenUnlocked)).isFalse();
        assertThat(AMBIENT.isNodeSkipped(enabledWhenUnlocked)).isFalse();
    }

    private Document readDocument(String documentName) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(documentName)) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

    private Node getGroupByName(Document document, String groupName) throws Exception {
        XPathExpression xPathExpression =
                XPathFactory.newInstance()
                        .newXPath()
                        .compile(String.format("/WatchFace/Scene/Group[@name='%s']", groupName));
        return (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
    }

    private Node getInnerGroupByName(Document document, String parentName, String groupName)
            throws Exception {
        XPathExpression xPathExpression =
                XPathFactory.newInstance()
                        .newXPath()
                        .compile(
                                String.format(
                                        "/WatchFace/Scene/Group[@name='group1']"
                                                + "/Condition/%s/Group[@name='%s']",
                                        parentName, groupName));
        return (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
    }
}
