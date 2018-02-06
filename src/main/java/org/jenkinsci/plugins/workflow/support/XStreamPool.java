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
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a basic pooling implementation for {@link XStream} instances, to avoid lock contention when instances are reused.
 * @author Sam Van Oort
 */
public final class XStreamPool<FactoryType extends XStreamFactory> {

    private PoolInstance wrappedPool;

    private static class PoolInstance extends GenericObjectPool<XStream> {

        PoolInstance(XStreamFactory factory) {
            super(new XStreamPoolFactoryShim(factory));
        }
    }

    private XStreamPool(XStreamFactory factory) {
        wrappedPool = new PoolInstance(factory);
    }

    private static ConcurrentHashMap<XStreamFactory, XStreamPool> pools = new ConcurrentHashMap<XStreamFactory, XStreamPool>();

    /** Obtain an {@link XStream} instance from the pools for this poolable type. */
    public static XStream borrowXStream(@Nonnull XStreamPooled consumer) {
        try {
            return poolForConsumer(consumer).wrappedPool.borrowObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /** Return an {@link XStream} instance to the pool. */
    public static void returnXStream(@Nonnull XStream pooledInstance, @Nonnull XStreamPooled consumer) {
        poolForConsumer(consumer).wrappedPool.returnObject(pooledInstance);
    }

    /** Empties pools of all their instances. */
    public static void clearPools() {
        for (XStreamPool pool : pools.values()) {
            pool.clearPools();
        }
    }

    private static XStreamPool poolForConsumer(@Nonnull XStreamPooled consumer) {
        XStreamFactory factoryInstance = consumer.getFactory();
        XStreamPool pool = pools.computeIfAbsent(factoryInstance, k -> new XStreamPool(factoryInstance));
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

}
