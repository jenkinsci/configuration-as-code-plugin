package io.jenkins.plugins.casc;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;
import org.kohsuke.stapler.RequestImpl;
import org.kohsuke.stapler.ResponseImpl;

import java.io.IOException;

public class SchemaGeneratorTest {

    @Test
    public void schemaShouldSucceed() throws IOException {

        SchemaGenerator schemaGenerator = new SchemaGenerator();
        System.out.println(schemaGenerator.generateSchema());

    }
}
