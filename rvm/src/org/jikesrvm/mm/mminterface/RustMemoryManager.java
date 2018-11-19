package org.jikesrvm.mm.mminterface;

import org.jikesrvm.VM;
import org.jikesrvm.architecture.StackFrameLayout;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.options.OptionSet;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Magic;
import org.jikesrvm.scheduler.RVMThread;
import org.mmtk.plan.Plan;
import org.mmtk.policy.Space;
import org.mmtk.utility.heap.HeapGrowthManager;
import org.mmtk.utility.options.Options;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;

import static org.jikesrvm.runtime.ExitStatus.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG;
import static org.jikesrvm.runtime.SysCall.sysCall;

@Uninterruptible
public class RustMemoryManager extends GeneralMemoryManager {

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
    byte[] stack = GeneralMemoryManager.newStack(StackFrameLayout.getStackSizeCollector());
    CollectorThread t = new CollectorThread(stack, null);
    t.setWorker(workerInstance);
    t.start();
  }


}
