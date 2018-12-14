package org.jikesrvm.mm.mminterface;

import org.jikesrvm.VM;
import org.jikesrvm.architecture.StackFrameLayout;
import org.jikesrvm.classloader.SpecializedMethod;
import org.jikesrvm.objectmodel.ITable;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.scheduler.RVMThread;
import org.mmtk.plan.CollectorContext;
import org.mmtk.utility.heap.HeapGrowthManager;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;

import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.mmtk.utility.Constants.MIN_ALIGNMENT;

@Uninterruptible
public class RustMemoryManager extends AbstractMemoryManager {

  /***********************************************************************
   *
   * Class variables
   */

  private static final int     verboseUnimplemented = 0;

  /***********************************************************************
   *
   * Initialization
   */

  /**
   * Initialization that occurs at <i>boot</i> time (runtime
   * initialization).  This is only executed by one processor (the
   * primordial thread).
   * @param theBootRecord the boot record. Contains information about
   * the heap size.
   */
  @Interruptible
  public static void boot(BootRecord theBootRecord) {
    sysCall.sysGCInit(BootRecord.the_boot_record.tocRegister, theBootRecord.maximumHeapSize.toInt());
    RVMThread.threadBySlot[1].setHandle(sysCall
            .sysBindMutator(Magic.objectAsAddress(RVMThread.threadBySlot[1])));
    booted = true;
  }

  /**
   * Perform postBoot operations such as dealing with command line
   * options (this is called as soon as options have been parsed,
   * which is necessarily after the basic allocator boot).
   */
  @Interruptible
  public static void postBoot() {
    if (verboseUnimplemented > 3) {
      VM.sysFail("postBoot() unimplemented");
    }
  }

  /**
   * Allow collection (assumes threads can be created).
   */
  @Interruptible
  public static void enableCollection() {
    sysCall.sysEnableCollection(Magic
            .objectAsAddress(RVMThread.getCurrentThread()));
    collectionEnabled = true;
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
    /* Make sure that during GC, we don't update on a possibly moving object.
       Such updates are dangerous because they can be lost.
     */
    sysCall.sysModifyCheck(ObjectReference.fromObject(object));
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
    return Extent.fromIntZeroExtend(sysCall.sysFreeBytes());
  }

  /**
   * Returns the amount of total memory.
   *
   * @return The amount of total memory.
   */
  public static Extent totalMemory() {
    return Extent.fromIntZeroExtend(sysCall.sysTotalBytes());
  }

  /**
   * Returns the maximum amount of memory VM will attempt to use.
   *
   * @return The maximum amount of memory VM will attempt to use.
   */
  public static Extent maxMemory() {
    if (verboseUnimplemented > 3) {
      VM.sysFail("maxMemory() unimplemented");
    }

    return HeapGrowthManager.getMaxHeapSize();
  }

  /**
   * External call to force a garbage collection.
   */
  @Interruptible
  public static void gc() {
    sysCall.alignedHandleUserCollectionRequest(Magic.objectAsAddress(RVMThread
            .getCurrentThread()));
  }


  /****************************************************************************
   *
   * Check references, log information about references
   */


