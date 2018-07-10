package org.jenkinsci.plugins.casc;

import hudson.model.labels.LabelAtom;
import hudson.plugins.ec2.AMITypeData;
import hudson.plugins.ec2.AmazonEC2Cloud;
import hudson.plugins.ec2.SlaveTemplate;
import hudson.plugins.ec2.UnixData;
import io.jenkins.plugins.artifact_manager_jclouds.s3.S3BlobStoreConfig;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ArtifactManagerS3Test {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("ArtifactManagerS3.yml")
    public void configure_artifact_manager() throws Exception {
        assertThat(S3BlobStoreConfig.get().getRegion(), is(equalTo("us-east-1")));
        assertThat(S3BlobStoreConfig.get().getPrefix(), is(equalTo("jenkins_data/")));

    }
}
