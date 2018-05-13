package org.jenkinsci.plugins.casc;

import org.csanchez.jenkins.plugins.kubernetes.KubernetesCloud;
import org.csanchez.jenkins.plugins.kubernetes.PodTemplate;
import org.csanchez.jenkins.plugins.kubernetes.volumes.HostPathVolume;
import org.csanchez.jenkins.plugins.kubernetes.volumes.PodVolume;
import org.jenkinsci.plugins.casc.misc.ConfiguredWithCode;
import org.jenkinsci.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class KubernetesCloudTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("KubernetesCloudTest.yml")
    public void configure_kubernetes_cloud() throws Exception {
        final KubernetesCloud cloud = j.jenkins.clouds.get(KubernetesCloud.class);
        assertNotNull(cloud);
        assertEquals("kubernetes", cloud.name);
        assertEquals("serverUrl", cloud.getServerUrl());
        assertEquals("serverCertificate", cloud.getServerCertificate());
        assertTrue(cloud.isSkipTlsVerify());
        assertEquals("namespace", cloud.getNamespace());
        assertEquals("jenkinsUrl", cloud.getJenkinsUrl());
        assertEquals("jenkinsTunnel", cloud.getJenkinsTunnel());
        assertEquals(42, cloud.getContainerCap());
        assertEquals(5, cloud.getRetentionTimeout());
        assertEquals(10, cloud.getConnectTimeout());
        assertEquals(20, cloud.getReadTimeout());

        final List<PodTemplate> templates = cloud.getTemplates();
        assertEquals(1, templates.size());
        final PodTemplate template = templates.get(0);
        assertEquals("test", template.getName());
        assertEquals("serviceAccount", template.getServiceAccount());
        assertEquals(1234, template.getInstanceCap());
        assertEquals("label", template.getLabel());

        final List<PodVolume> volumes = template.getVolumes();
        assertEquals(1, volumes.size());
        final PodVolume volume = volumes.get(0);
        assertTrue(volume instanceof HostPathVolume);
        assertEquals("mountPath", volume.getMountPath());
        assertEquals("hostPath", ((HostPathVolume)volume).getHostPath());
    }
}

