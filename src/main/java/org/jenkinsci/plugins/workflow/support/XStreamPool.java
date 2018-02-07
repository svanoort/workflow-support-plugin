/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.workflow.support;

import com.thoughtworks.xstream.XStream;
import hudson.Extension;
import hudson.init.Terminator;
import jenkins.model.Jenkins;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a basic pooling implementation for {@link XStream} instances, to avoid lock contention when instances are reused.
 * @author Sam Van Oort
 */
@Extension
public final class XStreamPool {

    static Logger LOGGER = Logger.getLogger(XStreamPool.class.getName());

    static GenericObjectPoolConfig BASE_CONFIG = new GenericObjectPoolConfig();

    static final AbandonedConfig ABANDONED_CONFIG = new AbandonedConfig();

    static final long EVICTION_RUN__MILLIS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);;
    static final long EVICTION_IDLE_MILLIS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    static final long ABANDONED_TIMEOUT = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);

    static {
        BASE_CONFIG.setMaxTotal(-1); // Handle unlimited scaling
        BASE_CONFIG.setMinIdle(-1);
        BASE_CONFIG.setMaxIdle(-1);
        BASE_CONFIG.setBlockWhenExhausted(true);
        BASE_CONFIG.setMaxWaitMillis(1000L);
        BASE_CONFIG.setMinEvictableIdleTimeMillis(EVICTION_IDLE_MILLIS);
        BASE_CONFIG.setTimeBetweenEvictionRunsMillis(EVICTION_RUN__MILLIS);
        BASE_CONFIG.setJmxEnabled(false);

        ABANDONED_CONFIG.setLogAbandoned(false);
        ABANDONED_CONFIG.setRemoveAbandonedTimeout((int)ABANDONED_TIMEOUT);
        ABANDONED_CONFIG.setRemoveAbandonedOnMaintenance(true);
        ABANDONED_CONFIG.setUseUsageTracking(false);  // May wish to re-enable later for diagnostics of pool leaks
    }



    @Restricted(NoExternalUse.class)
    public XStreamPool(){

    }


    static class PoolInstance extends GenericObjectPool<XStream> {  // Not private so we can touch it for testing

        PoolInstance(@Nonnull XStreamFactory factory) {
            super(new XStreamPoolFactoryShim(factory), BASE_CONFIG, ABANDONED_CONFIG);
        }
    }

    ConcurrentHashMap<XStreamFactory, PoolInstance> pools = new ConcurrentHashMap<XStreamFactory, PoolInstance>();

    /** Obtain an {@link XStream} instance from the pools for this poolable type. */
    public XStream borrowXStream(@Nonnull XStreamPooled consumer) {
        try {
            return poolForConsumer(consumer).borrowObject();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error obtaining XStream instance", ex);
            throw new RuntimeException(ex);
        }
    }

    /** Return an {@link XStream} instance to the pool. */
    public void returnXStream(@Nonnull XStream pooledInstance, @Nonnull XStreamPooled consumer) {
        poolForConsumer(consumer).returnObject(pooledInstance);
    }

    /** Empties pools of all their instances. */
    public void clearPools() {
        for (PoolInstance pool : pools.values()) {
            pool.clear();
        }
    }

    /** Return the instance */
    @Nonnull
    public static XStreamPool get() {
        return Jenkins.getActiveInstance().getExtensionList(XStreamPool.class).get(0);
    }

    PoolInstance poolForConsumer(@Nonnull XStreamPooled consumer) { // Package-scoped for tests
        XStreamFactory factoryInstance = consumer.getFactory();
        PoolInstance pool = pools.computeIfAbsent(factoryInstance, k -> new PoolInstance(factoryInstance));
        return pool;
    }

    /** Wraps an {@link XStreamFactory} so you can use it as a {@link org.apache.commons.pool2.PooledObjectFactory}. */
    private static class XStreamPoolFactoryShim extends BasePooledObjectFactory<XStream> {
        XStreamFactory myCustomFactory;

        XStreamPoolFactoryShim(@Nonnull XStreamFactory newFactory) {
            this.myCustomFactory = newFactory;
        }

        public XStream create() throws Exception {
            return myCustomFactory.createXStream();
        }

        public PooledObject<XStream> wrap(XStream xStream) {
            return new DefaultPooledObject<XStream> (xStream);
        }
    }

    @Terminator
    public void shutdown() {
        for (PoolInstance pool : pools.values()) {
            if (!pool.isClosed()) {
                try {
                    pool.close();
                } catch (Exception ex) {
                    // Who cares?
                }
            }
        }
    }

}
