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
package org.mmtk.plan.g1;

import org.mmtk.plan.*;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.LargeObjectLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.ForwardingWord;
import org.mmtk.vm.VM;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This class implements <i>per-collector thread</i> behavior
 * and state for the <i>SS</i> plan, which implements a full-heap
 * semi-space collector.<p>
 *
 * Specifically, this class defines <i>SS</i> collection behavior
 * (through <code>trace</code> and the <code>collectionPhase</code>
 * method), and collection-time allocation (copying of objects).<p>
 *
 * See {@link G1} for an overview of the semi-space algorithm.
 *
 * @see G1
 * @see G1Mutator
 * @see StopTheWorldCollector
 * @see CollectorContext
 */
@Uninterruptible
public class G1Collector extends StopTheWorldCollector {

  /****************************************************************************
   * Instance fields
   */

  /**
   *
   */
  protected final G1TraceLocal trace;
  protected final CopyLocal ss;
  protected final LargeObjectLocal los;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public G1Collector() {
    this(new G1TraceLocal(global().trace));
  }

  /**
   * Constructor
   * @param tr The trace to use
   */
  protected G1Collector(G1TraceLocal tr) {
    ss = new CopyLocal();
    los = new LargeObjectLocal(Plan.loSpace);
    trace = tr;
  }

  /****************************************************************************
   *
   * Collection-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public Address allocCopy(ObjectReference original, int bytes,
      int align, int offset, int allocator) {
    if (allocator == Plan.ALLOC_LOS) {
      if (VM.VERIFY_ASSERTIONS) VM.assertions._assert(bytes > Plan.MAX_NON_LOS_COPY_BYTES);
      return los.alloc(bytes, align, offset);
    } else {
      if (VM.VERIFY_ASSERTIONS) {
        VM.assertions._assert(bytes <= Plan.MAX_NON_LOS_COPY_BYTES);
        VM.assertions._assert(allocator == G1.ALLOC_SS);
      }
      return ss.alloc(bytes, align, offset);
    }
  }

  @Override
  @Inline
  public void postCopy(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    ForwardingWord.clearForwardingBits(object);
    if (allocator == Plan.ALLOC_LOS)
      Plan.loSpace.initializeHeader(object, false);
  }

  /****************************************************************************
   *
   * Collection
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public void collectionPhase(short phaseId, boolean primary) {
    if (phaseId == G1.PREPARE) {
      // rebind the copy bump pointer to the appropriate semispace.
      ss.rebind(G1.toSpace());
      los.prepare(true);
      super.collectionPhase(phaseId, primary);
      return;
    }

    if (phaseId == G1.CLOSURE) {
      trace.completeTrace();
      return;
    }

    if (phaseId == G1.RELEASE) {
      trace.release();
      los.release(true);
      super.collectionPhase(phaseId, primary);
      return;
    }

    super.collectionPhase(phaseId, primary);
  }


  /****************************************************************************
   *
   * Object processing and tracing
   */

  /**
   * Return {@code true} if the given reference is to an object that is within
   * one of the semi-spaces.
   *
   * @param object The object in question
   * @return {@code true} if the given reference is to an object that is within
   * one of the semi-spaces.
   */
  public static boolean isSemiSpaceObject(ObjectReference object) {
    return Space.isInSpace(G1.SS0, object) || Space.isInSpace(G1.SS1, object);
  }

  /****************************************************************************
   *
   * Miscellaneous
   */

  /** @return The active global plan as an <code>SS</code> instance. */
  @Inline
  private static G1 global() {
    return (G1) VM.activePlan.global();
  }

  @Override
  public TraceLocal getCurrentTrace() {
    return trace;
  }
}
