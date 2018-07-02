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
import org.jikesrvm.architecture.StackFrameLayout;
import org.jikesrvm.classloader.*;
import org.jikesrvm.compilers.common.CodeArray;
import org.jikesrvm.objectmodel.*;
import org.jikesrvm.options.OptionSet;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Callbacks;
import org.jikesrvm.runtime.SysCall;
import org.jikesrvm.scheduler.RVMThread;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import static org.jikesrvm.HeapLayoutConstants.*;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MAX_ALIGNMENT;
import static org.jikesrvm.objectmodel.TIBLayoutConstants.IMT_METHOD_SLOTS;
import static org.jikesrvm.runtime.ExitStatus.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MIN_ALIGNMENT;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MAX_SPACES;

/**
 * The interface that the MMTk memory manager presents to Jikes RVM
 */
@Uninterruptible
public final class MemoryManager extends AbstractMemoryManager {

  /***********************************************************************
   *
   * Class variables
   */

  /**
   * <code>true</code> if checking of allocated memory to ensure it is
   * zeroed is desired.
   */
  private static final boolean CHECK_MEMORY_IS_ZEROED = false;
  private static final boolean traceAllocator = false;
  private static final boolean traceAllocation = false;

  /**
   * Has the interface been booted yet?
   */
  private static boolean booted = false;

  /**
   * Has garbage collection been enabled yet?
   */
  private static boolean collectionEnabled = false;

  /***********************************************************************
   *
   * Initialization
   */

  @Entrypoint
  public static void prepareMutator(RVMThread t) {
    /*
     * The collector threads of processors currently running threads
     * off in JNI-land cannot run.
     */
    t.monitor().lockNoHandshake();
    // are these the only unexpected states?
    t.assertUnacceptableStates(RVMThread.IN_JNI,RVMThread.IN_NATIVE);
    int execStatus = t.getExecStatus();
    // these next assertions are not redundant given the ability of the
    // states to change asynchronously, even when we're holding the lock, since
    // the thread may change its own state.  of course that shouldn't happen,
    // but having more assertions never hurts...
    if (VM.VerifyAssertions) VM._assert(execStatus != RVMThread.IN_JNI);
    if (VM.VerifyAssertions) VM._assert(execStatus != RVMThread.IN_NATIVE);
    if (execStatus == RVMThread.BLOCKED_IN_JNI) {
      if (false) {
        VM.sysWriteln("for thread #",t.getThreadSlot()," setting up JNI stack scan");
        VM.sysWriteln("thread #",t.getThreadSlot()," has top java fp = ",t.getJNIEnv().topJavaFP());
      }

      /* thread is blocked in C for this GC.
       Its stack needs to be scanned, starting from the "top" java
       frame, which has been saved in the running threads JNIEnv.  Put
       the saved frame pointer into the threads saved context regs,
       which is where the stack scan starts. */
      t.contextRegisters.setInnermost(Address.zero(), t.getJNIEnv().topJavaFP());
    }
    t.monitor().unlock();
  }


  /*
   * This code is just for rust to call into JikesRVM. It is only for debugging purposes.
   */
  @Entrypoint
  public static int test2(int a, int b) {
    return a + b;
  }

  @Entrypoint
  public static int test3(int a, int b, int c, int d) {
    VM.sysWriteln(a);
    VM.sysWriteln(b);
    VM.sysWriteln(c);
    VM.sysWriteln(d);
    return a * b + c + d;
  }

  @Entrypoint
  public static void test1() {
      VM.sysWriteln("testprint");
  }

  @Entrypoint
  public static int test(int a) {
    return a + 10;
  }


  /**
   * Suppress default constructor to enforce noninstantiability.
   */
  private MemoryManager() {} // This constructor will never be invoked.

  /**
   * Initialization that occurs at <i>boot</i> time (runtime
   * initialization).  This is only executed by one processor (the
   * primordial thread).
   * @param theBootRecord the boot record. Contains information about
   * the heap size.
   */
  @Interruptible
  public static void boot(BootRecord theBootRecord) {
    DebugUtil.boot(theBootRecord);
    sysCall.sysGCInit(BootRecord.the_boot_record.tocRegister, theBootRecord.maximumHeapSize.toInt());
    RVMThread.threadBySlot[1].setHandle(sysCall.sysBindMutator(1));
    booted = true;
  }

