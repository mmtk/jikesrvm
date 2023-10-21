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
import org.vmmagic.unboxed.Address;

/**
 * Abstract base class for scanning and processing reference
 * objects during garbage collection.
 *
 * Subclasses implement scanning and forwarding logic for
 * different reference types.
 */
@Uninterruptible
public abstract class ReferenceProcessingHelper {

  public enum State {
    INITIAL,
    SOFT_REFS,
    WEAK_REFS,
    FINALIZABLE,
    PHANTOM_REFS
  }

  /**
   * Scans reference objects and enqueues reachable objects.
   *
   * Implementations should scan a specific reference type.
   *
   * @param traceObjectCallback MMTK side `trace_object` callback
   * @param trace               Tracer context
   * @param isNursery           If scanning nursery
   * @param needRetain          If should retain forwarded references
   * @return True to continue scanning
   */
  public abstract boolean scan(Address traceObjectCallback,
      Address tracer,
      boolean isNursery,
      boolean needRetain);

  /**
   * Forwards reference objects to new locations.
   * 
   * Implementations should forward a specific reference type.
   *
   * @param traceObjectCallback MMTK side `trace_object` callback
   * @param trace               Tracer context
   * @param isNursery           If scanning nursery
   */
  public abstract void forward(
      Address traceObjectCallback,
      Address tracer,
      boolean isNursery);

}
