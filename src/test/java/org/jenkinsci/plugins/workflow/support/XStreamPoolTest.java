package org.jenkinsci.plugins.workflow.support;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author Sam Van Oort
 */
public class XStreamPoolTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();


    @Test
    public void testPoolShutdown() throws Exception {
        XStreamPool pool = XStreamPool.get();
        pool.shutdown();
        for (XStreamPool.PoolInstance p : pool.pools.values()) {
            Assert.assertTrue("Found a pool left unclosed closed", p.isClosed());
        }
    }

    @Test
    public void testIdle() throws Exception {
        XStreamPool pool = XStreamPool.get();

    }
}
