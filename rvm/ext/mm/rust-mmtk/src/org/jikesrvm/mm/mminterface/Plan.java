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
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

/**
 * This abstract class implements the global core functionality for all
 * memory management schemes.  All global MMTk plans should inherit from
 * this class.<p>
 *
 * All plans make a clear distinction between <i>global</i> and
 * <i>thread-local</i> activities, and divides global and local state
 * into separate class hierarchies.  Global activities must be
 * synchronized, whereas no synchronization is required for
 * thread-local activities.  There is a single instance of Plan (or the
 * appropriate sub-class), and a 1:1 mapping of PlanLocal to "kernel
 * threads" (aka CPUs).  Thus instance
 * methods of PlanLocal allow fast, unsynchronized access to functions such as
 * allocation and collection.
 *
 * The global instance defines and manages static resources
 * (such as memory and virtual memory resources).  This mapping of threads to
 * instances is crucial to understanding the correctness and
 * performance properties of MMTk plans.
 */
@Uninterruptible
public class Plan {
  /****************************************************************************
   * Constants
   */

  /**
   *
   */

  /* GC State */
  public static final int NOT_IN_GC = 0; // this must be zero for C code
  public static final int GC_PREPARE = 1; // before setup and obtaining root
  public static final int GC_PROPER = 2;

  /* Space Size Constants. */
  public static final boolean USE_CODE_SPACE = true;

  /* Allocator Constants */
  public static final int ALLOC_DEFAULT = 0;
  public static final int ALLOC_NON_REFERENCE = 1;
  public static final int ALLOC_NON_MOVING = 2;
  public static final int ALLOC_IMMORTAL = 3;
  public static final int ALLOC_LOS = 4;
  public static final int ALLOC_PRIMITIVE_LOS = 5;
  public static final int ALLOC_GCSPY = 6;
  public static final int ALLOC_CODE = 7;
  public static final int ALLOC_LARGE_CODE = 8;
  public static final int ALLOC_HOT_CODE = USE_CODE_SPACE ? ALLOC_CODE : ALLOC_DEFAULT;
  public static final int ALLOC_COLD_CODE = USE_CODE_SPACE ? ALLOC_CODE : ALLOC_DEFAULT;
  public static final int ALLOC_STACK = ALLOC_LOS;
  public static final int ALLOCATORS = 9;
  public static final int DEFAULT_SITE = -1;

  /* Miscellaneous Constants */
//  public static final int LOS_SIZE_THRESHOLD = SegregatedFreeListSpace.MAX_CELL_SIZE;
  public static final int NON_PARTICIPANT = 0;
  public static final boolean GATHER_WRITE_BARRIER_STATS = false;

  public static int pretenureThreshold = Integer.MAX_VALUE;

  /** Support for allocation-site identification */
  protected static int allocationSiteCount = 0;


  /****************************************************************************
   * Constructor.
   */
  public Plan() {
  }

  /****************************************************************************
   * The complete boot Sequence is:
   *
   *  1. enableAllocation: allow allocation (but not collection).
   *  2. processOptions  : the VM has parsed/prepared options for MMTk to react to.
   *  3. enableCollection: the VM can support the spawning of MMTk collector contexts.
   *  4. fullyBooted     : control is just about to be given to application code.
   */

  /**
   * The enableAllocation method is called early in the boot process to allow
   * allocation.
   */
  @Interruptible
  public void enableAllocation() {
    VM.sysFail("did not implement enableAllocation");
  }

  /**
   * The processOptions method is called by the runtime immediately after
   * command-line arguments are available. Allocation must be supported
   * prior to this point because the runtime infrastructure may require
   * allocation in order to parse the command line arguments.  For this
   * reason all plans should operate gracefully on the default minimum
   * heap size until the point that processOptions is called.
   */
  @Interruptible
  public void processOptions() {
    VM.sysFail("did not implement processOptions");
  }

  /**
   * The enableCollection method is called by the runtime after it is
   * safe to spawn collector contexts and allow garbage collection.
   */
  @Interruptible
  public void enableCollection() {
    int actualThreadCount = determineThreadCount();

    preCollectorSpawn();

    spawnCollectorThreads(actualThreadCount);
  }

  /**
   * Determines the number of threads that will be used for collection.<p>
   *
   * Collectors that need fine-grained control over the number of spawned collector
   * threads may override this method. Subclasses must ensure that the return value
   * of this method is consistent with the number of collector threads in
   * the active plan.
   *
   * @return number of threads to be used for collection
   *  of collectors
   */
  @Interruptible("Options methods and Math.min are interruptible")
  protected int determineThreadCount() {
    VM.sysFail("did not implement determineThreadCount");
    return 0;
  }

