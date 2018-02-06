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
 * Provides customized {@link XStream} instances that are pooled and reused for {@link XStreamPooled} classes.
 * <p><strong>Implementation note:</strong> instances are retained long-term by pools, so they should be lightweight.
 */
public class XStreamFactory {

    /** Provides an {@link XStream} instance, customized if needed for this class, such as with converters or aliases.
     */
    @Nonnull
    public XStream createXStream() {
        return new XStream2();
    }

    @Override
    public final int hashCode() {
        // Ensures you can't override the default identity hashcode
        return super.hashCode();
    }

    @Override
    public final boolean equals(Object ob) {
        // Ensures we can't break the default equality check, which is object identity
        return super.equals(ob);
    }
}
