package com.google.wear.watchface.dfx.memory;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class WatchFaceResourceCollectorTest {

    @Test
    public void collectResources_collectsExpectedPartTextWithBitmapFontResources()
            throws Exception {
        Document watchFaceDocument = readDocument("/CollectResourcesTestCases.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node partTextNode =
                getNodeByXpath(
                        watchFaceDocument,
                        "/WatchFace/Scene/Group[@name='with-simple-bitmap-font']/PartText");

        Set<String> partTextResources = watchFaceResourceCollector.collectResources(partTextNode);

        assertThat(partTextResources).containsExactly("char_a_bmp", "word_asd_bmp");
    }

    @Test
    public void collectResources_collectsExpectedPartTextWithInlineImage() throws Exception {
        Document watchFaceDocument = readDocument("/CollectResourcesTestCases.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node partTextWithInlineImageNode =
                getNodeByXpath(
                        watchFaceDocument,
                        "/WatchFace/Scene/Group[@name='with-inline-image']/PartText");

        Set<String> partTextResources =
                watchFaceResourceCollector.collectResources(partTextWithInlineImageNode);

        assertThat(partTextResources).containsExactly("char_a_bmp", "word_asd_bmp", "inline-image");
    }

    @Test
    public void collectResources_collectsExpectedDigitalClockImages() throws Exception {
        Document watchFaceDocument = readDocument("/CollectResourcesTestCases.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node digitalClockNode =
                getNodeByXpath(
                        watchFaceDocument,
                        "/WatchFace/Scene/Group[@name='with-digital-clock']/DigitalClock");

        Set<String> digitalClockResources =
                watchFaceResourceCollector.collectResources(digitalClockNode);

        assertThat(digitalClockResources)
                .containsExactly(
                        "char_0_bmp",
                        "char_1_bmp",
                        "char_2_bmp",
                        "char_3_bmp",
                        "char_4_bmp",
                        "char_5_bmp",
                        "char_6_bmp",
                        "char_7_bmp",
                        "char_8_bmp",
                        "char_9_bmp",
                        "char_:_bmp",
                        "word_00_bmp");
    }

    @Test
    public void collectResources_throwsTestFailedExceptionOnMissingFont() throws Exception {
        Document watchFaceDocument = readDocument("/CollectResourcesTestCases.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node nodeWithMissingFont =
                getNodeByXpath(
                        watchFaceDocument,
                        "/WatchFace/Scene/Group[@name='with-missing-font']/PartText");

        assertThrows(
                TestFailedException.class,
                () -> watchFaceResourceCollector.collectResources(nodeWithMissingFont));
    }

    @Test
    public void collectResources_returnsAllResourcesFromWatchFaceWithPartImages() throws Exception {
        Document watchFaceDocument = readDocument("/LinearCombinationsWithVariant.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node sceneNode = getNodeByXpath(watchFaceDocument, "/WatchFace/Scene");

        Set<String> allResources = watchFaceResourceCollector.collectResources(sceneNode);

        assertThat(allResources)
                .containsExactly(
                        "image-active",
                        "list1-image1",
                        "list1-image2",
                        "list1-image3",
                        "list2-image1",
                        "list2-image2",
                        "list2-image3",
                        "boolean-image-true",
                        "boolean-image-false");
    }

    @Test
    public void collectResources_collectsAnalogHandsResources() throws Exception {
        Document watchFaceDocument = readDocument("/CollectResourcesTestCases.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node sceneNode =
                getNodeByXpath(
                        watchFaceDocument,
                        "/WatchFace/Scene/Group[@name='analog-clock']/AnalogClock");

        Set<String> analogClockResources = watchFaceResourceCollector.collectResources(sceneNode);

        assertThat(analogClockResources).containsExactly("hour-hand", "minute-hand", "second-hand");
    }

    @Test
    public void collectResources_skipsSecondsFromAnalogHands() throws Exception {
        Document watchFaceDocument = readDocument("/CollectResourcesTestCases.xml");
        WatchFaceResourceCollector watchFaceResourceCollector =
                new WatchFaceResourceCollector(
                        watchFaceDocument, new HashMap<>(), getTestEvaluationSettings());
        Node sceneNode =
                getNodeByXpath(
                        watchFaceDocument,
                        "/WatchFace/Scene/Group[@name='analog-clock']/AnalogClock");

        Set<String> analogClockResources =
                watchFaceResourceCollector.collectResources(
                        sceneNode, VariantConfigValue.ambient(getTestEvaluationSettings()));

        assertThat(analogClockResources).containsExactly("hour-hand", "minute-hand");
    }

    private Document readDocument(String documentPath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(documentPath)) {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        }
    }

    private Node getNodeByXpath(Document document, String xpath) throws Exception {
        XPathExpression xPathExpression = XPathFactory.newInstance().newXPath().compile(xpath);
        return (Node) xPathExpression.evaluate(document, XPathConstants.NODE);
    }

    private static EvaluationSettings getTestEvaluationSettings() {
        return new EvaluationSettings("");
    }
}
