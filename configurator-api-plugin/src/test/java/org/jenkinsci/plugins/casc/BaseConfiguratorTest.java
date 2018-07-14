package org.jenkinsci.plugins.casc;

import org.jenkinsci.plugins.casc.impl.configurators.DataBoundConfiguratorTest;
import org.jenkinsci.plugins.casc.model.Mapping;
import org.jenkinsci.plugins.casc.model.Scalar;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.JenkinsRule;

@For(BaseConfigurator.class)
public class BaseConfiguratorTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test(expected = ConfiguratorException.class)
    public void shouldFailWithoutContext() throws Exception {
        Mapping config = new Mapping();
        config.put("foo", "foo");
        config.put("bar", "true");
        config.put("qix", "123");
        config.put("zot", "DataBoundSetter");
        final DataBoundConfiguratorTest.Foo configured = (DataBoundConfiguratorTest.Foo) Configurator.lookup(DataBoundConfiguratorTest.Foo.class).configure(config);
    }
}
