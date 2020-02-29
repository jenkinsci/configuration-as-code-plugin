package io.jenkins.plugins.casc;

import io.jenkins.plugins.analysis.warnings.groovy.GroovyParser;
import io.jenkins.plugins.analysis.warnings.groovy.ParserConfiguration;
import io.jenkins.plugins.casc.misc.ConfiguredWithReadme;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithReadmeRule;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;


/**
 * @author v1v (Victor Martinez)
 */
public class WarningsTest {

    @Rule
    public JenkinsConfiguredWithReadmeRule j = new JenkinsConfiguredWithReadmeRule();

    @Test
    @ConfiguredWithReadme("warnings/README.md")
    public void configure_warnings() throws Exception {
        final ParserConfiguration configuration = ParserConfiguration.getInstance();
        assertNotNull(configuration);
        assertThat(configuration.getParsers(), hasSize(1));
        GroovyParser parser = configuration.getParsers().get(0);
        assertThat(parser.getId(), is("example-id"));
        assertThat(parser.getName(), is("Example parser"));
        assertThat(parser.getRegexp(), is("^\\s*(.*):(\\d+):(.*):\\s*(.*)$"));
        assertThat(parser.getScript(), containsString("import edu.hm.hafner.analysis.Severity"));
        assertThat(parser.getExample(), is("somefile.txt:2:SeriousWarnings:SomethingWentWrong"));
    }
}
