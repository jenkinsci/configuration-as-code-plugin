package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;
import io.jenkins.plugins.artifact_manager_jclouds.s3.S3BlobStoreConfig;
import io.jenkins.plugins.aws.global_configuration.CredentialsAwsGlobalConfiguration;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import jenkins.model.ArtifactManagerFactory;
import jenkins.model.ArtifactManagerFactoryDescriptor;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class ArtifactManagerS3Test {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("ArtifactManagerS3.yml")
    public void configure_artifact_manager() throws Exception {
        assertThat(CredentialsAwsGlobalConfiguration.get().getRegion(), is(equalTo("us-east-1")));
        assertThat(S3BlobStoreConfig.get().getPrefix(), is(equalTo("jenkins_data/")));

        final DescriptorExtensionList<ArtifactManagerFactory, ArtifactManagerFactoryDescriptor> artifactManagers = ArtifactManagerFactoryDescriptor.all();
        assertThat(artifactManagers, hasSize(1));

    }
}
