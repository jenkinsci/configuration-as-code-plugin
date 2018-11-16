package io.jenkins.plugins.casc.plugins;

import hudson.PluginWrapper;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

@Restricted(NoExternalUse.class)
public class MavenExporter {

    private final static Logger LOGGER = LogManager.getLogManager().getLogger(MavenExporter.class.getName());

    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();

    private static String getAttribute(final Attributes attributes, final String name) {
        final String value = attributes.getValue(name);
        return value == null ? "" : value;
    }

    static String determineArtifactVersion(final String implementationVersion, final String pluginVersion) {
        if (implementationVersion == null || implementationVersion.length() == 0) {
            return pluginVersion;
        }
        return implementationVersion;
    }

    static ExtendedDependency exportPlugin(final PluginWrapper pluginWrapper) {
        final ExtendedDependency dep = new ExtendedDependency();
        final Manifest manifest = pluginWrapper.getManifest();
        final Attributes attributes = manifest.getMainAttributes();
        final String implementationVersion = getAttribute(attributes, "Implementation-Version");
        final String pluginVersion = pluginWrapper.getVersion();
        final String artifactVersion = determineArtifactVersion(implementationVersion, pluginVersion);

        dep.setGroupId(attributes.getValue("Group-Id"));
        dep.setArtifactId(pluginWrapper.getShortName());
        dep.setVersion(artifactVersion);

        dep.setExtendedVersion(pluginVersion);
        dep.setLongName(pluginWrapper.getLongName());
        dep.setDescription(getAttribute(attributes, "Specification-Title"));
        dep.setUrl(pluginWrapper.getUrl());
        dep.setJenkinsVersion(pluginWrapper.getRequiredCoreVersion());
        dep.setPluginDependencies(getAttribute(attributes, "Plugin-Dependencies"));

        return dep;
    }

    public static void exportPlugins(final List<PluginWrapper> plugins, final Writer writer)
            throws XPathExpressionException, IOException, SAXException, TransformerException {
        final Document document = exportPlugins(plugins);
        writeDocument(document, writer);
    }

    static Document exportPlugins(final List<PluginWrapper> plugins)
            throws IOException, XPathExpressionException, SAXException {
        try(
            final InputStream inputStream = openResourceStream(MavenExporter.class, "pom.xml");
            final Reader reader = new InputStreamReader(inputStream, "UTF-8");
            ) {
            return exportPlugins(plugins, reader);
        }
    }

    static Document exportPlugins(final List<PluginWrapper> plugins, final Reader reader)
            throws IOException, SAXException, XPathExpressionException {
        final Document doc = parse(reader);
        final XPath xPath = X_PATH_FACTORY.newXPath();
        final XPathExpression expression = xPath.compile("/project/dependencies");
        final Node dependenciesNode = (Node) expression.evaluate(doc, XPathConstants.NODE);
        for (final PluginWrapper plugin : plugins) {
            final ExtendedDependency extendedDependency = exportPlugin(plugin);
            final Element dependencyNode = doc.createElement("dependency");

            final String template = " %s%n" +
                    "Long Name: %s%n" +
                    "Long Version: %s%n" +
                    "Description: %s%n" +
                    "Dependencies: [%s]%n" +
                    "URL: %s%n";
            final String comment = String.format(template, extendedDependency.getArtifactId(),
                    extendedDependency.getLongName(),
                    extendedDependency.getExtendedVersion(),
                    extendedDependency.getDescription(),
                    extendedDependency.getPluginDependencies(),
                    extendedDependency.getUrl());
            final Comment commentNode = doc.createComment(comment);
            dependencyNode.appendChild(commentNode);

            addElementTo(dependencyNode, "groupId", extendedDependency.getGroupId());
            addElementTo(dependencyNode, "artifactId", extendedDependency.getArtifactId());
            addElementTo(dependencyNode, "version", extendedDependency.getVersion());

            dependenciesNode.appendChild(dependencyNode);
        }
        return doc;
    }

    private static Document parse(final Reader reader) throws SAXException, IOException {
        DocumentBuilder docBuilder;

        try {
            docBuilder = newDocumentBuilderFactory().newDocumentBuilder();
            docBuilder.setEntityResolver(RESTRICTIVE_ENTITY_RESOLVER_INSTANCE);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unexpected error creating DocumentBuilder.", e);
        }

        return docBuilder.parse(new InputSource(reader));
    }

    private final static RestrictiveEntityResolver RESTRICTIVE_ENTITY_RESOLVER_INSTANCE = new RestrictiveEntityResolver();

    /**
     * An EntityResolver that will fail to resolve any entities.
     * Useful in preventing External XML Entity injection attacks.
     */
    @Restricted(NoExternalUse.class)
    private static final class RestrictiveEntityResolver implements EntityResolver {


        private RestrictiveEntityResolver() {
            // prevent multiple instantiation.
            super();
        }

        /**
         * Throws a SAXException if this tried to resolve any entity.
         */
        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            throw new SAXException("Refusing to resolve entity with publicId(" + publicId + ") and systemId (" + systemId + ")");
        }
    }


    private static DocumentBuilderFactory newDocumentBuilderFactory() {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        // Set parser features to prevent against XXE etc.
        // Note: setting only the external entity features on DocumentBuilderFactory instance
        // (ala how safeTransform does it for SAXTransformerFactory) does seem to work (was still
        // processing the entities - tried Oracle JDK 7 and 8 on OSX). Setting seems a bit extreme,
        // but looks like there's no other choice.
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);
        setDocumentBuilderFactoryFeature(documentBuilderFactory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setDocumentBuilderFactoryFeature(documentBuilderFactory, "http://xml.org/sax/features/external-general-entities", false);
        setDocumentBuilderFactoryFeature(documentBuilderFactory, "http://xml.org/sax/features/external-parameter-entities", false);
        setDocumentBuilderFactoryFeature(documentBuilderFactory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        return documentBuilderFactory;
    }

    private static void setDocumentBuilderFactoryFeature(final DocumentBuilderFactory documentBuilderFactory, String feature, boolean state) {
        try {
            documentBuilderFactory.setFeature(feature, state);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("Failed to set the XML Document Builder factory feature %s to %s", feature, state), e);
        }
    }

    private static void addElementTo(final Element destination, final String name, final String text) {
        final Document doc = destination.getOwnerDocument();
        final Element element = doc.createElement(name);
        final Text textNode = doc.createTextNode(text);
        element.appendChild(textNode);
        destination.appendChild(element);
    }

    static InputStream openResourceStream(final Class clazz, final String fileName) {
        final Package aPackage = clazz.getPackage();
        final String name = aPackage.getName();
        final String resourceFolder = name.replace('.', '/');
        final String resourcePath = resourceFolder + "/" + fileName;
        final ClassLoader loader = clazz.getClassLoader();
        //noinspection UnnecessaryLocalVariable
        final InputStream result = loader.getResourceAsStream(resourcePath);
        return result;
    }

    static void writeDocument(final Document document, final Writer writer) throws TransformerException {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Transformer transformer = tf.newTransformer();
        final DOMSource source = new DOMSource(document);
        final StreamResult result = new StreamResult(writer);
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);
    }
}
