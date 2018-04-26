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

import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_ADDRESS;

import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.objectmodel.JavaHeader;
import org.jikesrvm.objectmodel.ObjectModel;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;

/**
 * Supply and interpretation of values to be alignment-encoded into
 * the TIB pointer of an object.
 */
public class HandInlinedScanning {

  public static final int AE_FALLBACK = (1 << AlignmentEncoding.FIELD_WIDTH) - 1;
  public static final int AE_REFARRAY = AE_FALLBACK - 1;

  public static final int AE_PATTERN_0x0  = 0;
  public static final int AE_PATTERN_0x1  = 1;
  public static final int AE_PATTERN_0x7  = 2;
  public static final int AE_PATTERN_0x3F = 3;
  public static final int AE_PATTERN_0x3  = 4;
  public static final int AE_PATTERN_0x3D = 5;

  private static final int FIELD0_OFFSET =
    JavaHeader.objectStartOffset(RVMType.JavaLangObjectType) +
    ObjectModel.computeScalarHeaderSize(RVMType.JavaLangObjectType);

  private static final int FIELD1_OFFSET = FIELD0_OFFSET + BYTES_IN_ADDRESS;
  private static final int FIELD2_OFFSET = FIELD1_OFFSET + BYTES_IN_ADDRESS;
  private static final int FIELD3_OFFSET = FIELD2_OFFSET + BYTES_IN_ADDRESS;
  private static final int FIELD4_OFFSET = FIELD3_OFFSET + BYTES_IN_ADDRESS;
  private static final int FIELD5_OFFSET = FIELD4_OFFSET + BYTES_IN_ADDRESS;

  /** Master switch */
  public static final boolean ENABLED = true;

  public static int referenceArray() {
    if (!ENABLED)
      return AlignmentEncoding.ALIGN_CODE_NONE;
    return AE_REFARRAY;
  }

  public static int primitiveArray() {
    if (!ENABLED)
      return AlignmentEncoding.ALIGN_CODE_NONE;
    return AE_PATTERN_0x0;
  }

  public static int fallback() {
    if (!ENABLED)
      return AlignmentEncoding.ALIGN_CODE_NONE;
    return AE_FALLBACK;
  }

  public static int scalar(int[] offsets) {
    if (!ENABLED)
      return AlignmentEncoding.ALIGN_CODE_NONE;
    if (offsets.length == 0) {
      return AE_PATTERN_0x0;
    }
    if (offsets.length == 1) {
      if (offsets[0] == FIELD0_OFFSET)
        return AE_PATTERN_0x1;
    }
//    if (offsets.length == 2) {
//      if (offsets[0] == FIELD0_OFFSET &&
//          offsets[1] == FIELD1_OFFSET)
//        return AE_PATTERN_0x3;
//    }
    if (offsets.length == 3) {
      if (offsets[0] == FIELD0_OFFSET &&
          offsets[1] == FIELD1_OFFSET &&
          offsets[2] == FIELD2_OFFSET)
        return AE_PATTERN_0x7;
    }
//    if (offsets.length == 5) {
//      if (offsets[0] == FIELD0_OFFSET &&
//          offsets[1] == FIELD2_OFFSET &&
//          offsets[2] == FIELD3_OFFSET &&
//          offsets[3] == FIELD4_OFFSET &&
//          offsets[4] == FIELD5_OFFSET)
//        return AE_PATTERN_0x3D;
//    }
    if (offsets.length == 6) {
      if (offsets[0] == FIELD0_OFFSET &&
          offsets[1] == FIELD1_OFFSET &&
          offsets[2] == FIELD2_OFFSET &&
          offsets[3] == FIELD3_OFFSET &&
          offsets[4] == FIELD4_OFFSET &&
          offsets[5] == FIELD5_OFFSET)
        return AE_PATTERN_0x3F;
    }
    return AE_FALLBACK;
  }
}
