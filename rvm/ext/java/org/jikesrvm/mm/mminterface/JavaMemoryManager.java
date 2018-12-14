package org.jikesrvm.mm.mminterface;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.classloader.SpecializedMethod;
import org.jikesrvm.classloader.TypeReference;
import org.jikesrvm.mm.mmtk.ReferenceProcessor;
import org.jikesrvm.mm.mmtk.SynchronizedCounter;
import org.jikesrvm.objectmodel.ITable;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Callbacks;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.CollectorContext;
import org.mmtk.plan.Plan;
import org.mmtk.policy.Space;
import org.mmtk.utility.Memory;
import org.mmtk.utility.heap.HeapGrowthManager;
import org.mmtk.utility.heap.layout.HeapLayout;
import org.mmtk.utility.options.Options;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;

import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import static org.jikesrvm.HeapLayoutConstants.*;
import static org.mmtk.utility.Constants.MIN_ALIGNMENT;

@Uninterruptible
public class JavaMemoryManager extends AbstractMemoryManager {

  /***********************************************************************
   *
   * Initialization
   */
  private static final boolean CHECK_MEMORY_IS_ZEROED = false;

  @Interruptible
  public static void boot(BootRecord theBootRecord) {
    Extent pageSize = BootRecord.the_boot_record.bytesInPage;
    org.jikesrvm.runtime.Memory.setPageSize(pageSize);
    HeapLayout.mmapper.markAsMapped(BOOT_IMAGE_DATA_START, BOOT_IMAGE_DATA_SIZE);
    HeapLayout.mmapper.markAsMapped(BOOT_IMAGE_CODE_START, BOOT_IMAGE_CODE_SIZE);
    HeapGrowthManager.boot(theBootRecord.initialHeapSize, theBootRecord.maximumHeapSize);
    DebugUtil.boot(theBootRecord);
    Selected.Plan.get().enableAllocation();
    SynchronizedCounter.boot();

    Callbacks.addExitMonitor(new Callbacks.ExitMonitor() {
      @Override
      public void notifyExit(int value) {
        Selected.Plan.get().notifyExit(value);
      }
    });

    booted = true;
  }

  /**
   * Perform postBoot operations such as dealing with command line
   * options (this is called as soon as options have been parsed,
   * which is necessarily after the basic allocator boot).
   */
  @Interruptible
  public static void postBoot() {
    Selected.Plan.get().processOptions();
    if (Options.noReferenceTypes.getValue()) {
      RVMType.JavaLangRefReferenceReferenceField.makeTraced();
    }

    if (VM.BuildWithGCSpy) {
      // start the GCSpy interpreter server
      AbstractMemoryManager.startGCspyServer();
    }
  }

  /**
   * Allow collection (assumes threads can be created).
   */
  @Interruptible
  public static void enableCollection() {
    collectionEnabled = true;
    Selected.Plan.get().enableCollection();
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
    if (Plan.gcInProgressProper()) {
      ObjectReference ref = ObjectReference.fromObject(object);
      if (Space.isMovable(ref)) {
        VM.sysWriteln("GC modifying a potentially moving object via Java (i.e. not magic)");
        VM.sysWriteln("  obj = ", ref);
        RVMType t = Magic.getObjectType(object);
        VM.sysWrite(" type = ");
        VM.sysWriteln(t.getDescriptor());
        VM.sysFail("GC modifying a potentially moving object via Java (i.e. not magic)");
      }
    }
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
    return Plan.freeMemory();
  }

  /**
   * Returns the amount of total memory.
   *
   * @return The amount of total memory.
   */
  public static Extent totalMemory() {
    return Plan.totalMemory();
  }

  /**
   * Returns the maximum amount of memory VM will attempt to use.
   *
   * @return The maximum amount of memory VM will attempt to use.
   */
  public static Extent maxMemory() {
    return HeapGrowthManager.getMaxHeapSize();
  }

  /**
   * External call to force a garbage collection.
   */
  @Interruptible
  public static void gc() {
    Selected.Plan.handleUserCollectionRequest();
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
    return Space.isMappedAddress(address);
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
    return Space.isMappedObject(object);
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
    return Space.isMappedAddress(address);
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
    region = context.allocCopy(from, bytes, align, offset, allocator);
    /* TODO: if (Stats.GATHER_MARK_CONS_STATS) Plan.mark.inc(bytes); */
    if (CHECK_MEMORY_IS_ZEROED) Memory.assertIsZeroed(region, bytes);

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
    if (!VM.runningVM) {
      return ITable.allocate(size);
    }
    return (ITable)newRuntimeTable(size, RVMType.ITableType);
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
    return Selected.Plan.get().willNeverMove(ObjectReference.fromObject(obj));
  }
  /**
   * @param obj the object in question
   * @return whether the object is immortal
   */
  @Pure
  public static boolean isImmortal(Object obj) {
    return Space.isImmortal(ObjectReference.fromObject(obj));
  }
  /**
   * Add a soft reference to the list of soft references.
   *
   * @param obj the soft reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addSoftReference(SoftReference<?> obj, Object referent) {
      ReferenceProcessor.addSoftCandidate(obj, ObjectReference.fromObject(referent));
  }
  /**
   * Add a weak reference to the list of weak references.
   *
   * @param obj the weak reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addWeakReference(WeakReference<?> obj, Object referent) {
      ReferenceProcessor.addWeakCandidate(obj, ObjectReference.fromObject(referent));
  }
  /**
   * Add a phantom reference to the list of phantom references.
   *
   * @param obj the phantom reference to be added to the list
   * @param referent the object that the reference points to
   */
  @Interruptible
  public static void addPhantomReference(PhantomReference<?> obj, Object referent) {
      ReferenceProcessor.addPhantomCandidate(obj, ObjectReference.fromObject(referent));
  }

  /**
   * Returns true if GC is in progress.
   *
   * @return True if GC is in progress.
   */
  public static boolean gcInProgress() {
    return Plan.gcInProgress();
  }

  /**
   * Flush the mutator context.
   */
  public static void flushMutatorContext() {
    Selected.Mutator.get().flush();
  }
  /**
   * Initialize a specified specialized method.
   *
   * @param id the specializedMethod
   * @return the created specialized scan method
   */
  @Interruptible
  public static SpecializedMethod createSpecializedMethod(int id) {
    if (VM.VerifyAssertions) {
      VM._assert(SpecializedScanMethod.ENABLED);
      VM._assert(id < Selected.Constraints.get().numSpecializedScans());
    }

    /* What does the plan want us to specialize this to? */
    Class<?> traceClass = Selected.Plan.get().getSpecializedScanClass(id);

    /* Create the specialized method */
    return new SpecializedScanMethod(id, TypeReference.findOrCreate(traceClass));
  }


}
