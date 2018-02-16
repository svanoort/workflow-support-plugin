package org.jenkinsci.plugins.workflow.support;

import com.thoughtworks.xstream.XStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Sam Van Oort
 */
public class XStreamPoolTest {
    @Rule
    public JenkinsRule rule = new JenkinsRule();

    /** Verifies that XStream pooling returns different pools for different {@link XStreamFactory} instances and configures pools right. */
    @Test
    public void testPoolBasics() throws Exception {
        XStreamPool pool = XStreamPool.get();
        BogusPooled pooledType = new BogusPooled();
        XStream xs = pool.borrowXStream(pooledType);
        Assert.assertNotNull(xs);

        Assert.assertNotEquals(pool.poolForConsumer(new XStreamPooled() {}), pool.poolForConsumer(pooledType));

        XStreamPool.PoolInstance instance =  pool.poolForConsumer(pooledType);
        int idleStart = instance.getNumIdle();
        pool.returnXStream(xs, pooledType);
        Assert.assertEquals(idleStart+1, instance.getNumIdle());

        // Ensures config propagated right
        Assert.assertEquals(XStreamPool.BASE_CONFIG.getMaxTotal(), instance.getMaxTotal());
        Assert.assertEquals(XStreamPool.BASE_CONFIG.getMinIdle(), instance.getMinIdle());
        Assert.assertEquals(XStreamPool.BASE_CONFIG.getMaxIdle(), instance.getMaxIdle());
        Assert.assertEquals(XStreamPool.BASE_CONFIG.getMaxWaitMillis(), instance.getMaxWaitMillis());
        Assert.assertEquals(XStreamPool.BASE_CONFIG.getMinEvictableIdleTimeMillis(), instance.getMinEvictableIdleTimeMillis());
        Assert.assertEquals(XStreamPool.BASE_CONFIG.getTimeBetweenEvictionRunsMillis(), instance.getTimeBetweenEvictionRunsMillis());

        Assert.assertEquals(XStreamPool.ABANDONED_CONFIG.getRemoveAbandonedTimeout(), instance.getRemoveAbandonedTimeout());
    }

    @Test
    public void testPoolCreateShutdown() throws Exception {
        XStreamPool pool = XStreamPool.get();
        pool.shutdown();
        for (XStreamPool.PoolInstance p : pool.pools.values()) {
            Assert.assertTrue("Found a pool left unclosed", p.isClosed());
        }
    }

    /** Distinct factory instances so it does not get just the default */
    static final class BogusPooled implements XStreamPooled {
        static XStreamFactory MY_FAC = new XStreamFactory();

        public  XStreamFactory getFactory() {
            return MY_FAC;
        }
    }

    /** Ensure we dispose unused pool instances so they don't leak indefinitely */
    @Test
    public void testAbandonedEntryRemoval() throws Exception {
        XStreamPool pool = XStreamPool.get();

        // Hacky setup to ensure we can run in the time we need
        XStreamPool.BASE_CONFIG.setMinEvictableIdleTimeMillis(1000);
        XStreamPool.BASE_CONFIG.setTimeBetweenEvictionRunsMillis(100);
        XStreamPool.ABANDONED_CONFIG.setRemoveAbandonedTimeout(100);
        pool.clearPools();
        pool.pools.clear();

        try {
            final int threadCount = 10;  // Twice Timer pool size to ensure we end up creating at least one new thread
            final CountDownLatch startLatch = new CountDownLatch(threadCount+1);
            final BogusPooled poolUser = new BogusPooled();
            List<Thread> forked = new ArrayList<Thread>();

            for (int i=0; i<threadCount; i++) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startLatch.countDown();
                            startLatch.await();
                            XStream xs = XStreamPool.get().borrowXStream(poolUser);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            throw new RuntimeException(ex);
                        }
                    }
                };
                Thread t = new Thread(r);
                forked.add(t);
                t.start();
            }
            startLatch.countDown();
            for (Thread t : forked) {
                t.join();
            }
            XStreamPool.PoolInstance instance = pool.poolForConsumer(poolUser);
            Assert.assertEquals(threadCount, instance.getNumActive());
            Thread.sleep(XStreamPool.BASE_CONFIG.getMinEvictableIdleTimeMillis()*3);

            // FIXME figure out why the entries are not evicted?
            Assert.assertEquals(0, instance.getNumActive());
        } finally {  // Reset the pool setup
            XStreamPool.BASE_CONFIG.setMinEvictableIdleTimeMillis(XStreamPool.EVICTION_IDLE_MILLIS);
            XStreamPool.BASE_CONFIG.setTimeBetweenEvictionRunsMillis(XStreamPool.EVICTION_RUN__MILLIS);
            XStreamPool.ABANDONED_CONFIG.setRemoveAbandonedTimeout((int)XStreamPool.ABANDONED_TIMEOUT);
        }

    }
}
