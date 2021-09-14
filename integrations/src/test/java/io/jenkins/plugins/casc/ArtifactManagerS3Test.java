package io.jenkins.plugins.casc;

import hudson.DescriptorExtensionList;
import io.jenkins.plugins.artifact_manager_jclouds.s3.S3BlobStoreConfig;
import io.jenkins.plugins.aws.global_configuration.CredentialsAwsGlobalConfiguration;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
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
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme(value = "artifact-manager-s3/README.md")
    public void configure_artifact_manager() {
        assertThat(CredentialsAwsGlobalConfiguration.get().getRegion(), is(equalTo("us-east-1")));
        assertThat(S3BlobStoreConfig.get().getPrefix(), is(equalTo("jenkins_data/")));

        final DescriptorExtensionList<ArtifactManagerFactory, ArtifactManagerFactoryDescriptor> artifactManagers = ArtifactManagerFactoryDescriptor.all();
        assertThat(artifactManagers, hasSize(1));

    }
}
