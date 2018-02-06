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

import javax.annotation.Nonnull;

/**
 * Implement this and your class can use {@link XStreamPool} to pool and reuse {@link com.thoughtworks.xstream.XStream} instances.
 *
 * <p><strong>Customization of XStream:</strong> if you need customized XStream instances, you can return a customized static {@link XStreamFactory
 *  instance that registers converters, aliases, etc.
 *
 * <p><strong>Implementation note: {@link XStreamPooled} classes MUST define and return static {@link XStreamFactory} instances for pooling to work.</strong>
 * This is because pooling is defined per factory instance, with <em>direct equality comparison</em> for safety.
 *
 * <p>If (for some crazy reason) you want to use the same {@link XStreamFactory} in an unrelated class, you can just forward the {@link #getFactory()} call.
 *
 * @author Sam Van Oort
 */
public interface XStreamPooled {
    /** Static base implementation used when we don't customize the XStream serialization. */
    XStreamFactory BASE_FACTORY = new XStreamFactory();

    /** Returns the factory for this type, which <strong>MUST</strong> be safe to retain in-memory long-term.
     *  In most cases this means providing a lightweight static implementation.
     */
    @Nonnull
    public default XStreamFactory getFactory() {
        return BASE_FACTORY;
    }
}
