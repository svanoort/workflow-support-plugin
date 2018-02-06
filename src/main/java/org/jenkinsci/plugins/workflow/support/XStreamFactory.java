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

import javax.annotation.Nonnull;

/**
 * Implement this and your class can now use the {@link XStreamPool} to use pooled {@link XStream} instances, to avoid
 *  thread contention when multiple threads try to reuse an instance.
 *
 * <strong>Implementation note:</strong> If you customize instances, i.e. by registering {@link com.thoughtworks.xstream.converters.Converter}s
 *  or aliases you <strong>MUST</strong> override <strong>BOTH</strong> {@link #createXStream()} AND {@link #getFactoryKey()}.
 *
 *  <p> See their JavaDocs for what is needed.
 */
public interface XStreamFactory {

    /** Provides an {@link XStream} instance, customized if needed for this class, such as with converters or aliases.
     *  If you override this method you <strong>MUST</strong> provide a new key for {@link #getFactoryKey()}.
     */
    @Nonnull
    public default XStream createXStream() {
        return new XStream2();
    }

    /** Returns a key unique to the configuration of {@link XStream} instances returned by {@link #createXStream()}.
     *  If you are overriding {@link #createXStream()}, then you need a unique key.
     *
     *  <p>Generally <code>getClass().getName()</code> is fine unless you want to share XStream instances with non-inheriting classes. */
    @Nonnull
    public default String getFactoryKey() {
        return "BASE";
    }
}
