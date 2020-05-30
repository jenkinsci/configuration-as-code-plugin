package io.jenkins.plugins.casc;

import com.amazonaws.services.ec2.model.InstanceType;
import hudson.model.labels.LabelAtom;
import hudson.plugins.ec2.AMITypeData;
import hudson.plugins.ec2.AmazonEC2Cloud;
import hudson.plugins.ec2.SlaveTemplate;
import hudson.plugins.ec2.UnixData;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import java.util.List;
import jenkins.model.Jenkins;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EC2CloudTest {

    @Rule
    public RuleChain chain = RuleChain.outerRule(new EnvironmentVariables()
        .set("EC2_PRIVATE_KEY", "ADMIN123"))
        .around(new JenkinsConfiguredWithReadmeRule());

    @Test
    @ConfiguredWithReadme("ec2/README.md")
    public void configure_ec2_cloud() throws Exception {
        final AmazonEC2Cloud ec2Cloud = (AmazonEC2Cloud) Jenkins.get().getCloud("ec2-ec2");
        assertNotNull(ec2Cloud);

        assertTrue(ec2Cloud.isUseInstanceProfileForCredentials());
        assertThat(ec2Cloud.getPrivateKey().getPrivateKey(), is("ADMIN123"));
        final List<SlaveTemplate> templates = ec2Cloud.getTemplates();
        assertThat(templates, hasSize(2));

        SlaveTemplate slaveTemplate = templates.get(0);
        assertThat(slaveTemplate.getDisplayName(), containsString("Auto configured EC2 Agent Small"));
        assertFalse(slaveTemplate.getAssociatePublicIp());
        assertFalse(slaveTemplate.isConnectBySSHProcess());
        assertFalse(slaveTemplate.deleteRootOnTermination);
        assertFalse(slaveTemplate.ebsOptimized);
        assertFalse(slaveTemplate.monitoring);
        assertFalse(slaveTemplate.stopOnTerminate);
        assertFalse(slaveTemplate.getUseDedicatedTenancy());
        assertFalse(slaveTemplate.useEphemeralDevices);
        assertThat(slaveTemplate.type, is(InstanceType.T2Small));
        assertThat(slaveTemplate.getAmi(), equalTo("ami-0c6bb742864ffa3f3"));
        assertThat(slaveTemplate.getLabelString(), containsString("Small"));
        assertThat(slaveTemplate.getLabelSet(), is(notNullValue()));
        assertThat(slaveTemplate.remoteFS, equalTo("/home/ec2-user"));
        assertThat(slaveTemplate.getRemoteAdmin(), equalTo("ec2-user"));
        assertThat(slaveTemplate.zone, equalTo("us-east-1"));
        assertThat(slaveTemplate.getSecurityGroupString(), equalTo("some-group"));

        // fails here without mode specified
        assertTrue(ec2Cloud.canProvision(new LabelAtom("Small")));

        // Checks that the AMI type is Unix and configured
        AMITypeData amiType = slaveTemplate.getAmiType();
        assertTrue(amiType.isUnix());
        assertTrue(amiType instanceof UnixData);
        UnixData unixData = (UnixData) amiType;
        assertThat(unixData.getRootCommandPrefix(), equalTo("sudo"));
        assertThat(unixData.getSlaveCommandPrefix(), equalTo("sudo -u jenkins"));
        assertThat(unixData.getSshPort(), equalTo("61120"));


        slaveTemplate = templates.get(1);
        assertThat(slaveTemplate.getDisplayName(), containsString("Auto configured EC2 Agent Large"));
        assertFalse(slaveTemplate.getAssociatePublicIp());
        assertFalse(slaveTemplate.isConnectBySSHProcess());
        assertFalse(slaveTemplate.deleteRootOnTermination);
        assertFalse(slaveTemplate.ebsOptimized);
        assertFalse(slaveTemplate.monitoring);
        assertFalse(slaveTemplate.stopOnTerminate);
        assertFalse(slaveTemplate.getUseDedicatedTenancy());
        assertFalse(slaveTemplate.useEphemeralDevices);
        assertThat(slaveTemplate.type, is(InstanceType.T2Xlarge));
        assertThat(slaveTemplate.getAmi(), equalTo("ami-0c6bb742864ffa3f3"));
        assertThat(slaveTemplate.getLabelString(), containsString("Large"));
        assertThat(slaveTemplate.getLabelSet(), is(notNullValue()));
        assertThat(slaveTemplate.remoteFS, equalTo("/home/ec2-user"));
        assertThat(slaveTemplate.getRemoteAdmin(), equalTo("ec2-user"));
        assertThat(slaveTemplate.zone, equalTo("us-east-1"));
        assertThat(slaveTemplate.getSecurityGroupString(), equalTo("some-group"));

        // fails here without mode specified
        assertTrue(ec2Cloud.canProvision(new LabelAtom("Large")));

        // Checks that the AMI type is Unix and configured
        amiType = slaveTemplate.getAmiType();
        assertTrue(amiType.isUnix());
        assertTrue(amiType instanceof UnixData);
        unixData = (UnixData) amiType;
        assertThat(unixData.getRootCommandPrefix(), equalTo("sudo"));
        assertThat(unixData.getSlaveCommandPrefix(), equalTo("sudo -u jenkins"));
        assertThat(unixData.getSshPort(), equalTo("61120"));
    }
}
