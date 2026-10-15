/*
 * #%L
 * JBossOSGi Deployment
 * %%
 * Copyright (C) 2010 - 2026 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.test.osgi.deployment.issues;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.jboss.osgi.deployment.interceptor.AbstractInvocationContext;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptorService;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptor;
import org.jboss.osgi.spi.AttachableSupport;
import org.jboss.osgi.spi.AttachmentKey;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The lifecycle interceptor chain can place an interceptor before the interceptor that produces its input when the
 * inputs form a chain of three or more interceptors; the consumer then never runs, because
 * {@code handleStateChange} skips an interceptor whose input attachment is missing.
 *
 * <h3>Severity</h3>
 * minor. {@code LifecycleInterceptor} is public API: bundles register interceptors as services and the framework's
 * {@code LifecycleInterceptorServiceImpl} (and through it the WildFly web integration) builds the chain with this
 * class. Whether the chain is wrong depends on the {@code HashSet} iteration order, i.e. on the interceptors' hash codes
 * (identity hash codes by default), so it happens at random between runs: in a fuzz run (random acyclic input/output
 * graphs of 2-6 interceptors with orders 500/1000/2000, identity hash codes, 20000 rounds, JDK 25) 307 chains placed an
 * interceptor before the producer of one of its inputs. This test pins one such iteration order with fixed hash codes.
 * Only chains of input-requiring interceptors are affected (2 interceptors are always ordered correctly, see the
 * existing InterceptorOrderTestCase).
 *
 * <h3>Component</h3>
 * jbosgi-deployment, {@code org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptorService#addInterceptor}
 * and {@code #addWithRelativeOrder}.
 *
 * <h3>Found by</h3>
 * subrepos-a workstream, fuzzing of the interceptor ordering (dependency invariant: every input that some interceptor
 * of the chain outputs is output by an earlier interceptor).
 *
 * <h3>Spec reference</h3>
 * The method's javadoc ({@code AbstractLifecycleInterceptorService.java:105-112}): "Add a LifecycleInterceptor to the
 * service. The interceptor is added according to its input requirements and relative order." and
 * {@code LifecycleInterceptor#getInput()}: "Get the required set of inputs."; {@code handleStateChange} invokes an
 * interceptor only "if all required input is available" ({@code :245-256}).
 *
 * <h3>Code location</h3>
 * {@code src/main/java/org/jboss/osgi/deployment/interceptor/AbstractLifecycleInterceptorService.java:119-160} on
 * WF41-osgi-r8: the "remaining" interceptors (inputs not produced by an input-less interceptor) are inserted one by
 * one in HashSet order with {@code addWithRelativeOrder} ({@code :178-200}), which appends an interceptor whose
 * producer is not in the list yet; when the producer is inserted later the consumer is not moved behind it.
 * Pre-existing (2009/2010), present on WF41.
 *
 * <h3>Root cause</h3>
 * Insertion sort without a topological order: an element placed before its producer exists is never revisited.
 *
 * <h3>Reproduction</h3>
 * <pre>
 * cd jbosgi-deployment
 * export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
 * mvn -Dmaven.compiler.proc=full test -Dtest=InterceptorChainInputOrderIssueTestCase
 * </pre>
 *
 * <h3>Observed</h3>
 * <pre>
 * testConsumerAfterProducer: Every interceptor must come after the producer of its inputs, but the chain is
 *   [R(out=k0), X(in=k0,out=k1), B(in=k1,out=k2), C(in=k3,out=k4), D(in=k2,out=k3)]
 * testEveryInterceptorInvoked: All interceptors must be invoked when each one's input is produced by another one,
 *   but the invoked ones are [R, X, B, D] (C was skipped: its input k3 is produced after it)
 * </pre>
 *
 * <h3>Expected</h3>
 * {@code [R, X, B, D, C]}; all five interceptors invoked.
 *
 * <h3>Reference behaviour</h3>
 * n/a (jboss-osgi specific API).
 *
 * <h3>Suggested fix</h3>
 * Build the chain with a topological sort over input/output dependencies (Kahn's algorithm), choosing among the ready
 * interceptors by relative order (and a stable tie breaker instead of HashSet order).
 *
 * <h3>Regression vs WF41</h3>
 * no (pre-existing).
 */
