package io.jenkins.plugins.casc;

import org.junit.Assert;
import org.junit.Test;

public class ReportTest {

    @Test
    public void test() {
        // dummy test to ensure jacoco data execution
        String actual = "world";
        Dummy world = new Dummy(actual);
        Assert.assertEquals(world.getHello(), actual);
        Assert.assertTrue(true);
    }

}
