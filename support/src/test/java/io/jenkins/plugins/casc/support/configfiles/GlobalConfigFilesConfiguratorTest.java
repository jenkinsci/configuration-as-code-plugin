package io.jenkins.plugins.casc.support.configfiles;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Map.Entry;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorException;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import io.jenkins.plugins.casc.model.Mapping;
import io.jenkins.plugins.casc.model.Scalar;
import io.jenkins.plugins.casc.model.Sequence;

/**
 * @author srempfer
 */
public class GlobalConfigFilesConfiguratorTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("GlobalConfigFilesTest.yml")
    public void verifDescribeConfigs() throws Exception {
        final GlobalConfigFilesConfigurator root = getGlobalConfigFilesConfigurator();
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);

        final CNode rootNode = root.describe(root.getTargetComponent(context), context);
        assertNotNull(rootNode);
        assertThat(rootNode, instanceOf(Mapping.class));

        // check configs
        final CNode configsNode = ((Mapping) rootNode).get("configs");
        assertNotNull(configsNode);
        assertThat(configsNode, instanceOf(Sequence.class));

        Sequence configsSequence =  configsNode.asSequence();
        assertThat(configsSequence, hasSize(4));

        for (CNode configNode : configsSequence) {

            assertNotNull(configNode);
            assertThat(configNode, instanceOf(Mapping.class));
            Mapping configMapping = configNode.asMapping();

            Set<String> keySet = configMapping.keySet();
            assertThat(keySet, hasSize(1));
            String key = keySet.iterator().next();

            CNode configTypeNode = configMapping.get(key);
            assertNotNull(configTypeNode);
            assertThat(configTypeNode, instanceOf(Mapping.class));
            Mapping configTypeMapping = configNode.asMapping();

            // XXX: currently both are allowed xyzConfig and xyz - should this be more strict?
            if ("customConfig".equals(key) || "custom".equals(key)) {
                verifCustomConfig(configTypeMapping.get(key));

            } else if ("jsonConfig".equals(key) || "json".equals(key)) {
                verifJsonConfig(configTypeMapping.get(key));

            } else if ("xmlConfig".equals(key) || "xml".equals(key)) {
                verifXmlConfig(configTypeMapping.get(key));

            } else if ("mavenSettingsConfig".equals(key) || "mavenSettings".equals(key)) {
                verifMavenSettingsConfig(configTypeMapping.get(key));
            }
        }
    }

    private void verifCustomConfig(CNode configValuesNode) throws ConfiguratorException {

        assertNotNull(configValuesNode);
        assertThat(configValuesNode, instanceOf(Mapping.class));
        Mapping configValuesMapping = configValuesNode.asMapping();

        Set<Entry<String, CNode>> entrySet = configValuesMapping.entrySet();
        assertThat(entrySet, hasSize(4));

        for (Entry<String, CNode> entry : entrySet) {
            assertThat(entry.getValue(), instanceOf(Scalar.class));

            String key = entry.getKey();
            String value = entry.getValue().asScalar().getValue();

            if ("id".equals(key)) {
                assertThat(value, equalTo("68168f02-ad9e-4729-b83f-7b59432be774"));

            } else if ("name".equals(key)) {
                assertThat(value, equalTo("DummyCustom1"));

            } else if ("comment".equals(key)) {
                assertThat(value, equalTo("dummy custom 1"));

            } else if ("content".equals(key)) {
                assertThat(value, equalTo("dummy content 1"));
            }
        }
    }

    private void verifJsonConfig(CNode configValuesNode) throws ConfiguratorException {

        assertNotNull(configValuesNode);
        assertThat(configValuesNode, instanceOf(Mapping.class));
        Mapping configValuesMapping = configValuesNode.asMapping();

        Set<Entry<String, CNode>> entrySet = configValuesMapping.entrySet();
        assertThat(entrySet, hasSize(4));

        for (Entry<String, CNode> entry : entrySet) {
            assertThat(entry.getValue(), instanceOf(Scalar.class));

            String key = entry.getKey();
            String value = entry.getValue().asScalar().getValue();

            if ("id".equals(key)) {
                assertThat(value, equalTo("3c9bc804-3ae8-4026-a3a3-1192bf564422"));

            } else if ("name".equals(key)) {
                assertThat(value, equalTo("DummyJsonConfig"));

            } else if ("comment".equals(key)) {
                assertThat(value, equalTo("dummy json config"));

            } else if ("content".equals(key)) {
                //XXX: currently the content is not described with a block quote
                assertThat(value, equalTo("{ \"dummydata\": {\"dummyKey\": \"dummyValue\"} }"));
            }
        }
    }

    private void verifXmlConfig(CNode configValuesNode) throws ConfiguratorException {

        assertNotNull(configValuesNode);
        assertThat(configValuesNode, instanceOf(Mapping.class));
        Mapping configValuesMapping = configValuesNode.asMapping();

        Set<Entry<String, CNode>> entrySet = configValuesMapping.entrySet();
        assertThat(entrySet, hasSize(4));

        for (Entry<String, CNode> entry : entrySet) {
            assertThat(entry.getValue(), instanceOf(Scalar.class));

            String key = entry.getKey();
            String value = entry.getValue().asScalar().getValue();

            if ("id".equals(key)) {
                assertThat(value, equalTo("ef61bcee-506a-47e7-9325-73857d7441a3"));

            } else if ("name".equals(key)) {
                assertThat(value, equalTo("DummyXmlConfig"));

            } else if ("comment".equals(key)) {
                assertThat(value, equalTo("dummy xml config"));

            } else if ("content".equals(key)) {
                assertThat(value, equalTo("<root><dummy test=\"abc\"></dummy></root>"));
            }
        }
    }

    private void verifMavenSettingsConfig(CNode configValuesNode) throws ConfiguratorException {

        assertNotNull(configValuesNode);
        assertThat(configValuesNode, instanceOf(Mapping.class));
        Mapping configValuesMapping = configValuesNode.asMapping();

        Set<Entry<String, CNode>> entrySet = configValuesMapping.entrySet();
        assertThat(entrySet, hasSize(6));

        for (Entry<String, CNode> entry : entrySet) {
            String key = entry.getKey();

            if ("id".equals(key)) {
                assertThat(entry.getValue(), instanceOf(Scalar.class));
                String value = entry.getValue().asScalar().getValue();
                assertThat(value, equalTo("d40b9db5-3369-4c63-968e-290fd6614009"));

            } else if ("name".equals(key)) {
                assertThat(entry.getValue(), instanceOf(Scalar.class));
                String value = entry.getValue().asScalar().getValue();
                assertThat(value, equalTo("DummySettings"));

            } else if ("comment".equals(key)) {
                assertThat(entry.getValue(), instanceOf(Scalar.class));
                String value = entry.getValue().asScalar().getValue();
                assertThat(value, equalTo("dummy settings"));

            } else if ("isReplaceAll".equals(key)) {
                assertThat(entry.getValue(), instanceOf(Scalar.class));
                String value = entry.getValue().asScalar().getValue();
                assertThat(value, equalTo("true"));

            } else if ("content".equals(key)) {
                assertThat(entry.getValue(), instanceOf(Scalar.class));
                String value = entry.getValue().asScalar().getValue();
                //XXX: currently the content is not described with a block quote
                assertThat(value, startsWith("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" \n" +
                        "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));

            } else if ("serverCredentialMappings".equals(key)) {
                assertThat(entry.getValue(), instanceOf(Sequence.class));
                Sequence serverCredentialMappingsSequence = entry.getValue().asSequence();

                verifServerCredentialMappings(serverCredentialMappingsSequence);
            }
        }
    }

    private void verifServerCredentialMappings(Sequence serverCredentialMappingsSequence) throws ConfiguratorException {
        assertThat(serverCredentialMappingsSequence, hasSize(2));

        for (CNode serverCredentialMappingNode : serverCredentialMappingsSequence) {
            assertThat(serverCredentialMappingNode, instanceOf(Mapping.class));
            Mapping serverCredentialMapping = serverCredentialMappingNode.asMapping();

            Set<String> keySet = serverCredentialMapping.keySet();
            assertThat(keySet, hasItems("serverId", "credentialsId"));
            assertThat(keySet, hasSize(2));

            CNode serverIdNode = serverCredentialMapping.get("serverId");
            assertThat(serverIdNode, instanceOf(Scalar.class));
            String serverId = serverIdNode.asScalar().getValue();

            CNode credentialsIdNode = serverCredentialMapping.get("credentialsId");
            assertThat(credentialsIdNode, instanceOf(Scalar.class));
            String credentialsId = credentialsIdNode.asScalar().getValue();

            assertThat(serverId + "|" + credentialsId, anyOf(equalTo("server3|credentialsB"), equalTo("server4|credentialsY")));

        }
    }

    private GlobalConfigFilesConfigurator getGlobalConfigFilesConfigurator() {
        return j.jenkins.getExtensionList(GlobalConfigFilesConfigurator.class).get(0);
    }

}