  /**
   * Allow collection (assumes threads can be created).
   */
  @Interruptible
  public static void enableCollection() {
    sysCall.sysEnableCollection(RVMThread.getCurrentThreadSlot());
    collectionEnabled = true;
  }

  @Interruptible
  @Entrypoint
  public static void spawnCollectorThread(Address workerInstance) {
    byte[] stack = MemoryManager.newStack(StackFrameLayout.getStackSizeCollector());
    CollectorThread t = new CollectorThread(stack);
    t.setWorker(workerInstance);
    t.start();
  }

  /**
   * @return whether collection is enabled
   */
  public static boolean collectionEnabled() {
    return collectionEnabled;
  }

  @Interruptible
  public static void processCommandLineArg(String arg) {
    if (!OptionSet.gc.process(arg)) {
      VM.sysWriteln("Unrecognized command line argument: \"" + arg + "\"");
      VM.sysExit(EXIT_STATUS_BOGUS_COMMAND_LINE_ARG);
    }
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
    return Extent.fromIntZeroExtend(SysCall.sysCall.sysFreeBytes());
  }

  /**
   * Returns the amount of total memory.
   *
   * @return The amount of total memory.
   */
  public static Extent totalMemory() {
    return Extent.fromIntZeroExtend(SysCall.sysCall.sysTotalBytes());
  }

  /**
   * Returns the maximum amount of memory VM will attempt to use.
   *
   * @return The maximum amount of memory VM will attempt to use.
   */
  public static Extent maxMemory() {
    return Extent.fromIntZeroExtend(SysCall.sysCall.sysTotalBytes());
  }

