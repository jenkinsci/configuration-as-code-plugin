package io.jenkins.plugins.casc.plugins;

import hudson.PluginWrapper;
import jenkins.util.xml.XMLUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

@Restricted(NoExternalUse.class)
public class MavenExporter {

    private static final XPathFactory X_PATH_FACTORY = XPathFactory.newInstance();

    private static String getAttribute(final Attributes attributes, final String name) {
        final String value = attributes.getValue(name);
        return value == null ? "" : value;
    }

    static ExtendedDependency exportPlugin(final PluginWrapper pluginWrapper) {
        final ExtendedDependency dep = new ExtendedDependency();
        final Manifest manifest = pluginWrapper.getManifest();
        final Attributes attributes = manifest.getMainAttributes();

        dep.setGroupId(attributes.getValue("Group-Id"));
        dep.setArtifactId(pluginWrapper.getShortName());
        dep.setVersion(pluginWrapper.getVersion());

        dep.setLongName(getAttribute(attributes, "Long-Name"));
        dep.setDescription(getAttribute(attributes, "Specification-Title"));
        dep.setUrl(getAttribute(attributes, "Url"));
        dep.setJenkinsVersion(getAttribute(attributes, "Jenkins-Version"));
        dep.setPluginDependencies(getAttribute(attributes, "Plugin-Dependencies"));

        return dep;
    }

    static Document exportPlugins(final List<PluginWrapper> plugins, final Reader reader)
            throws IOException, SAXException, XPathExpressionException {
        final Document doc = XMLUtils.parse(reader);
        final XPath xPath = X_PATH_FACTORY.newXPath();
        final XPathExpression expression = xPath.compile("/project/dependencies");
        final Node dependenciesNode = (Node) expression.evaluate(doc, XPathConstants.NODE);
        for (final PluginWrapper plugin : plugins) {
            final ExtendedDependency extendedDependency = exportPlugin(plugin);
            final Element dependencyNode = doc.createElement("dependency");

            final String template = " %s\r\n" +
                    "Long Name: %s\r\n" +
                    "Description: %s\r\n" +
                    "Dependencies: [%s]\r\n" +
                    "URL: %s\r\n";
            final String comment = String.format(template, extendedDependency.getArtifactId(),
                    extendedDependency.getLongName(),
                    extendedDependency.getDescription(),
                    extendedDependency.getPluginDependencies(),
                    extendedDependency.getUrl());
            final Comment commentNode = doc.createComment(comment);
            dependencyNode.appendChild(commentNode);

            createElement(dependencyNode, "groupId", extendedDependency.getGroupId());
            createElement(dependencyNode, "artifactId", extendedDependency.getArtifactId());
            createElement(dependencyNode, "version", extendedDependency.getVersion());

            dependenciesNode.appendChild(dependencyNode);
        }
        return doc;
    }

    static void createElement(final Element destination, final String name, final String text) {
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

}
