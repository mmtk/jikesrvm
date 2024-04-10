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
package org.jikesrvm.mm.mmtk;

import static org.jikesrvm.runtime.UnboxedSizeConstants.LOG_BYTES_IN_ADDRESS;

import org.jikesrvm.VM;
import org.jikesrvm.mm.mminterface.Selected;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.util.Services;
import org.mmtk.plan.RefLifecycleTracer;
import org.vmmagic.pragma.NoInline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.Unpreemptible;
import org.vmmagic.pragma.UnpreemptibleNoWarn;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.AddressArray;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.Word;

/**
 * ReferenceProcessingHelper manages scanning and processing of different
 * reference
 * types during garbage collection. It acts as a router to delegate
 * scanning to internal reference processors based on the current phase.
 */
@Uninterruptible
public final class ReferenceProcessingHelper extends org.mmtk.vm.ReferenceProcessingHelper {

  /********************************************************************
   * Class fields
   */

  /** The ReferenceProcessingHelper singleton */
  private static final ReferenceProcessingHelper referenceProcessingHelper = new ReferenceProcessingHelper();

  /**
   * The current scanning phase.
   */
  private static State stateId;

  private RustTraceLocal traceLocal;

  /**
   * Create a new table.
   */
  protected ReferenceProcessingHelper() {
    stateId = State.INITIAL;
    traceLocal = new RustTraceLocal();
  }

  /**
   * Forwards references to new locations during a compacting GC.
   * 
   * @param trace     The tracer context
   * @param isNursery True if scanning the nursery
   */
  @Override
  @Uninterruptible
  public void forward(Address traceObjectCallback, Address tracer, boolean isNursery) {
    if (stateId == State.INITIAL) {
      traceLocal.setContext(traceObjectCallback, tracer);
      org.mmtk.vm.VM.finalizableProcessor.forward(traceLocal, isNursery);
    }
  }

  /**
   * Scans references and traces reachable objects.
   * Goes through each phase in turn, delegating scanning to the
   * appropriate processor.
   *
   * @param trace      The tracer context
   * @param isNursery  True if scanning the nursery
   * @param needRetain Whether to retain forwarded references
   * @return True to continue scanning or false if finished
   */
  @Override
  @Uninterruptible
  public boolean scan(Address traceObjectCallback, Address tracer, boolean isNursery, boolean needRetain) {
    traceLocal.setContext(traceObjectCallback, tracer);

    if (stateId == State.INITIAL) {
      stateId = State.SOFT_REFS;
    }

    if (stateId == State.SOFT_REFS) {
      org.mmtk.vm.VM.softReferences.scan(traceLocal, isNursery, needRetain);
      stateId = State.WEAK_REFS;
      return true;
    } else if (stateId == State.WEAK_REFS) {
      org.mmtk.vm.VM.weakReferences.scan(traceLocal, isNursery, needRetain);
      stateId = State.FINALIZABLE;
      return true;
    } else if (stateId == State.FINALIZABLE) {
      org.mmtk.vm.VM.finalizableProcessor.scan(traceLocal, isNursery);
      stateId = State.PHANTOM_REFS;
      return true;
    } else if (stateId == State.PHANTOM_REFS) {
      org.mmtk.vm.VM.phantomReferences.scan(traceLocal, isNursery, needRetain);
      stateId = State.INITIAL;
      return false;
    } else {
      VM.sysFail("unreachable");
      return false;
    }
  }

  /***********************************************************************
   * Static methods.
   */

  /** @return the processor singleton */
  public static ReferenceProcessingHelper getProcessor() {
    return referenceProcessingHelper;
  }

}
