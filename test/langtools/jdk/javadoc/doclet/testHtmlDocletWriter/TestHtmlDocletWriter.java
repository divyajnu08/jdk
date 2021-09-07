/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/*
 * @test
 * @bug 8248001
 * @library  /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestHtmlDocletWriter
 */

import javadoc.tester.JavadocTester;

public class TestHtmlDocletWriter extends JavadocTester {

    public static void main(String args[]) throws Exception {
        TestHtmlDocletWriter tester = new TestHtmlDocletWriter();
        tester.runTests();
    }

    private void checkHtml(String html) {
        checkOutput(html, true,
                    "<a href=\"ftp://www.domain.com/\">FTP Site</a>");
        checkOutput(html, true,
                    "<a href=\"http://www.domain.com/\">HTTP Site</a>");
        checkOutput(html, true,
                    "<a href=\"https://www.domain.com/\">HTTPS Site</a>");
        checkOutput(html, true,
                    "<a href=\"file:///path/to/somefile\">file service</a>");
        checkOutput(html, true,
                    "<a href=\"mailto:username@hogehoge.com\">mail service</a>");
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "--allow-script-in-comments",
                "-notimestamp",
                "-sourcepath", testSrc,
                "-use",
                "pkg");
        checkExit(Exit.OK);

        checkHtml("pkg/A1.html");
        checkHtml("pkg/package-use.html");
        checkHtml("pkg/class-use/B1.StaticInnerB1.html");
        checkHtml("index-all.html");
    }
}
