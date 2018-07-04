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

import static org.jikesrvm.objectmodel.TIBLayoutConstants.IMT_METHOD_SLOTS;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MAX_SPACES;

import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMMethod;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.SpecializedMethod;
import org.jikesrvm.compilers.common.CodeArray;
import org.jikesrvm.objectmodel.BootImageInterface;
import org.jikesrvm.objectmodel.IMT;
import org.jikesrvm.objectmodel.ITable;
import org.jikesrvm.objectmodel.ITableArray;
import org.jikesrvm.objectmodel.TIB;
import org.jikesrvm.runtime.BootRecord;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * The interface that the MMTk memory manager presents to Jikes RVM
 */
@Uninterruptible
public abstract class AbstractMemoryManager {

  /***********************************************************************
   *
   * Class variables
   */

  /**
   * Has the interface been booted yet?
   */
  static boolean booted = false;

  /**
   * Has garbage collection been enabled yet?
   */
  static boolean collectionEnabled = false;

  /***********************************************************************
   *
   * Initialization
   */

  /**
   * Initialization that occurs at <i>boot</i> time (runtime
   * initialization).  This is only executed by one processor (the
   * primordial thread).
   *
   * @param theBootRecord the boot record. Contains information about
   * the heap size.
   */
  @Interruptible
  public static void boot(BootRecord theBootRecord) {
    booted = true;
    throw new UnsupportedOperationException("boot() has not been implemented in subclass");
  }

  /**
   * Perform postBoot operations such as dealing with command line
   * options (this is called as soon as options have been parsed,
   * which is necessarily after the basic allocator boot).
   */
  @Interruptible
  public static void postBoot() {
    throw new UnsupportedOperationException("postBoot() has not been implemented in subclass");
  }

  /**
   * Allow collection (assumes threads can be created).
   */
  @Interruptible
  public static void enableCollection() {
    collectionEnabled = true;
    throw new UnsupportedOperationException("enableCollection() has not been implemented in subclass");
  }

  /**
   * @return whether collection is enabled
   */
  public static boolean collectionEnabled() {
    return collectionEnabled;
  }

  /**
   * Notify the MM that the host VM is now fully booted.
   */
  @Interruptible
  public static void fullyBootedVM() {
    throw new UnsupportedOperationException("fullyBootedVM() has not been implemented in subclass");
  }

  @Interruptible
  public static void processCommandLineArg(String arg) {
    throw new UnsupportedOperationException("processCommandLineArg() has not been implemented in subclass");
  }

  /***********************************************************************
   *
   * Write barriers
   */

  /**
   * Checks that if a garbage collection is in progress then the given
   * object is not movable.  If it is movable error messages are
   * logged and the system exits.
   *
   * @param object the object to check
   */
  @Entrypoint
  public static void modifyCheck(Object object) {
    VM.sysFail("modifyCheck() has not been implemented in subclass");
  }

  /***********************************************************************
   *
   * Statistics
   */

  /***********************************************************************
   *
   * Application interface to memory manager
   */

  /**
   * Returns the amount of free memory.
   *
   * @return The amount of free memory.
   */
  public static Extent freeMemory() {
    VM.sysFail("freeMemory() has not been implemented in subclass");
    return null;
  }

  /**
   * Returns the amount of total memory.
   *
   * @return The amount of total memory.
   */
  public static Extent totalMemory() {
    VM.sysFail("totalMemory() has not been implemented in subclass");
    return null;
  }

  /**
   * Returns the maximum amount of memory VM will attempt to use.
   *
   * @return The maximum amount of memory VM will attempt to use.
   */
  public static Extent maxMemory() {
    VM.sysFail("maxMemory() has not been implemented in subclass");
    return null;
  }

  /**
   * External call to force a garbage collection.
   */
  @Interruptible
  public static void gc() {
    throw new UnsupportedOperationException("gc() has not been implemented in subclass");
  }

  /****************************************************************************
   *
   * Check references, log information about references
   */

