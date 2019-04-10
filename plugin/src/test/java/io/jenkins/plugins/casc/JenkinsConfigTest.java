package io.jenkins.plugins.casc;

import hudson.security.csrf.CrumbIssuer;
import hudson.security.csrf.CrumbIssuerDescriptor;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import java.io.ByteArrayOutputStream;
import javax.servlet.ServletRequest;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.RuleChain;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JenkinsConfigTest {


    @Rule
    public RuleChain chain = RuleChain.outerRule( new EnvironmentVariables()
            .set("CASC_JENKINS_CONFIG", getClass().getResource("JenkinsConfigTest.yml").toExternalForm()))
            .around(new JenkinsConfiguredWithCodeRule());


    @Test
    public void loadFromCASC_JENKINS_CONFIG() {
        Jenkins j = Jenkins.getInstance();
        assertEquals("configuration as code - JenkinsConfigTest", j.getSystemMessage());
        assertEquals(10, j.getQuietPeriod());
    }


    @Test
    public void shouldExportEvenOnError() throws Exception {
        Jenkins j = Jenkins.getInstance();
        j.setCrumbIssuer(new BrokenCrumbIssuer("bar"));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConfigurationAsCode.get().export(out);
        final String s = out.toString();
        System.out.println(s);

    }


    // This class is broken : no getter for property "foo"
    @Symbol("broken")
    public static class BrokenCrumbIssuer extends CrumbIssuer {

        @DataBoundConstructor
        public BrokenCrumbIssuer(String foo) {

        }

        @Override
        protected String issueCrumb(ServletRequest request, String salt) {
            return null;
        }

        @Override
        public boolean validateCrumb(ServletRequest request, String salt, String crumb) {
            return false;
        }

        @Override
        public CrumbIssuerDescriptor<CrumbIssuer> getDescriptor() {
            return null;
        }
    }
}
