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

import org.jikesrvm.VM;
import org.vmmagic.pragma.Entrypoint;

import static org.jikesrvm.runtime.UnboxedSizeConstants.LOG_BYTES_IN_WORD;

/**
 * This class merely exposes the MMTk constants into the Jikes RVM
 * package space so that they can be accessed by the VM in an
 * MM-neutral way.  It is separate from MemoryManager to break
 * cyclic class-loading dependencies.
 */
public class MemoryManagerConstants {

  public static final int INSTANCE_FIELD = 0;
  public static final int ARRAY_ELEMENT = 1;
  public static final byte LOG_BYTES_IN_ADDRESS = VM.BuildFor64Addr ? 3 : 2;
  public static final byte LOG_BYTES_IN_INT = 2;
  public static final byte LOG_BYTES_IN_CHAR = 1;
  public static final byte LOG_BYTES_IN_SHORT = 1;
  public static final byte LOG_BYTES_IN_LONG = 3;
  public static final int BYTES_IN_LONG = 1 << LOG_BYTES_IN_LONG;
  public static final byte LOG_BYTES_IN_WORD = LOG_BYTES_IN_ADDRESS;
  public static final byte BYTES_IN_ADDRESS = 1 << LOG_BYTES_IN_ADDRESS;
  public static final byte LOG_BITS_IN_BYTE = 3;
  public static final int BITS_IN_BYTE = 1 << LOG_BITS_IN_BYTE;
  public static final int BYTES_IN_WORD = 1 << LOG_BYTES_IN_WORD;

  /** {@code true} if the selected plan needs support for linearly scanning the heap */
  public static final boolean NEEDS_LINEAR_SCAN = false; // Selected.Constraints.get().needsLinearScan();
  /** Number of bits in the GC header required by the selected plan */
  public static final int GC_HEADER_BITS = 0; // Selected.Constraints.get().gcHeaderBits();
  /** Number of additional bytes required in the header by the selected plan */
  public static final int GC_HEADER_BYTES = 0; // Selected.Constraints.get().gcHeaderWords() << LOG_BYTES_IN_WORD;
  /** {@code true} if the selected plan requires concurrent worker threads */
  public static final boolean NEEDS_CONCURRENT_WORKERS = false; // Selected.Constraints.get().needsConcurrentWorkers();
  /** {@code true} if the selected plan needs support for generating a GC trace */
  public static final boolean GENERATE_GC_TRACE = false; //Selected.Constraints.get().generateGCTrace();
  /** {@code true} if the selected plan may move objects */
  public static final boolean MOVES_OBJECTS = false; // Selected.Constraints.get().movesObjects();
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
  public static final byte LOG_MIN_ALIGNMENT = 2;
  public static final int MIN_ALIGNMENT = 1 << LOG_MIN_ALIGNMENT;
  public static final int MAX_ALIGNMENT_SHIFT = (VM.BuildForIA32 ? 1 : 0) + LOG_BYTES_IN_LONG - LOG_BYTES_IN_INT;
  public static final int MAX_ALIGNMENT = MIN_ALIGNMENT << MAX_ALIGNMENT_SHIFT;

}

