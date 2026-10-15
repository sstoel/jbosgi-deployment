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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.junit.Test;
import org.osgi.framework.Version;

/**
 * {@code Deployment.equals} throws {@code NullPointerException} when the deployment has no symbolic name (a legal
 * state, R3 bundles have none) and the other deployment has one.
 *
 * <h3>Severity</h3>
 * minor. {@code equals} must never throw; collection operations over deployments ({@code List.contains/remove},
 * {@code HashSet} with colliding hash codes) fail with an NPE instead of answering. Deployments are kept in such
 * collections by the framework ({@code BootstrapBundlesInstall}, {@code PersistentBundlesInstallPlugin}) and by the
 * WildFly subsystem ({@code InitialDeploymentTracker} keeps a {@code HashSet<Deployment>}, where equals is only called
 * on a hash collision); no framework code path is known that calls equals with such a pair today.
 *
 * <h3>Component</h3>
 * jbosgi-deployment, {@code org.jboss.osgi.deployment.deployer.AbstractDeployment#equals(Object)} (base of
 * VirtualFileDeployment and NullFileDeployment).
 *
 * <h3>Found by</h3>
 * subrepos-a workstream, review of the Deployment API contracts.
 *
 * <h3>Spec reference</h3>
 * {@code AbstractDeployment} constructor comment "symbolicName can be null" ({@code AbstractDeployment.java:74}) and
 * {@code java.lang.Object#equals(Object)} (Java SE 25): an equivalence relation that is symmetric ("x.equals(y) should
 * return true if and only if y.equals(x) returns true"); the general contract leaves no room for an exception for a
 * non-null argument.
 *
 * <h3>Code location</h3>
 * {@code src/main/java/org/jboss/osgi/deployment/deployer/AbstractDeployment.java:136}:
 * {@code boolean matchName = symbolicName == other.symbolicName || symbolicName.equals(other.symbolicName);}
 * Pre-existing, present on WF41.
 *
 * <h3>Root cause</h3>
 * Null check only by identity; {@code symbolicName.equals(...)} dereferences a null symbolic name.
 *
 * <h3>Reproduction</h3>
 * <pre>
 * cd jbosgi-deployment
 * export JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home
 * mvn -Dmaven.compiler.proc=full test -Dtest=DeploymentEqualsNullSymbolicNameIssueTestCase
 * </pre>
 *
 * <h3>Observed</h3>
 * <pre>
 * testEqualsWithoutSymbolicName: equals of a deployment without symbolic name must return false for a deployment with
 *   one, but it throws java.lang.NullPointerException: Cannot invoke "String.equals(Object)" because "this.symbolicName" is null
 * testListContains: List.contains must answer false, but it throws java.lang.NullPointerException: Cannot invoke
 *   "String.equals(Object)" because "this.symbolicName" is null
 * </pre>
 *
 * <h3>Expected</h3>
 * {@code false} (and the same answer in both directions).
 *
 * <h3>Reference behaviour</h3>
 * n/a.
 *
 * <h3>Suggested fix</h3>
 * {@code boolean matchName = java.util.Objects.equals(symbolicName, other.symbolicName);}
 *
 * <h3>Regression vs WF41</h3>
 * no (pre-existing).
 */
public class DeploymentEqualsNullSymbolicNameIssueTestCase {

    @Test
    public void testEqualsWithoutSymbolicName() {
        Deployment unnamed = DeploymentFactory.createDeployment("location", null, Version.emptyVersion);
        Deployment named = DeploymentFactory.createDeployment("location", "name", Version.emptyVersion);
        assertFalse("Precondition: the other direction answers false", named.equals(unnamed));
        boolean result;
        try {
            result = unnamed.equals(named);
        } catch (RuntimeException ex) {
            fail("equals of a deployment without symbolic name must return false for a deployment with one, but it throws " + ex);
            return;
        }
        assertFalse("equals must be symmetric", result);
    }

    @Test
    public void testListContains() {
        List<Deployment> deployments = new ArrayList<Deployment>();
        deployments.add(DeploymentFactory.createDeployment("location", "name", Version.emptyVersion));
        Deployment unnamed = DeploymentFactory.createDeployment("location", null, Version.emptyVersion);
        try {
            assertFalse(deployments.contains(unnamed));
        } catch (RuntimeException ex) {
            fail("List.contains must answer false, but it throws " + ex);
        }
    }
}
