package org.jenkinsci.plugins.casc;

import hudson.model.labels.LabelAtom;
import hudson.plugins.ec2.AmazonEC2Cloud;
import hudson.plugins.ec2.SlaveTemplate;
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

public class EC2CloudTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("EC2Cloud.yml")
    public void configure_ec2_cloud() throws Exception {
        final AmazonEC2Cloud ec2Cloud = (AmazonEC2Cloud) Jenkins.getInstance().getCloud("ec2-ec2");
        assertNotNull(ec2Cloud);

        assertTrue(ec2Cloud.isUseInstanceProfileForCredentials());
        final List<SlaveTemplate> templates = ec2Cloud.getTemplates();
        assertThat(templates, hasSize(1));
        final SlaveTemplate slaveTemplate = templates.get(0);
        assertThat(slaveTemplate.getAmi(), equalTo("ami-0c6bb742864ffa3f3"));

        assertThat(slaveTemplate.getLabelString(), containsString("test"));
        assertThat(slaveTemplate.getLabelString(), containsString("yey"));

        assertThat(slaveTemplate.getLabelSet(), is(notNullValue()));

        // fails here without mode specified
        assertTrue(ec2Cloud.canProvision(new LabelAtom("test")));
    }


}
