/*
 * Copyright (c) 2018 Oleg Nenashev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to
 * whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.casc.misc;

import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.AccessControlled;
import hudson.security.Permission;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Provides asserts for {@link hudson.security.Permission} checks.
 * @author Oleg Nenashev
 */
public class PermissionAssert {

    public static void assertHasPermission(User user, final Permission permission, final AccessControlled ... items) {
        for (AccessControlled item : items) {
            assertPermission(user, item, permission);
        }
    }

    public static void assertHasPermission(User user, final AccessControlled item, final Permission ... permissions) {
        for (Permission permission : permissions) {
            assertPermission(user, item, permission);
        }
    }

    public static void assertHasNoPermission(User user, final Permission permission, final AccessControlled ... items) {
        for (AccessControlled item : items) {
            assertNoPermission(user, item, permission);
        }
    }

    public static void assertHasNoPermission(User user, final AccessControlled item, final Permission ... permissions) {
        for (Permission permission : permissions) {
            assertNoPermission(user, item, permission);
        }
    }

    private static void assertPermission(User user, final AccessControlled item, final Permission p) {
        assertThat("User '" + user + "' has no " + p.getId() + " permission for " + item + ", but it should according to security settings",
                hasPermission(user, item, p), equalTo(true));
    }

    private static void assertNoPermission(User user, final AccessControlled item, final Permission p) {
        assertThat("User '" + user + "' has the " + p.getId() + " permission for " + item + ", but it should not according to security settings",
                hasPermission(user, item, p), equalTo(false));
    }

    private static boolean hasPermission(User user, final AccessControlled item, final Permission p) {
        try (ACLContext c = ACL.as(user)) {
            return item.hasPermission(p);
        }
    }
}
