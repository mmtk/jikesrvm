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
package org.mmtk.vm;

import org.mmtk.plan.RefLifecycleTracer;
import org.vmmagic.pragma.Uninterruptible;

/**
 * This class manages finalizable objects.
 */
@Uninterruptible
public abstract class FinalizableProcessor {

  /**
   * Clear the contents of the table. This is called when finalization is
   * disabled to make it easier for VMs to change this setting at runtime.
   */
  public abstract void clear();

  /**
   * Scan through the list of references.
   *
   * @param trace the thread local trace element.
   * @param nursery {@code true} if it is safe to only scan new references.
   */
  public abstract void scan(RefLifecycleTracer trace, boolean nursery);

  /**
   * Iterates over and forward entries in the table.
   *
   * @param trace the trace to use for the processing of the references
   * @param nursery if {@code true}, scan only references generated since
   *  last scan. Otherwise, scan all references.
   */
  public abstract void forward(RefLifecycleTracer trace, boolean nursery);
}
