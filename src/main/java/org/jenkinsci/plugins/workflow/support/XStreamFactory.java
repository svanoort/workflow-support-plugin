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

/** Classes that implement this can produce their own configured XStream instances and support pooling of instances to
 *  prevent threads blocking due to internal synchronization when XStream instances are reused. */
public interface XStreamFactory {
    @Nonnull
    public default XStream createXStream() {
        return new XStream2();
    }

    /** Returns a key that uniquely identifies how XStream instances are configured.
     *  Generally the class name itself will be fine, unless you want to share instances with non-inheriting classes. */
    @Nonnull
    public default String getFactoryKey() {
        return this.getClass().getName();
    }
}