  /**
   * External call to force a garbage collection.
   */
  @Interruptible
  public static void gc() { VM.sysWriteln("Called MM.gc(). This function does nothing.");}

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
    return (SysCall.sysCall.sysStartingHeapAddress().LE(address) && address.LE(sysCall.sysCall.sysLastHeapAddress())) ||
            (BOOT_IMAGE_DATA_START.LE(address) && BOOT_IMAGE_END.LE(address));
  }

  /**
   * Checks if a reference refers to an object in an in-use area of
   * memory.
   *
   * <p>References may be addresses just outside the memory region
   * allocated to the object.
   *
   * @param object the reference to be checked
   * @return <code>true</code> if the object refered to is in an
   * in-use area
   */
  @Inline
  public static boolean objectInVM(ObjectReference object) {
    return (SysCall.sysCall.sysStartingHeapAddress().LE(object.toAddress()) && object.toAddress().LE(sysCall.sysCall.sysLastHeapAddress())) ||
            (BOOT_IMAGE_DATA_START.LE(object.toAddress()) && BOOT_IMAGE_END.LE(object.toAddress()));
  }

  /**
   * Return true if address is in a space which may contain stacks
   *
   * @param address The address to be checked
   * @return true if the address is within a space which may contain stacks
   */
  public static boolean mightBeFP(Address address) {
    // In general we don't know which spaces may hold allocated stacks.
    // If we want to be more specific than the space being mapped we
    // will need to add a check in Plan that can be overriden.
    return (SysCall.sysCall.sysStartingHeapAddress().LE(address) && address.LE(sysCall.sysCall.sysLastHeapAddress())) ||
            (BOOT_IMAGE_DATA_START.LE(address) && BOOT_IMAGE_END.LE(address));
  }
  /***********************************************************************
   *
   * Allocation
   */

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
  private static boolean isPrefix(String a, byte[] b) {
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


  /***********************************************************************
   * These methods allocate memory.  Specialized versions are available for
   * particular object types.
   ***********************************************************************
   */

  /**
   * Throw an out of memory error due to an array allocation request that is
   * larger than the maximum allowed value. This is in a separate method
   * so it can be forced out of line.
   */
  @NoInline
  @UnpreemptibleNoWarn
  private static void throwLargeArrayOutOfMemoryError() {
    throw new OutOfMemoryError();
  }

  /**
   * Allocate space for runtime allocation of an object
   *
   * @param mutator The mutator instance to be used for this allocation
   * @param bytes The size of the allocation in bytes
   * @param align The alignment requested; must be a power of 2.
   * @param offset The offset at which the alignment is desired.
   * @param allocator The MMTk allocator to be used (if allocating)
   * @param site Allocation site.
   * @return The first byte of a suitably sized and aligned region of memory.
   */
  @Inline
  private static Address allocateSpace(Selected.Mutator mutator, int bytes, int align, int offset, int allocator,
                                       int site) {
    /* MMTk requests must be in multiples of MIN_ALIGNMENT */
    bytes = org.jikesrvm.runtime.Memory.alignUp(bytes, MIN_ALIGNMENT);
    /* Now make the request */
    Address region;
    region = mutator.alloc(bytes, align, offset, allocator, site);
    return region;
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
   * Checks if the object can move. This information is useful to
   *  optimize some JNI calls.
   *
   * @param obj the object in question
   * @return {@code true} if this object can never move, {@code false}
   *   if it can move.
   */
  @Pure
  public static boolean willNeverMove(Object obj) {
    return sysCall.sysWillNeverMove(ObjectReference.fromObject(obj));
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
    //FinalizableProcessor.addCandidate(object);
  }

  /**
   * Gets an object from the list of objects that are to be reclaimed
   * and need to have their <code>finalize</code> method called.
   *
   * @return the object needing to be finialized
   */
  @Unpreemptible("Non-preemptible but may yield if finalizable table is being grown")
  public static Object getFinalizedObject() {
    //return FinalizableProcessor.getForFinalize();
    VM.sysFail("Have not yet implemented getFinalizedObject for RustMMTk");
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
    //ReferenceProcessor.addSoftCandidate(obj,ObjectReference.fromObject(referent));
  }

  /**
   * Add a weak reference to the list of weak references.
   *
   * @param obj the weak reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addWeakReference(WeakReference<?> obj, Object referent) {
    //ReferenceProcessor.addWeakCandidate(obj,ObjectReference.fromObject(referent));
  }

  /**
   * Add a phantom reference to the list of phantom references.
   *
   * @param obj the phantom reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addPhantomReference(PhantomReference<?> obj, Object referent) {
    //ReferenceProcessor.addPhantomCandidate(obj,ObjectReference.fromObject(referent));
  }

  /***********************************************************************
   *
   * Tracing
   */

  /***********************************************************************
   *
   * Heap size and heap growth
   */

  /**
   * Return the max heap size in bytes (as set by -Xmx).
   *
   * @return The max heap size in bytes (as set by -Xmx).
   */
  public static Extent getMaxHeapSize() {
    return Extent.fromIntZeroExtend(SysCall.sysCall.sysTotalBytes());
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
    //vmType.setMMAllocator(pickAllocatorForType(vmType));
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
    return !obj.isNull() && // todo
            SysCall.sysCall.sysStartingHeapAddress().LE(obj.toAddress()) &&
            SysCall.sysCall.sysLastHeapAddress().GT(obj.toAddress()) &&
            SysCall.sysCall.sysStartingHeapAddress().LE(ObjectReference.fromObject(ObjectModel.getTIB(obj)).toAddress()) &&
            SysCall.sysCall.sysLastHeapAddress().GT(ObjectReference.fromObject(ObjectModel.getTIB(obj)).toAddress());
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


  @Entrypoint
  @Unpreemptible
  public static void blockForGC() {
    RVMThread t = RVMThread.getCurrentThread();
    t.assertAcceptableStates(RVMThread.IN_JAVA, RVMThread.IN_JAVA_TO_BLOCK);
    RVMThread.observeExecStatusAtSTW(t.getExecStatus());
    t.block(RVMThread.gcBlockAdapter);
  }
}

