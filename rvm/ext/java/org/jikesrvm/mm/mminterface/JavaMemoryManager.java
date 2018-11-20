package org.jikesrvm.mm.mminterface;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.RVMType;
import org.jikesrvm.mm.mmtk.SynchronizedCounter;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.Callbacks;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.Plan;
import org.mmtk.policy.Space;
import org.mmtk.utility.heap.HeapGrowthManager;
import org.mmtk.utility.heap.layout.HeapLayout;
import org.mmtk.utility.options.Options;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Extent;
import org.vmmagic.unboxed.ObjectReference;

import static org.jikesrvm.HeapLayoutConstants.*;

@Uninterruptible
public class JavaMemoryManager extends AbstractMemoryManager {

  /***********************************************************************
   *
   * Initialization
   */

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


}