  /**
   * Logs information about a reference to the error output.
   *
   * @param ref the address to log information about
   */
  public static void dumpRef(ObjectReference ref) {
    DebugUtil.dumpRef(ref);
  }

  /**
   * Checks if a reference is valid.
   *
   * @param ref the address to be checked
   * @return <code>true</code> if the reference is valid
   */
  @Inline
  public static boolean validRef(ObjectReference ref) {
    if (booted) {
      return DebugUtil.validRef(ref);
    } else {
      return true;
    }
  }

  /**
   * Checks if an address refers to an in-use area of memory.
   *
   * @param address the address to be checked
   * @return <code>true</code> if the address refers to an in use area
   */
  @Inline
  public static boolean addressInVM(Address address) {
    VM.sysFail("addressInVM() has not been implemented in subclass");
    return false;
  }

  /**
   * Return true if address is in a space which may contain stacks
   *
   * @param address The address to be checked
   * @return true if the address is within a space which may contain stacks
   */
  public static boolean mightBeFP(Address address) {
    VM.sysFail("mightBeFP() has not been implemented in subclass");
    return false;
  }

  /***********************************************************************
   *
   * Allocation
   */

  /**
   * Is string <code>a</code> a prefix of string
   * <code>b</code>. String <code>b</code> is encoded as an ASCII byte
   * array.
   *
   * @param a prefix string
   * @param b string which may contain prefix, encoded as an ASCII
   * byte array.
   * @return <code>true</code> if <code>a</code> is a prefix of
   * <code>b</code>
   */
  @Interruptible
  static boolean isPrefix(String a, byte[] b) {
    int aLen = a.length();
    if (aLen > b.length) {
      return false;
    }
    for (int i = 0; i < aLen; i++) {
      if (a.charAt(i) != ((char) b[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Return an allocation site upon request.  The request may be made
   * in the context of compilation.
   *
   * @param compileTime {@code true} if this request is being made in the
   * context of a compilation.
   * @return an allocation site
   */
  public static int getAllocationSite(boolean compileTime) {
    VM.sysFail("getAllocationSite() has not been implemented in subclass");
    return 0;
  }

  /**
   * Returns the appropriate allocation scheme/area for the given
   * type.  This form is deprecated.  Without the RVMMethod argument,
   * it is possible that the wrong allocator is chosen which may
   * affect correctness. The prototypical example is that JMTk
   * meta-data must generally be in immortal or at least non-moving
   * space.
   *
   *
   * @param type the type of the object to be allocated
   * @return the identifier of the appropriate allocator
   */
  @Interruptible
  public static int pickAllocator(RVMType type) {
    return pickAllocator(type, null);
  }

  /**
   * Returns the appropriate allocation scheme/area for the given type
   * and given method requesting the allocation.
   *
   * @param type the type of the object to be allocated
   * @param method the method requesting the allocation
   * @return the identifier of the appropriate allocator
   */
  @Interruptible
  public static int pickAllocator(RVMType type, RVMMethod method) {
    throw new UnsupportedOperationException("pickAllocator() has not been implemented in subclass");
  }

  /***********************************************************************
   * These methods allocate memory.  Specialized versions are available for
   * particular object types.
   ***********************************************************************
   */

  /**
   * Allocate a scalar object.
   *
   * @param size Size in bytes of the object, including any headers
   * that need space.
   * @param tib  Type of the object (pointer to TIB).
   * @param allocator Specify which allocation scheme/area JMTk should
   * allocate the memory from.
   * @param align the alignment requested; must be a power of 2.
   * @param offset the offset at which the alignment is desired.
   * @param site allocation site.
   * @return the initialized Object
   */
  @Inline
  public static Object allocateScalar(int size, TIB tib, int allocator, int align, int offset, int site) {
    VM.sysFail("allocateScalar() has not been implemented in subclass");
    return null;
  }

  /**
   * Allocate an array object. This is the interruptible component, including throwing
   * an OutOfMemoryError for arrays that are too large.
   *
   * @param numElements number of array elements
   * @param logElementSize size in bytes of an array element, log base 2.
   * @param headerSize size in bytes of array header
   * @param tib type information block for array object
   * @param allocator int that encodes which allocator should be used
   * @param align the alignment requested; must be a power of 2.
   * @param offset the offset at which the alignment is desired.
   * @param site allocation site.
   * @return array object with header installed and all elements set
   *         to zero/null
   * See also: bytecode 0xbc ("newarray") and 0xbd ("anewarray")
   */
  @Inline
  @Unpreemptible
  public static Object allocateArray(int numElements, int logElementSize, int headerSize, TIB tib, int allocator,
                                     int align, int offset, int site) {
    VM.sysFail("allocateArray() has not been implemented in subclass");
    return null;
  }

  /**
   * Align an allocation using some modulo arithmetic to guarantee the
   * following property:<br>
   * <code>(region + offset) % alignment == 0</code>
   *
   * @param initialOffset The initial (unaligned) start value of the
   * allocated region of memory.
   * @param align The alignment requested, must be a power of two
   * @param offset The offset at which the alignment is desired
   * @return <code>initialOffset</code> plus some delta (possibly 0) such
   * that the return value is aligned according to the above
   * constraints.
   */
  @Inline
  public static Offset alignAllocation(Offset initialOffset, int align, int offset) {
    VM.sysFail("alignAllocation() has not been implemented in subclass");
    return null;
  }

  /**
   * Allocate a CodeArray into a code space.
   * Currently the interface is fairly primitive;
   * just the number of instructions in the code array and a boolean
   * to indicate hot or cold code.
   * @param numInstrs number of instructions
   * @param isHot is this a request for hot code space allocation?
   * @return The  array
   */
  @NoInline
  @Interruptible
  public static CodeArray allocateCode(int numInstrs, boolean isHot) {
    throw new UnsupportedOperationException("allocateCode() has not been implemented in subclass");
  }

  /**
   * Allocate a stack
   * @param bytes the number of bytes to allocate. Must be greater than
   *  0.
   * @return The stack
   */
  @NoInline
  @Unpreemptible
  public static byte[] newStack(int bytes) {
    VM.sysFail("newStack() has not been implemented in subclass");
    return null;
  }

  /**
   * Allocates a non moving word array.
   *
   * @param size The size of the array
   * @return the new non moving word array
   */
  @NoInline
  @Interruptible
  public static WordArray newNonMovingWordArray(int size) {
    throw new UnsupportedOperationException("newNonMovingWordArray() has not been implemented in subclass");
  }

  /**
   * Allocates a non moving double array.
   *
   * @param size The size of the array
   * @return the new non moving double array
   */
  @NoInline
  @Interruptible
  public static double[] newNonMovingDoubleArray(int size) {
    throw new UnsupportedOperationException("newNonMovingDoubleArray() has not been implemented in subclass");
  }

  /**
   * Allocates a non moving int array.
   *
   * @param size The size of the array
   * @return the new non moving int array
   */
  @NoInline
  @Interruptible
  public static int[] newNonMovingIntArray(int size) {
    throw new UnsupportedOperationException("newNonMovingIntArray() has not been implemented in subclass");
  }

  /**
   * Allocates a non moving short array.
   *
   * @param size The size of the array
   * @return the new non moving short array
   */
  @NoInline
  @Interruptible
  public static short[] newNonMovingShortArray(int size) {
    throw new UnsupportedOperationException("newNonMovingShortArray() has not been implemented in subclass");
  }

  /**
   * Allocates a new type information block (TIB).
   *
   * @param numVirtualMethods the number of virtual method slots in the TIB
   * @param alignCode alignment encoding for the TIB
   * @return the new TIB
   * @see AlignmentEncoding
   */
  @NoInline
  @Interruptible
  public static TIB newTIB(int numVirtualMethods, int alignCode) {
    throw new UnsupportedOperationException("newTIB() has not been implemented in subclass");
  }

  /**
   * Allocate a new interface method table (IMT).
   *
   * @return the new IMT
   */
  @NoInline
  @Interruptible
  public static IMT newIMT() {
    if (!VM.runningVM) {
      return IMT.allocate();
    }

    return (IMT)newRuntimeTable(IMT_METHOD_SLOTS, RVMType.IMTType);
  }

  /**
   * Allocate a new ITable
   *
   * @param size the number of slots in the ITable
   * @return the new ITable
   */
  @NoInline
  @Interruptible
  public static ITable newITable(int size) {
    if (!VM.runningVM) {
      return ITable.allocate(size);
    }

    return (ITable)newRuntimeTable(size, RVMType.ITableType);
  }

  /**
   * Allocate a new ITableArray
   *
   * @param size the number of slots in the ITableArray
   * @return the new ITableArray
   */
  @NoInline
  @Interruptible
  public static ITableArray newITableArray(int size) {
    if (!VM.runningVM) {
      return ITableArray.allocate(size);
    }

    return (ITableArray)newRuntimeTable(size, RVMType.ITableArrayType);
  }

  /**
   * Allocates a new runtime table (at runtime).
   *
   * @param size The size of the table
   * @param type the type for the table
   * @return the newly allocated table
   */
  @NoInline
  @Interruptible
  public static Object newRuntimeTable(int size, RVMType type) {
    throw new UnsupportedOperationException("newRuntimeTable() has not been implemented in subclass");
  }

  /**
   * Checks if the object can move. This information is useful to
   * optimize some JNI calls.
   *
   * @param obj the object in question
   * @return {@code true} if this object can never move, {@code false}
   *   if it can move.
   */
  @Pure
  public static boolean willNeverMove(Object obj) {
    VM.sysFail("willNeverMove() has not been implemented in subclass");
    return false;
  }

  /**
   * @param obj the object in question
   * @return whether the object is immortal
   */
  @Pure
  public static boolean isImmortal(Object obj) {
    VM.sysFail("isImmortal() has not been implemented in subclass");
    return false;
  }

  /***********************************************************************
   *
   * Finalizers
   */

  /**
   * Adds an object to the list of objects to have their
   * <code>finalize</code> method called when they are reclaimed.
   *
   * @param object the object to be added to the finalizer's list
   */
  @Interruptible
  public static void addFinalizer(Object object) {
    throw new UnsupportedOperationException("addFinalizer() has not been implemented in subclass");
  }

  /**
   * Gets an object from the list of objects that are to be reclaimed
   * and need to have their <code>finalize</code> method called.
   *
   * @return the object needing to be finialized
   */
  @Unpreemptible("Non-preemptible but may yield if finalizable table is being grown")
  public static Object getFinalizedObject() {
    VM.sysFail("getFinalizedObject() has not been implemented in subclass");
    return null;
  }

  /***********************************************************************
   *
   * References
   */

  /**
   * Add a soft reference to the list of soft references.
   *
   * @param obj the soft reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addSoftReference(SoftReference<?> obj, Object referent) {
    throw new UnsupportedOperationException("addSoftReference() has not been implemented in subclass");
  }

  /**
   * Add a weak reference to the list of weak references.
   *
   * @param obj the weak reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addWeakReference(WeakReference<?> obj, Object referent) {
    throw new UnsupportedOperationException("addWeakReference() has not been implemented in subclass");
  }

  /**
   * Add a phantom reference to the list of phantom references.
   *
   * @param obj the phantom reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addPhantomReference(PhantomReference<?> obj, Object referent) {
    throw new UnsupportedOperationException("addPhantomReference() has not been implemented in subclass");
  }

  /***********************************************************************
   *
   * Miscellaneous
   */

  /**
   * A new type has been resolved by the VM.  Create a new MM type to
   * reflect the VM type, and associate the MM type with the VM type.
   *
   * @param vmType The newly resolved type
   */
  @Interruptible
  public static void notifyClassResolved(RVMType vmType) {
    throw new UnsupportedOperationException("notifyClassResolved() has not been implemented in subclass");
  }

  /**
   * Check if object might be a TIB.
   *
   * @param obj address of object to check
   * @return <code>false</code> if the object is in the wrong
   * allocation scheme/area for a TIB, <code>true</code> otherwise
   */
  @Inline
  public static boolean mightBeTIB(ObjectReference obj) {
    VM.sysFail("mightBeTIB() has not been implemented in subclass");
    return false;
  }

  /**
   * Returns true if GC is in progress.
   *
   * @return True if GC is in progress.
   */
  public static boolean gcInProgress() {
    VM.sysFail("gcInProgress() has not been implemented in subclass");
    return false;
  }

  /**
   * @return the number of specialized methods.
   */
  public static int numSpecializedMethods() {
    VM.sysFail("gcInProgress() has not been implemented in subclass");
    return 0;
  }

  /**
   * Initialize a specified specialized method.
   *
   * @param id the specializedMethod
   * @return the created specialized scan method
   */
  @Interruptible
  public static SpecializedMethod createSpecializedMethod(int id) {
    throw new UnsupportedOperationException("createSpecializedMethod() has not been implemented in subclass");
  }

  /**
   * Flush the mutator context.
   */
  public static void flushMutatorContext() {
    VM.sysFail("flushMutatorContext() has not been implemented in subclass");
  }

  /***********************************************************************
   *
   * Header initialization
   */

  /**
   * Override the boot-time initialization method here, so that
   * the core MMTk code doesn't need to know about the
   * BootImageInterface type.
   *
   * @param bootImage the bootimage instance
   * @param ref       the object's address
   * @param tib       the object's TIB
   * @param size      the number of bytes allocated by the GC system for
   *                  the object
   * @param isScalar  whether the header belongs to a scalar or an array
   */
  @Interruptible
  public static void initializeHeader(BootImageInterface bootImage, Address ref, TIB tib, int size,
                                      boolean isScalar) {
    throw new UnsupportedOperationException("initializeHeader() has not been implemented in subclass");
  }

  /**
   * Installs a reference into the boot image.
   *
   * @param value the reference to install
   * @return the installed reference
   */
  @Interruptible
  public static Word bootTimeWriteBarrier(Word value) {
    throw new UnsupportedOperationException("createSpecializedMethod() has not been implemented in subclass");
  }

  /***********************************************************************
   *
   * Deprecated and/or broken.  The following need to be expunged.
   */

  /**
   * Returns the maximum number of heaps that can be managed.
   *
   * @return the maximum number of heaps
   */
  public static int getMaxHeaps() {
    /*
     *  The boot record has a table of address ranges of the heaps,
     *  the maximum number of heaps is used to size the table.
     */
    return MAX_SPACES;
  }

  /**
   * Allocate a contiguous int array
   * @param n The number of ints
   * @return The contiguous int array
   */
  @Inline
  @Interruptible
  public static int[] newContiguousIntArray(int n) {
    return new int[n];
  }

  /***********************************************************************
   *
   * Experimental
   */

  /**
   * Take a bytemap encoding of all references in the boot image, and
   * produce an encoded byte array.  Return the total length of the
   * encoding.
   *
   * @param bootImageRMap     space for the compressed reference map. The map
   *                          is initially empty and will be filled during execution of this method.
   * @param referenceMap      the (uncompressed) reference map for the bootimage
   * @param referenceMapLimit the highest index in the referenceMap that
   *                          contains a reference
   * @return the total length of the encoding
   */
  @Interruptible
  public static int encodeRMap(byte[] bootImageRMap, byte[] referenceMap,
                               int referenceMapLimit) {
    return ScanBootImage.encodeRMap(bootImageRMap,referenceMap,referenceMapLimit);
  }
}