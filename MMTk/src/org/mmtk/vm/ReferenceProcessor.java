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
 * This class manages SoftReferences, WeakReferences, and
 * PhantomReferences.
 */
@Uninterruptible
public abstract class ReferenceProcessor {

  public enum Semantics { SOFT, WEAK, PHANTOM }

  /**
   * Clear the contents of the table. This is called when reference types are
   * disabled to make it easier for VMs to change this setting at runtime.
   */
  public abstract void clear();

  /**
   * Scan through the list of references.
   *
   * @param trace the thread local trace element.
   * @param nursery {@code true} if it is safe to only scan new references.
   * @param retain whether to retain those references whose referents are not
   *  reachable
   */
  public abstract void scan(RefLifecycleTracer trace, boolean nursery, boolean retain);

  /**
   * Iterate over all references and forward.
   *
   * @param trace The MMTk trace to forward to
   * @param nursery The nursery collection hint
   */
  public abstract void forward(RefLifecycleTracer trace, boolean nursery);

  /**
   * @return the number of references objects on the queue
   */
  public abstract int countWaitingReferences();
}