  /**
   * Prepares for spawning of collectors.<p>
   *
   * This is a good place to do initialization work that depends on
   * options that are only known at runtime. Collectors must keep allocation
   * to a minimum because collection is not yet enabled.
   */
  @Interruptible
  protected void preCollectorSpawn() {
    // most collectors do not need to do any work here
  }

  /**
   * Spawns the collector threads.<p>
   *
   * Collection is enabled after this method returns.
   *
   * @param numThreads the number of collector threads to spawn
   */
  @Interruptible("Spawning collector threads requires allocation")
  protected void spawnCollectorThreads(int numThreads) {
    VM.sysFail("did not implement spawnCollectorThreads");
  }

  @Interruptible
  public void fullyBooted() {
    VM.sysFail("did not implement fullyBooted");
  }

  /**
   * The VM is about to exit. Perform any clean up operations.
   *
   * @param value The exit value
   */
  @Interruptible
  public void notifyExit(int value) {
    VM.sysFail("did not implement notifyExit");
  }

  /**
   * Any Plan can override this to provide additional plan specific
   * timing information.
   *
   * @param totals Print totals
   */
  protected void printDetailedTiming(boolean totals) {}

  /**
   * Perform any required write barrier action when installing an object reference
   * a boot time.
   *
   * @param reference the reference value that is to be stored
   * @return The raw value to be stored
   */
  public Word bootTimeWriteBarrier(Word reference) {
    return reference;
  }

  /**
   * Performs any required initialization of the GC portion of the header.
   * Called for objects created at boot time.
   *
   * @param object the Address representing the storage to be initialized
   * @param typeRef the type reference for the instance being created
   * @param size the number of bytes allocated by the GC system for
   * this object.
   * @return The new value of the status word
   */
  public byte setBuildTimeGCByte(Address object, ObjectReference typeRef, int size) {
    if (HeaderByte.NEEDS_UNLOGGED_BIT) {
      return HeaderByte.UNLOGGED_BIT;
    } else {
      return 0;
    }
  }

  /****************************************************************************
   * Allocation
   */

  /**
   * @param compileTime is this a call by the compiler?
   * @return an allocation site
   *
   */
  public static int getAllocationSite(boolean compileTime) {
    if (compileTime) // a new allocation site is being compiled
      return allocationSiteCount++;
    else             // an anonymous site
      return DEFAULT_SITE;
  }

  /****************************************************************************
   * Collection.
   */

  /**
   * Perform a (global) collection phase.
   *
   * @param phaseId The unique id of the phase to perform.
   */
  //todo public abstract void collectionPhase(short phaseId);

  /**
   * Replace a phase.
   *
   * @param oldScheduledPhase The scheduled phase to insert after
   * @param scheduledPhase The scheduled phase to insert
   */
  @Interruptible
  public void replacePhase(int oldScheduledPhase, int scheduledPhase) {
    VM.sysFail("did not implement replacePhase");
  }

  /**
   * Insert a phase.
   *
   * @param markerScheduledPhase The scheduled phase to insert after
   * @param scheduledPhase The scheduled phase to insert
   */
  @Interruptible
  public void insertPhaseAfter(int markerScheduledPhase, int scheduledPhase) {
    VM.sysFail("did not implement insertPhaseAfter");
  }

  /**
   * @return Whether last GC was an exhaustive attempt to collect the heap.  For many collectors this is the same as asking whether the last GC was a full heap collection.
   */
  public boolean lastCollectionWasExhaustive() {
    return lastCollectionFullHeap();
  }

  /**
   * @return Whether last GC is a full GC.
   */
  public boolean lastCollectionFullHeap() {
    return true;
  }

  /**
   * @return Is last GC a full collection?
   */
  public static boolean isEmergencyCollection() {
    return emergencyCollection;
  }

  /**
   * Force the next collection to be full heap.
   */
  public void forceFullHeapCollection() {}

  /**
   * @return Is current GC only collecting objects allocated since last GC.
   */
  public boolean isCurrentGCNursery() {
    return false;
  }

  private long lastStressPages = 0;

  /**
   * Return the expected reference count. For non-reference counting
   * collectors this becomes a {@code true/false} relationship.
   *
   * @param object The object to check.
   * @param sanityRootRC The number of root references to the object.
   * @return The expected (root excluded) reference count.
   */
  public int sanityExpectedRC(ObjectReference object, int sanityRootRC) {
    VM.sysFail("did not implement sanityExpectedRC");
    return 0;
  }