public class InterceptorChainInputOrderIssueTestCase {

    static final AttachmentKey<K0> KEY0 = AttachmentKey.create(K0.class);
    static final AttachmentKey<K1> KEY1 = AttachmentKey.create(K1.class);
    static final AttachmentKey<K2> KEY2 = AttachmentKey.create(K2.class);
    static final AttachmentKey<K3> KEY3 = AttachmentKey.create(K3.class);
    static final AttachmentKey<K4> KEY4 = AttachmentKey.create(K4.class);

    @Test
    public void testConsumerAfterProducer() {
        TestService service = createService(new ArrayList<String>());
        List<LifecycleInterceptor> chain = service.chain();
        List<AttachmentKey<?>> produced = new ArrayList<AttachmentKey<?>>();
        boolean ordered = true;
        for (LifecycleInterceptor aux : chain) {
            if (aux.getInput() != null && !produced.containsAll(aux.getInput()))
                ordered = false;
            produced.addAll(aux.getOutput());
        }
        assertTrue("Every interceptor must come after the producer of its inputs, but the chain is " + chain, ordered);
    }

    @Test
    public void testEveryInterceptorInvoked() {
        List<String> invoked = new ArrayList<String>();
        TestService service = createService(invoked);
        service.handleStateChange(Bundle.STARTING, mock(Bundle.class));
        assertTrue("All interceptors must be invoked when each one's input is produced by another one, but the invoked ones are " + invoked,
                invoked.size() == 5);
    }

    // R -> X -> B -> D -> C (data flow). The hash codes fix the HashSet iteration order to C, D, B, X, R,
    // one of the orders that identity hash codes produce at random.
    private static TestService createService(List<String> invoked) {
        TestService service = new TestService();
        service.add(new TestInterceptor("C", 1, invoked, KEY4, KEY3));
        service.add(new TestInterceptor("D", 2, invoked, KEY3, KEY2));
        service.add(new TestInterceptor("B", 3, invoked, KEY2, KEY1));
        service.add(new TestInterceptor("X", 4, invoked, KEY1, KEY0));
        service.add(new TestInterceptor("R", 5, invoked, KEY0, null));
        return service;
    }

    static class TestInterceptor extends AbstractLifecycleInterceptor {
        private final String name;
        private final int hash;
        private final List<String> invoked;
        private final AttachmentKey<?> out;

        TestInterceptor(String name, int hash, List<String> invoked, AttachmentKey<?> out, AttachmentKey<?> in) {
            this.name = name;
            this.hash = hash;
            this.invoked = invoked;
            this.out = out;
            addOutput(out);
            if (in != null)
                addInput(in);
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void invoke(int state, InvocationContext context) {
            invoked.add(name);
            context.putAttachment((AttachmentKey) out, Boolean.TRUE);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return name + "(" + (getInput() != null ? "in=" + keyName(getInput().iterator().next()) + "," : "") + "out=" + keyName(out) + ")";
        }

        private static String keyName(AttachmentKey<?> key) {
            return key.getType().getSimpleName().toLowerCase();
        }
    }

    static class TestService extends AbstractLifecycleInterceptorService {
        void add(LifecycleInterceptor interceptor) {
            addInterceptor(interceptor);
        }

        List<LifecycleInterceptor> chain() {
            return new ArrayList<LifecycleInterceptor>(getInterceptorChain());
        }

        @Override
        protected InvocationContext getInvocationContext(Bundle bundle) {
            return new AbstractInvocationContext(mock(BundleContext.class), bundle, null, new AttachableSupport());
        }
    }

    static <T> T mock(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("equals".equals(method.getName()))
                    return proxy == args[0];
                if ("hashCode".equals(method.getName()))
                    return System.identityHashCode(proxy);
                if ("toString".equals(method.getName()))
                    return "mock " + type.getSimpleName();
                return null;
            }
        }));
    }

    static class K0 {
    }

    static class K1 {
    }

    static class K2 {
    }

    static class K3 {
    }

    static class K4 {
    }
}
