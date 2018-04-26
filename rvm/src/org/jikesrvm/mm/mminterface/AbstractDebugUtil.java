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

import static org.jikesrvm.HeapLayoutConstants.BOOT_IMAGE_CODE_END;
import static org.jikesrvm.HeapLayoutConstants.BOOT_IMAGE_CODE_START;
import static org.jikesrvm.HeapLayoutConstants.BOOT_IMAGE_DATA_END;
import static org.jikesrvm.HeapLayoutConstants.BOOT_IMAGE_DATA_START;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMArray;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.objectmodel.ObjectModel;
import org.jikesrvm.objectmodel.TIB;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.scheduler.RVMThread;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Interruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

/**
 * Common debugging utility functions used by various garbage collectors
 */
@Uninterruptible
public abstract class AbstractDebugUtil {

  private static TIB tibForArrayType;
  private static TIB tibForClassType;
  private static TIB tibForPrimitiveType;

  @Interruptible
  static void boot(BootRecord theBootRecord) {
    // get addresses of TIBs for RVMArray & RVMClass used for testing Type ptrs
    RVMType t = RVMArray.IntArray;
    tibForArrayType = ObjectModel.getTIB(t);
    tibForPrimitiveType = ObjectModel.getTIB(RVMType.IntType);
    t = Magic.getObjectType(BootRecord.the_boot_record);
    tibForClassType = ObjectModel.getTIB(t);
  }

  /**
   * Check if an address appears to point to an instance of RVMType
   *
   * @param typeAddress the address to check
   * @return {@code true} if and only if the address appears to
   *  be an an instance of RVMType
   */
  @Uninterruptible
  public static boolean validType(ObjectReference typeAddress) {
    VM.sysFail("validType() has not been implemented in subclass");
    return false;
  }

  /**
   * Dump all threads &amp; their stacks starting at the frame identified
   * by the threads saved contextRegisters (ip &amp; fp fields).
   */
  @Uninterruptible
  public static void dumpAllThreadStacks() {
    RVMThread.dumpVirtualMachine();
  }  // dumpAllThreadStacks

  /**
   * Checks if a reference, its TIB pointer and type pointer
   * are all in the heap.
   *
   * @param ref the reference to check
   * @return {@code true} if and only if the reference
   *  refers to a valid object
   */
  @Uninterruptible
  public static boolean validObject(Object ref) {
    return validRef(ObjectReference.fromObject(ref));
  }

  @Uninterruptible
  public static boolean validRef(ObjectReference ref) {
    VM.sysFail("validRef() has not been implemented in subclass");
    return false;
  }

  @Uninterruptible
  public static boolean mappedVMRef(ObjectReference ref) {
    VM.sysFail("mappedVMRef() has not been implemented in subclass");
    return false;
  }

  @Entrypoint
  @Uninterruptible
  public static void dumpRef(ObjectReference ref) {
    VM.sysWrite("REF=");
    if (ref.isNull()) {
      VM.sysWriteln("NULL");
      VM.sysWriteln();
      return;
    }
    VM.sysWrite(ref);
    if (!mappedVMRef(ref)) {
      VM.sysWriteln(" (REF OUTSIDE OF HEAP OR NOT MAPPED)");
      return;
    }
    ObjectModel.dumpHeader(ref);
    ObjectReference tib = ObjectReference.fromObject(ObjectModel.getTIB(ref));
    if (!MemoryManager.mightBeTIB(tib)) {
      VM.sysWriteln(" (INVALID TIB: CLASS NOT ACCESSIBLE)");
      return;
    }
    RVMType type = Magic.getObjectType(ref.toObject());
    ObjectReference itype = ObjectReference.fromObject(type);
    VM.sysWrite(" TYPE=");
    VM.sysWrite(itype);
    if (!validType(itype)) {
      VM.sysWriteln(" (INVALID TYPE: CLASS NOT ACCESSIBLE)");
      return;
    }
    VM.sysWrite(" CLASS=");
    VM.sysWrite(type.getDescriptor());
    VM.sysWriteln();
  }

  public static boolean addrInBootImage(Address addr) {
    return (addr.GE(BOOT_IMAGE_DATA_START) && addr.LT(BOOT_IMAGE_DATA_END)) ||
           (addr.GE(BOOT_IMAGE_CODE_START) && addr.LT(BOOT_IMAGE_CODE_END));
  }
}