  /**
   * @return {@code true} is a stress test GC is required
   */
  @Inline
  public final boolean stressTestGCRequired() {
    VM.sysFail("did not implement stressTestGCRequired");
    return false;
  }

  /****************************************************************************
   * GC State
   */

  /**
   *
   */
  protected static boolean userTriggeredCollection;
  protected static boolean internalTriggeredCollection;
  protected static boolean lastInternalTriggeredCollection;
  protected static boolean emergencyCollection;
  protected static boolean stacksPrepared;

  private static boolean initialized = false;

  private static int gcStatus = NOT_IN_GC; // shared variable

  /** @return Is the memory management system initialized? */
  public static boolean isInitialized() {
    return initialized;
  }

  /**
   * Return {@code true} if stacks have been prepared in this collection cycle.
   *
   * @return {@code true} if stacks have been prepared in this collection cycle.
   */
  public static boolean stacksPrepared() {
    return stacksPrepared;
  }
  /**
   * Return {@code true} if a collection is in progress.
   *
   * @return {@code true} if a collection is in progress.
   */
  public static boolean gcInProgress() {
    return gcStatus != NOT_IN_GC;
  }

  /**
   * Return {@code true} if a collection is in progress and past the preparatory stage.
   *
   * @return {@code true} if a collection is in progress and past the preparatory stage.
   */
  public static boolean gcInProgressProper() {
    return gcStatus == GC_PROPER;
  }

  /**
   * Sets the GC status.
   *
   * @param s The new GC status.
   */
  public static void setGCStatus(int s) {
    VM.sysFail("did not implement setGCStatus");
  }

  /**
   * Print pre-collection statistics.
   */
  public void printPreStats() {
    VM.sysFail("did not implement printPreStats");
  }

  /**
   * Print out statistics at the end of a GC
   */
  public final void printPostStats() {
    VM.sysFail("did not implement printPostStats");
  }

  public final void printUsedPages() {
    VM.sysFail("did not implement printUsedPages");
  }

  /**
   * The application code has requested a collection.
   */
  @Unpreemptible
  public static void handleUserCollectionRequest() {
    VM.sysFail("did not implement handleUserCollectionRequest");
  }

  /**
   * MMTK has requested stop-the-world activity (e.g., stw within a concurrent gc).
   */
  public static void triggerInternalCollectionRequest() {
    VM.sysFail("did not implement triggerInternalCollectionRequest");
  }

  /**
   * Reset collection state information.
   */
  public static void resetCollectionTrigger() {
    lastInternalTriggeredCollection = internalTriggeredCollection;
    internalTriggeredCollection = false;
    userTriggeredCollection = false;
  }

  /**
   * @return {@code true} if this collection was triggered by application code.
   */
  public static boolean isUserTriggeredCollection() {
    return userTriggeredCollection;
  }

  /**
   * @return {@code true} if this collection was triggered internally.
   */
  public static boolean isInternalTriggeredCollection() {
    return lastInternalTriggeredCollection;
  }

  /****************************************************************************
   * Harness
   */

  /**
   *
   */
  protected static boolean insideHarness = false;

  /**
   * Generic hook to allow benchmarks to be harnessed.  A plan may use
   * this to perform certain actions prior to the commencement of a
   * benchmark, such as a full heap collection, turning on
   * instrumentation, etc.  By default we do a full heap GC,
   * and then start stats collection.
   */
  @Interruptible
  public static void harnessBegin() {
    VM.sysFail("did not implement harnessBegin");
  }

  /**
   * Generic hook to allow benchmarks to be harnessed.  A plan may use
   * this to perform certain actions after the completion of a
   * benchmark, such as a full heap collection, turning off
   * instrumentation, etc.  By default we stop all statistics objects
   * and print their values.
   */
  @Interruptible
  public static void harnessEnd()  {
    VM.sysFail("did not implement harnessEnd");
  }

  /****************************************************************************
   * VM Accounting
   */

  /* Global accounting and static access */

  /**
   * Return the amount of <i>free memory</i>, in bytes (where free is
   * defined as not in use).  Note that this may overstate the amount
   * of <i>available memory</i>, which must account for unused memory
   * that is held in reserve for copying, and therefore unavailable
   * for allocation.
   *
   * @return The amount of <i>free memory</i>, in bytes (where free is
   * defined as not in use).
   */
  public static Extent freeMemory() {
    return totalMemory().minus(usedMemory());
  }

