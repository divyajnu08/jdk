/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.reflect;

import jdk.internal.vm.annotation.DontInline;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Hidden;
import jdk.internal.vm.annotation.Stable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static jdk.internal.reflect.MethodHandleAccessorFactory.SPECIALIZED_PARAM_COUNT;
import static jdk.internal.reflect.MethodHandleAccessorFactory.newMethodHandleInvoker;

class DirectConstructorAccessorImpl extends ConstructorAccessorImpl {
    static ConstructorAccessorImpl constructorAccessor(Constructor<?> ctor, MethodHandle target) {
        if (ReflectionFactory.noInflation()) {
            // fast invoker
            var mhInvoker = newMethodHandleInvoker(ctor, target);
            return new DirectConstructorAccessorImpl(ctor, target);
        } else {
            // Default is the adaptive accessor method.
            return new AdaptiveConstructorAccessor(ctor, target);
        }
    }

    static ConstructorAccessorImpl nativeAccessor(Constructor<?> ctor) {
        return new NativeAccessor(ctor);
    }

    protected final Constructor<?> ctor;
    protected final int paramCount;

    @Stable protected final MethodHandle target;
    @Stable protected final MHInvoker invoker;
    DirectConstructorAccessorImpl(Constructor<?> ctor, MethodHandle target) {
        this.ctor = ctor;
        this.paramCount = ctor.getParameterCount();
        this.target = target;
        this.invoker = new MHInvokerDelegate(target);
    }

    @ForceInline
    MHInvoker mhInvoker() {
        return invoker;
    }

    @Override
    public Object newInstance(Object[] args) throws InstantiationException, InvocationTargetException {
        int argc = args != null ? args.length : 0;
        // only check argument count for specialized forms
        if (paramCount <= SPECIALIZED_PARAM_COUNT && argc != paramCount) {
            throw new IllegalArgumentException("wrong number of arguments: " + argc + " expected: " + paramCount);
        }
        try {
            return invokeImpl(args);
        } catch (ClassCastException|WrongMethodTypeException e) {
            if (isIllegalArgument(e))
                throw new IllegalArgumentException("argument type mismatch", e);
            else
                throw new InvocationTargetException(e);
        } catch (NullPointerException e) {
            if (isIllegalArgument(e))
                throw new IllegalArgumentException(e);
            else
                throw new InvocationTargetException(e);
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    private boolean isIllegalArgument(RuntimeException ex) {
        return AccessorUtils.isIllegalArgument(DirectConstructorAccessorImpl.class, ex);
    }

    @Hidden
    @ForceInline
    Object invokeImpl(Object[] args) throws Throwable {
        var mhInvoker = mhInvoker();
        return switch (paramCount) {
            case 0 -> mhInvoker.invoke();
            case 1 -> mhInvoker.invoke(args[0]);
            case 2 -> mhInvoker.invoke(args[0], args[1]);
            case 3 -> mhInvoker.invoke(args[0], args[1], args[2]);
            default -> mhInvoker.invoke(args);
        };
    }

    static class AdaptiveConstructorAccessor extends DirectConstructorAccessorImpl {
        private @Stable MHInvoker fastInvoker;
        private int numInvocations;
        AdaptiveConstructorAccessor(Constructor<?> ctor, MethodHandle target) {
            super(ctor, target);
        }

        @ForceInline
        MHInvoker mhInvoker() {
            var invoker = fastInvoker;
            if (invoker != null) {
                return invoker;
            }
            return slowInvoker();
        }

        @DontInline
        private MHInvoker slowInvoker() {
            var invoker = this.invoker;
            if (++numInvocations > ReflectionFactory.inflationThreshold()) {
                fastInvoker = invoker = newMethodHandleInvoker(ctor, target);
            }
            return invoker;
        }
    }

    /**
     * Invoke the constructor via native VM reflection
     */
    static class NativeAccessor extends ConstructorAccessorImpl {
        private final Constructor<?> ctor;
        NativeAccessor(Constructor<?> ctor) {
            this.ctor = ctor;
        }

        @Override
        public Object newInstance(Object[] args) throws InstantiationException, InvocationTargetException {
            return newInstance0(ctor, args);
        }
        private static native Object newInstance0(Constructor<?> c, Object[] args)
                    throws InstantiationException, InvocationTargetException;
    }
}