  /**
   * Checks if a reference is valid.
   *
   * @param ref the address to be checked
   * @return <code>true</code> if the reference is valid
   */
  @Inline
  public static boolean validRef(ObjectReference ref) {
    if (booted) {
      return sysCall.sysIsValidRef(ref);
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
    return sysCall.is_mapped_address(address);
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
    return sysCall.is_mapped_object(object);
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
    return sysCall.is_mapped_address(address);
  }

  /**
   * Allocate space for GC-time copying of an object
   *
   * @param context The collector context to be used for this allocation
   * @param bytes The size of the allocation in bytes
   * @param allocator the allocator associated with this request
   * @param align The alignment requested; must be a power of 2.
   * @param offset The offset at which the alignment is desired.
   * @param from The source object from which this is to be copied
   * @return The first byte of a suitably sized and aligned region of memory.
   */
  @Inline
  public static Address allocateSpace(CollectorContext context, int bytes, int align, int offset, int allocator,
                                      ObjectReference from) {
    /* MMTk requests must be in multiples of MIN_ALIGNMENT */
    bytes = org.jikesrvm.runtime.Memory.alignUp(bytes, MIN_ALIGNMENT);

    /* Now make the request */
    Address region;
    VM.sysFail("Tried to allocate in collector space for non-collecting plan");
    region = null;
    /* TODO: if (Stats.GATHER_MARK_CONS_STATS) Plan.mark.inc(bytes); */

    return region;
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
    if (verboseUnimplemented > 1) {
      VM.sysFail("newITable unimplemented()");
    }
    return null;
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

  /**
   * @param obj the object in question
   * @return whether the object is immortal
   */
  @Pure
  public static boolean isImmortal(Object obj) {
    if (verboseUnimplemented > 1) {
      VM.sysFail("isImmortal unimplemented()");
    }
    return false;
  }

  /**
   * Add a soft reference to the list of soft references.
   *
   * @param obj the soft reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addSoftReference(SoftReference<?> obj, Object referent) {
      sysCall.add_soft_candidate(Magic.objectAsAddress(obj),
              Magic.objectAsAddress(referent));
  }

  /**
   * Add a weak reference to the list of weak references.
   *
   * @param obj the weak reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addWeakReference(WeakReference<?> obj, Object referent) {
      sysCall.add_weak_candidate(Magic.objectAsAddress(obj),
              Magic.objectAsAddress(referent));
  }


  /**
   * Add a phantom reference to the list of phantom references.
   *
   * @param obj the phantom reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addPhantomReference(PhantomReference<?> obj, Object referent) {
      sysCall.add_phantom_candidate(Magic.objectAsAddress(obj),
              Magic.objectAsAddress(referent));
  }

  /**
   * Returns true if GC is in progress.
   *
   * @return True if GC is in progress.
   */
  public static boolean gcInProgress() {
    if (verboseUnimplemented > 1) {
      VM.sysFail("gcInProgress unimplemented()");
    }
    return false;
  }

  /**
   * Flush the mutator context.
   */
  public static void flushMutatorContext() {
    if (verboseUnimplemented > 1) {
      VM.sysFail("flushMutatorContext unimplemented()");
    }
  }

  /**
   * Initialize a specified specialized method.
   *
   * @param id the specializedMethod
   * @return the created specialized scan method
   */
  @Interruptible
  public static SpecializedMethod createSpecializedMethod(int id) {
    if (VM.BuildWithRustMMTk && verboseUnimplemented > 2) {
      VM.sysFail("createSpecializedMethod unimplemented()");
    }
    return null;
  }



  /***************************
   *
   * Entrypoints
   *
   * These should be refactored out eventually
   */
  @Entrypoint
  @Unpreemptible
  public static void blockForGC() {
    RVMThread t = RVMThread.getCurrentThread();
    t.assertAcceptableStates(RVMThread.IN_JAVA, RVMThread.IN_JAVA_TO_BLOCK);
    RVMThread.observeExecStatusAtSTW(t.getExecStatus());
    t.block(RVMThread.gcBlockAdapter);
  }

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

  @Entrypoint
  public static int test2(int a, int b) {
    return a + b;
  }

  @Entrypoint
  public static int test3(int a, int b, int c, int d) {
    if (VM.verboseBoot != 0) {
      VM.sysWriteln(a);
      VM.sysWriteln(b);
      VM.sysWriteln(c);
      VM.sysWriteln(d);
    }
    return a * b + c + d;
  }

  @Entrypoint
  public static void test1() {
    if (VM.verboseBoot != 0) {
      VM.sysWriteln("testprint");
    }
  }

  @Entrypoint
  public static int test(int a) {
    return a + 10;
  }

  @Entrypoint
  @UninterruptibleNoWarn
  public static void outOfMemory() {
    throw RVMThread.getOutOfMemoryError();
  }


  @Interruptible
  @Entrypoint
  public static void spawnCollectorThread(Address workerInstance) {
    byte[] stack = AbstractMemoryManager.newStack(StackFrameLayout.getStackSizeCollector());
    CollectorThread t = new CollectorThread(stack, null);
    t.setWorker(workerInstance);
    t.start();
  }


}
