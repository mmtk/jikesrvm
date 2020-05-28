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
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MOVES_OBJECTS;
import static org.jikesrvm.runtime.SysCall.sysCall;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMArray;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.objectmodel.ObjectModel;
import org.jikesrvm.objectmodel.TIB;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.scheduler.RVMThread;
import org.mmtk.policy.Space;
import org.mmtk.utility.heap.layout.HeapLayout;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Interruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

/**
 * Common debugging utility functions used by various garbage collectors
 */
@Uninterruptible
public class DebugUtil {

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
    if (!isMapped(typeAddress)) {
     return false;  // type address is outside of heap
    }

    // check if types tib is one of three possible values
    TIB typeTib = ObjectModel.getTIB(typeAddress);
    return ((typeTib == tibForClassType) || (typeTib == tibForArrayType) || (typeTib == tibForPrimitiveType));
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
  private static boolean isMapped(ObjectReference ref) {
    if (VM.BuildWithRustMMTk) {
      if (VM.VerifyAssertions && addrInBootImage(ref.toAddress()))
        VM._assert(sysCall.sysIsMappedObject(ref));
      return sysCall.sysIsMappedObject(ref);
    } else
      return Space.isMappedObject(ref); 
  }

  @Uninterruptible
  public static boolean validRef(ObjectReference ref) {
    if (ref.isNull()) return true;

    if (!isMapped(ref)) {
      VM.sysWrite("validRef: REF outside heap, ref = ");
      VM.sysWrite(ref);
      VM.sysWriteln();
      if (!VM.BuildWithRustMMTk)
        Space.printVMMap();
      return false;
    }
    if (MOVES_OBJECTS) {
      /*
      TODO: Work out how to check if forwarded
      if (Plan.isForwardedOrBeingForwarded(ref)) {
        // TODO: actually follow forwarding pointer
        // (need to bound recursion when things are broken!!)
        return true;
      }
      */
    }

    TIB tib = ObjectModel.getTIB(ref);
    Address tibAddr = Magic.objectAsAddress(tib);
    if (!isMapped(ObjectReference.fromObject(tib))) {
      VM.sysWrite("validRef: TIB outside heap, ref = ");
      VM.sysWrite(ref);
      VM.sysWrite(" tib = ");
      VM.sysWrite(tibAddr);
      VM.sysWriteln();
      if (!VM.BuildWithRustMMTk)
        ObjectModel.dumpHeader(ref);
      return false;
    }
    if (tibAddr.isZero()) {
      VM.sysWrite("validRef: TIB is Zero! ");
      VM.sysWrite(ref);
      VM.sysWriteln();
      ObjectModel.dumpHeader(ref);
      return false;
    }
    if (tib.length() == 0) {
      VM.sysWrite("validRef: TIB length zero, ref = ");
      VM.sysWrite(ref);
      VM.sysWrite(" tib = ");
      VM.sysWrite(tibAddr);
      VM.sysWriteln();
      ObjectModel.dumpHeader(ref);
      return false;
    }

    ObjectReference type = ObjectReference.fromObject(tib.getType());
    if (!validType(type)) {
      VM.sysWrite("validRef: invalid TYPE, ref = ");
      VM.sysWrite(ref);
      VM.sysWrite(" tib = ");
      VM.sysWrite(Magic.objectAsAddress(tib));
      VM.sysWrite(" type = ");
      VM.sysWrite(type);
      VM.sysWriteln();
      ObjectModel.dumpHeader(ref);
      return false;
    }
    return true;
  }  // validRef

  @Uninterruptible
  public static boolean mappedVMRef(ObjectReference ref) {
    if (VM.BuildWithRustMMTk) {
      if (VM.VerifyAssertions && addrInBootImage(ref.toAddress()))
        VM._assert(sysCall.sysIsMappedObject(ref));
      return sysCall.sysIsMappedObject(ref);
    } else {
      return Space.isMappedObject(ref) && HeapLayout.mmapper.objectIsMapped(ref);
    }
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
