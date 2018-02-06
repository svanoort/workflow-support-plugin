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
import hudson.util.XStream2;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a basic pooling implementation for {@link XStream} instances, to avoid lock contention when instances are reused.
 * @author Sam Van Oort
 */
@Restricted(NoExternalUse.class)
public class XStreamPooler<PoolType extends XStreamPooled> {

    static class PoolInstance extends GenericObjectPool<XStream> {
        String key;

        PoolInstance(BaseXStreamMaker factory) {
            super(factory);
            this.key = factory.getFactoryKey();
        }
    }

    PoolInstance myPool;

    XStreamPooler(BaseXStreamMaker factory) {
        myPool = new PoolInstance(factory);
    }

    /** Per-class pools */
    static ConcurrentHashMap<String, XStreamPooler> pools = new ConcurrentHashMap<String, XStreamPooler>();

    /** Used for classes that are {@link XStreamPooled} but not not {@link XStreamCustomizer}s. */
    static XStreamPooler BASE_POOL = new XStreamPooler(new BaseXStreamMaker());

    public static XStreamPooler poolForConsumer(@Nonnull XStreamPooled ob) {
        if (ob instanceof XStreamCustomizer) {
            XStreamCustomizer xc = (XStreamCustomizer)ob;
            String key = xc.getXStreamFactory().getFactoryKey();
            XStreamPooler pool = pools.computeIfAbsent(key, k -> new XStreamPooler(new CustomizedXStreamMaker(xc.getXStreamFactory())));
            return pool;
        }
        return BASE_POOL;
    }

    public void clearPools() {
        BASE_POOL.clearPools();
        for (XStreamPooler pool : pools.values()) {
            pool.clearPools();
        }
    }

    public XStream borrowXStream(@Nonnull PoolType consumer) {
        try {
            return myPool.borrowObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void returnXStream(@Nonnull XStream pooledInstance, @Nonnull PoolType consumer) {
        myPool.returnObject(pooledInstance);
    }

    static class BaseXStreamMaker extends BasePooledObjectFactory<XStream> {

        public String getFactoryKey() {
            return "";
        }

        @Override
        public XStream create() throws Exception {
            return new XStream2();
        }

        @Override
        public PooledObject<XStream> wrap(XStream xStream) {
            return new DefaultPooledObject<XStream> (xStream);
        }
    }

    static class CustomizedXStreamMaker extends BaseXStreamMaker {
        XStreamFactory myCustomFactory;

        CustomizedXStreamMaker(@Nonnull XStreamFactory newFactory) {
            this.myCustomFactory = newFactory;
        }

        public String getFactoryKey() {
            return myCustomFactory.getFactoryKey();
        }

        @Override
        public XStream create() throws Exception {
            return myCustomFactory.createXStream();
        }
    }

}
