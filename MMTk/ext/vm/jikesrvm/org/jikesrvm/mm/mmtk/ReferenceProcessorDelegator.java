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
import org.mmtk.plan.ReferenceProcessorDelegatorTracer;
import org.vmmagic.pragma.NoInline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.UninterruptibleNoWarn;
import org.vmmagic.pragma.Unpreemptible;
import org.vmmagic.pragma.UnpreemptibleNoWarn;
import org.vmmagic.unboxed.AddressArray;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.Word;

/**
 * ReferenceProcessorDelegator manages scanning and processing of different reference
 * types during garbage collection. It acts as a router to delegate
 * scanning to internal reference processors based on the current phase.
 */
@Uninterruptible
public final class ReferenceProcessorDelegator extends org.mmtk.vm.ReferenceProcessorDelegator {

  /********************************************************************
   * Class fields
   */

  /** The ReferenceProcessorDelegator singleton */
  private static final ReferenceProcessorDelegator referenceProcessorDelegator = new ReferenceProcessorDelegator();

  public enum Phase {
    PREPARE,
    SOFT_REFS,
    WEAK_REFS,
    FINALIZABLE,
    PHANTOM_REFS
  }

  /**
   * The current scanning phase.
   */
  private static Phase phaseId;

  /**
   * Create a new table.
   */
  protected ReferenceProcessorDelegator() {
    phaseId = Phase.PREPARE;
  }

  /**
   * Forwards references to new locations during a compacting GC.
   * 
   * @param trace     The tracer context
   * @param isNursery True if scanning the nursery
   */
  @Override
  @UninterruptibleNoWarn
  public void forward(ReferenceProcessorDelegatorTracer trace, boolean isNursery) {
    if (phaseId == Phase.PREPARE) {
      org.mmtk.vm.VM.finalizableProcessor.forward(trace, isNursery);
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
  @UninterruptibleNoWarn
  public boolean scan(ReferenceProcessorDelegatorTracer trace, boolean isNursery, boolean needRetain) {
    if (phaseId == Phase.PREPARE) {
      phaseId = Phase.SOFT_REFS;
    }

    if (phaseId == Phase.SOFT_REFS) {
      org.mmtk.vm.VM.softReferences.scan(trace, isNursery, needRetain);
      phaseId = Phase.WEAK_REFS;
      return true;
    } else if (phaseId == Phase.WEAK_REFS) {
      org.mmtk.vm.VM.weakReferences.scan(trace, isNursery, needRetain);
      phaseId = Phase.FINALIZABLE;
      return true;
    } else if (phaseId == Phase.FINALIZABLE) {
      org.mmtk.vm.VM.finalizableProcessor.scan(trace, isNursery);
      phaseId = Phase.PHANTOM_REFS;
      return true;
    } else if (phaseId == Phase.PHANTOM_REFS) {
      org.mmtk.vm.VM.phantomReferences.scan(trace, isNursery, needRetain);
      phaseId = Phase.PREPARE;
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
  public static ReferenceProcessorDelegator getProcessor() {
    return referenceProcessorDelegator;
  }

}
