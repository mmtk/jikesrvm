/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.rustgc;

import org.jikesrvm.junit.runners.RequiresBootstrapVM;
import org.jikesrvm.junit.runners.RequiresBuiltJikesRVM;
import org.jikesrvm.junit.runners.VMRequirements;
import org.jikesrvm.runtime.SysCall;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import static org.jikesrvm.junit.runners.VMRequirements.isRunningOnBuiltJikesRVM;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(VMRequirements.class)
public class SysCallTest {

  @Test
  @Category(RequiresBuiltJikesRVM.class)
  public void testThatRequiresJikesRVM() {
    assertTrue(isRunningOnBuiltJikesRVM());
  }

  @Test
  @Category(RequiresBootstrapVM.class)
  public void testThatRequiresBootstrapVM() {
    assertFalse(isRunningOnBuiltJikesRVM());
  }

  @Test
  public void testWithNoRequirements() {
    assertTrue(true);
  }

  @Test
  @Category(RequiresBuiltJikesRVM.class)
  public void testSyscalls() {
    for (int i = 0; i < 4; i++) {
      int a = (int) (Math.random() * 50);
      int b = (int) (Math.random() * 50);
      int c = (int) (Math.random() * 50);
      int d = (int) (Math.random() * 50);
      int e = (int) (Math.random() * 50);
      int result = SysCall.sysCall.sysTestStackAlignment1(a, b, c, d, e);
      assertTrue("Returned incorrect result on syscall parameter passing. Returned " + result
                      + " when expecting " + (a + 2 * b + 3 * c + 4 * d + 5 * e)
              ,result == a + 2 * b + 3 * c + 4 * d + 5 * e);
    }
  }
}
