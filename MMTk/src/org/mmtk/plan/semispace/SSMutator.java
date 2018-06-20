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
package org.mmtk.plan.semispace;

import org.jikesrvm.mm.mminterface.MemoryManager;
import org.mmtk.plan.*;
import org.mmtk.policy.CopyLocal;
import org.mmtk.policy.Space;
import org.mmtk.utility.Log;
import org.mmtk.utility.Memory;
import org.mmtk.utility.alloc.Allocator;

import org.vmmagic.unboxed.*;
import org.vmmagic.pragma.*;

/**
 * This class implements <i>per-mutator thread</i> behavior
 * and state for the <i>SS</i> plan, which implements a full-heap
 * semi-space collector.<p>
 *
 * Specifically, this class defines <i>SS</i> mutator-time allocation
 * and per-mutator thread collection semantics (flushing and restoring
 * per-mutator allocator state).<p>
 *
 * See {@link SS} for an overview of the semi-space algorithm.
 *
 * @see SS
 * @see SSCollector
 * @see StopTheWorldMutator
 * @see MutatorContext
 */
@Uninterruptible
public class SSMutator extends StopTheWorldMutator {
  /****************************************************************************
   * Instance fields
   */
  protected final CopyLocal ss;

  /****************************************************************************
   *
   * Initialization
   */

  /**
   * Constructor
   */
  public SSMutator() {
    ss = new CopyLocal();
  }

  /**
   * Called before the MutatorContext is used, but after the context has been
   * fully registered and is visible to collection.
   */
  @Override
  public void initMutator(int id) {
    super.initMutator(id);
    ss.rebind(SS.toSpace());
  }

  /****************************************************************************
   *
   * Mutator-time allocation
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Inline
  public Address alloc(int bytes, int align, int offset, int allocator, int site) {
    if (allocator == SS.ALLOC_SS)
      return ss.alloc(bytes, align, offset);
    else
      return super.alloc(bytes, align, offset, allocator, site);
  }

  @Override
  @Inline
  public void postAlloc(ObjectReference object, ObjectReference typeRef,
      int bytes, int allocator) {
    if (allocator == SS.ALLOC_SS) return;
    super.postAlloc(object, typeRef, bytes, allocator);
  }

  @Override
  public Allocator getAllocatorFromSpace(Space space) {
    if (space == SS.copySpace0 || space == SS.copySpace1) return ss;
    return super.getAllocatorFromSpace(space);
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
    if (phaseId == SS.PREPARE) {
      super.collectionPhase(phaseId, primary);
      return;
    }

    if (phaseId == SS.RELEASE) {
      super.collectionPhase(phaseId, primary);
      // rebind the allocation bump pointer to the appropriate semispace.
      ss.rebind(SS.toSpace());
      return;
    }

    super.collectionPhase(phaseId, primary);
  }


  /****************************************************************************
   *
   * Miscellaneous
   */

  /**
   * Show the status of each of the allocators.
   */
  public final void show() {
    ss.show();
    los.show();
    immortal.show();
  }

  @Override
  public ObjectReference objectReferenceRead(ObjectReference src, Address slot, Word metaDataA, Word metaDataB, int mode) {
    ObjectReference o = slot.loadObjectReference();
    Address add = o.toAddress();
    if (o.isNull() || MemoryManager.numGCFinished == 0) {
      return o;
    } else if (add.LT(Address.fromIntZeroExtend(0x68000000)) && add.GE(Address.fromIntZeroExtend(0x60000000))) {
      // Boot
      return o;
    } else if (add.LT(Address.fromIntZeroExtend(0xb0000000)) && add.GE(Address.fromIntZeroExtend(0x98000000))) {
      // VMSpace
      return o;
    } else {
      if (MemoryManager.numGCFinished % 2 == 0) {
        if (add.LT(Address.fromIntZeroExtend(0x80000000)) && add.GE(Address.fromIntZeroExtend(0x68000000))) return o;
        Log.write("heap slot ", slot);
        Log.writeln(" pointing at ", o);
        org.mmtk.vm.VM.assertions.fail("Slot not in correct space");
      } else {
        if (add.LT(Address.fromIntZeroExtend(0x98000000)) && add.GE(Address.fromIntZeroExtend(0x80000000))) return o;
        Log.write("heap slot ", slot);
        Log.writeln(" pointing at ", o);
        org.mmtk.vm.VM.assertions.fail("Slot not in correct space");
      }
    }
    return o;
  }

  @Override
  public ObjectReference objectReferenceNonHeapRead(Address slot, Word metaDataA, Word metaDataB) {
    ObjectReference o = slot.loadObjectReference();
    Address add = o.toAddress();
    if (o.isNull() || MemoryManager.numGCFinished == 0) {
      return o;
    } else if (add.LT(Address.fromIntZeroExtend(0x68000000)) && add.GE(Address.fromIntZeroExtend(0x60000000))) {
      // Boot
      return o;
    } else if (add.LT(Address.fromIntZeroExtend(0xb0000000)) && add.GE(Address.fromIntZeroExtend(0x98000000))) {
      // VMSpace
      return o;
    } else {
      if (MemoryManager.numGCFinished % 2 == 0) {
        if (add.LT(Address.fromIntZeroExtend(0x80000000)) && add.GE(Address.fromIntZeroExtend(0x68000000))) return o;
        Log.write("non heap slot ", slot);
        Log.writeln(" pointing at ", o);
        org.mmtk.vm.VM.assertions.fail("Slot not in correct space");
      } else {
        if (add.LT(Address.fromIntZeroExtend(0x98000000)) && add.GE(Address.fromIntZeroExtend(0x80000000))) return o;
        Log.write("non heap slot ", slot);
        Log.writeln(" pointing at ", o);
        org.mmtk.vm.VM.assertions.fail("Slot not in correct space");
      }
    }
    return o;  }
}
