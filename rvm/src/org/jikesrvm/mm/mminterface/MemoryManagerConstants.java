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
package org.jikesrvm.mm.mminterface;

import org.mmtk.vm.VM;
import org.vmmagic.pragma.Entrypoint;

import static org.jikesrvm.runtime.UnboxedSizeConstants.LOG_BYTES_IN_WORD;


/**
 * This class merely exposes the MMTk constants into the Jikes RVM
 * package space so that they can be accessed by the VM in an
 * MM-neutral way.  It is separate from MemoryManager to break
 * cyclic class-loading dependencies.
 */
public class MemoryManagerConstants {
  /** {@code true} if the selected plan needs support for linearly scanning the heap */
  public static final boolean NEEDS_LINEAR_SCAN = Selected.Constraints.get().needsLinearScan();
  /** Number of bits in the GC header required by the selected plan */
  public static final int GC_HEADER_BITS = Selected.Constraints.get().gcHeaderBits();
  /** Number of additional bytes required in the header by the selected plan */
  public static final int GC_HEADER_BYTES = Selected.Constraints.get().gcHeaderWords() << LOG_BYTES_IN_WORD;
  /** {@code true} if the selected plan requires concurrent worker threads */
  public static final boolean NEEDS_CONCURRENT_WORKERS = Selected.Constraints.get().needsConcurrentWorkers();
  /** {@code true} if the selected plan needs support for generating a GC trace */
  public static final boolean GENERATE_GC_TRACE = Selected.Constraints.get().generateGCTrace();
  /** {@code true} if the selected plan may move objects */
  public static final boolean MOVES_OBJECTS = Selected.Constraints.get().movesObjects();
  /** {@code true} if the selected plan moves TIB objects */
  public static final boolean MOVES_TIBS = false;
  /** {@code true} if the selected plan moves code */
  @Entrypoint(fieldMayBeFinal = true)
  public static final boolean MOVES_CODE = false;
  /**
   * log_2 of the maximum number of spaces a Plan can support.
   */
  public static final int LOG_MAX_SPACES = 4;

  /**
   * Maximum number of spaces a Plan can support.
   */
  public static final int MAX_SPACES = 1 << LOG_MAX_SPACES;
  /**
   * This value specifies the <i>minimum</i> allocation alignment
   * requirement of the VM.  When making allocation requests, both
   * <code>align</code> and <code>offset</code> must be multiples of
   * <code>MIN_ALIGNMENT</code>.
   *
   * This value is required to be a power of 2.
   */
  public static final byte LOG_MIN_ALIGNMENT = VM.LOG_MIN_ALIGNMENT;
  public static final int MIN_ALIGNMENT = 1 << LOG_MIN_ALIGNMENT;
}