  /**
   * Return the amount of <i>available memory</i>, in bytes.  Note
   * that this accounts for unused memory that is held in reserve
   * for copying, and therefore unavailable for allocation.
   *
   * @return The amount of <i>available memory</i>, in bytes.
   */
  public static Extent availableMemory() {
    return totalMemory().minus(reservedMemory());
  }

  /**
   * Return the amount of <i>memory in use</i>, in bytes.  Note that
   * this excludes unused memory that is held in reserve for copying,
   * and therefore unavailable for allocation.
   *
   * @return The amount of <i>memory in use</i>, in bytes.
   */
  public static Extent usedMemory() {
    VM.sysFail("did not implement usedMemory");
    return Extent.zero();
  }

  /**
   * Return the amount of <i>memory in use</i>, in bytes.  Note that
   * this includes unused memory that is held in reserve for copying,
   * and therefore unavailable for allocation.
   *
   * @return The amount of <i>memory in use</i>, in bytes.
   */
  public static Extent reservedMemory() {
    VM.sysFail("did not implement reservedMemory");
    return Extent.zero();
  }

  /**
   * Return the total amount of memory managed to the memory
   * management system, in bytes.
   *
   * @return The total amount of memory managed to the memory
   * management system, in bytes.
   */
  public static Extent totalMemory() {
    VM.sysFail("did not implement totalMemory");
    return Extent.zero();
  }

  /* Instance methods */

  /**
   * Return the total amount of memory managed to the memory
   * management system, in pages.
   *
   * @return The total amount of memory managed to the memory
   * management system, in pages.
   */
  public final int getTotalPages() {
    VM.sysFail("did not implement getTotalPages");
    return 0;
  }

  /**
   * Return the number of pages available for allocation.
   *
   * @return The number of pages available for allocation.
   */
  public int getPagesAvail() {
    return getTotalPages() - getPagesReserved();
  }

  /**
   * Return the number of pages reserved for use given the pending
   * allocation.  Sub-classes must override the getCopyReserve method,
   * as the arithmetic here is fixed.
   *
   * @return The number of pages reserved given the pending
   * allocation, including space reserved for copying.
   */
  public final int getPagesReserved() {
    return getPagesUsed() + getCollectionReserve();
  }

  /**
   * Return the number of pages reserved for collection.
   * In most cases this is a copy reserve, all subclasses that
   * manage a copying space must add the copying contribution.
   *
   * @return The number of pages reserved given the pending
   * allocation, including space reserved for collection.
   */
  public int getCollectionReserve() {
    return 0;
  }

  /**
   * Return the number of pages reserved for use given the pending
   * allocation.
   *
   * @return The number of pages reserved given the pending
   * allocation, excluding space reserved for copying.
   */
  public int getPagesUsed() {
    VM.sysFail("did not implement getPagesUsed");
    return 0;
  }

  /****************************************************************************
   * Internal read/write barriers.
   */

  /**
   * Store an object reference
   *
   * @param slot The location of the reference
   * @param value The value to store
   */
  @Inline
  public void storeObjectReference(Address slot, ObjectReference value) {
    slot.store(value);
  }

  /**
   * Load an object reference
   *
   * @param slot The location of the reference
   * @return the object reference loaded from slot
   */
  @Inline
  public ObjectReference loadObjectReference(Address slot) {
    return slot.loadObjectReference();
  }

  /****************************************************************************
   * Collection.
   */

  /**
   * This method controls the triggering of an atomic phase of a concurrent
   * collection. It is called periodically during allocation.
   *
   * @return <code>true</code> if a collection is requested by the plan.
   */
  protected boolean concurrentCollectionRequired() {
    return false;
  }

  /**
   * Start GCspy server.
   *
   * @param port The port to listen on,
   * @param wait Should we wait for a client to connect?
   */
  @Interruptible
  public void startGCspyServer(int port, boolean wait) {
    VM.sysFail("did not implement startGCspyServer");
  }

  /**
   * Can this object ever move.  Used by the VM to make decisions about
   * whether it needs to copy IO buffers etc.
   *
   * @param object The object in question
   * @return <code>true</code> if it is not possible that the object will ever move.
   */
  public boolean willNeverMove(ObjectReference object) {
    VM.sysFail("did not implement willNeverMove");
    return false;
  }

  /****************************************************************************
   * Specialized Methods
   */

  /**
   * Registers specialized methods.
   */
  @Interruptible
  protected void registerSpecializedMethods() {}

}
