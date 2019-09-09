package io.jenkins.plugins.casc;

import hudson.model.labels.LabelAtom;
import hudson.plugins.ec2.AMITypeData;
import hudson.plugins.ec2.AmazonEC2Cloud;
import hudson.plugins.ec2.SlaveTemplate;
import hudson.plugins.ec2.UnixData;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;

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
        final AmazonEC2Cloud ec2Cloud = (AmazonEC2Cloud) Jenkins.get().getCloud("ec2-ec2");
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

    @Test
    @ConfiguredWithCode("EC2CloudAMIType.yml")
    public void configure_ec2_cloud_with_custom_ami_type() throws Exception {
        final AmazonEC2Cloud ec2Cloud = (AmazonEC2Cloud) Jenkins.get().getCloud("ec2-ec2");
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

        // Checks that the AMI type is Unix and configured
        AMITypeData amiType = slaveTemplate.getAmiType();
        assertTrue(amiType.isUnix());
        assertTrue(amiType instanceof UnixData);
        UnixData unixData = (UnixData) amiType;
        assertThat(unixData.getRootCommandPrefix(), equalTo("sudo"));
        assertThat(unixData.getSlaveCommandPrefix(), equalTo("sudo -u jenkins"));
        assertThat(unixData.getSshPort(), equalTo("61120"));
    }

}
