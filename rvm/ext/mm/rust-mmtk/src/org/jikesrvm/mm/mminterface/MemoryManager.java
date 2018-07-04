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
import org.jikesrvm.objectmodel.*;
import org.jikesrvm.runtime.BootRecord;
import org.jikesrvm.runtime.SysCall;
import org.jikesrvm.scheduler.RVMThread;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;

import static org.jikesrvm.HeapLayoutConstants.*;
import static org.jikesrvm.runtime.CommandLineArgs.stringToBytes;
import static org.jikesrvm.runtime.ExitStatus.EXIT_STATUS_BOGUS_COMMAND_LINE_ARG;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MIN_ALIGNMENT;

/**
 * The interface that the MMTk memory manager presents to Jikes RVM
 */
@Uninterruptible
public final class MemoryManager extends AbstractMemoryManager {

  /***********************************************************************
   *
   * Class variables
   */

  private static final boolean traceAllocator = false;
  private static final boolean traceAllocation = false;

  /***********************************************************************
   *
   * Initialization
   */

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
    DebugUtil.boot(theBootRecord); // note: do we need this?
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
  public static void processCommandLineArg(String arg) {
    // FIXME this is a hackish solution
    // First handle the "option commands"
    if (arg.equals("help")) {
      VM.sysWriteln("No help available for Rust :(");
    }
    // Required format of arg is 'name=value'
    // Split into 'name' and 'value' strings
    int split = arg.indexOf('=');
    if (split == -1) {
      VM.sysWriteln("  Illegal option specification!\n  \"" + arg +
              "\" must be specified as a name-value pair in the form of option=value");
      VM.sysWriteln("Unrecognized command line argument: \"" + arg + "\"");
      VM.sysExit(EXIT_STATUS_BOGUS_COMMAND_LINE_ARG);
    }

    String name = arg.substring(0,split);
    String value = arg.substring(split + 1);
    byte[] nameBytes = stringToBytes("converting name", name);
    byte[] valueBytes = stringToBytes("converting value", value);
    SysCall.sysCall.sysProcess(nameBytes, valueBytes);
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
    //fixme not implemented
    return;
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

  /****************************************************************************
   *
   * Check references, log information about references
   */

  /**
   * Checks if an address refers to an in-use area of memory.
   *
   * @param address the address to be checked
   * @return <code>true</code> if the address refers to an in use area
   */
  @Inline
  public static boolean addressInVM(Address address) {
    // note: check if this is correct
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
    return addressInVM(object.toAddress().loadAddress());
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
    // note: check this is correct
    return (SysCall.sysCall.sysStartingHeapAddress().LE(address) && address.LE(sysCall.sysCall.sysLastHeapAddress())) ||
            (BOOT_IMAGE_DATA_START.LE(address) && BOOT_IMAGE_END.LE(address));
  }

  /***********************************************************************
   *
   * Allocation
   */

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

  /***********************************************************************
   *
   * Allocation
   */

  /**
   * Return an allocation site upon request.  The request may be made
   * in the context of compilation.
   *
   * @param compileTime {@code true} if this request is being made in the
   * context of a compilation.
   * @return an allocation site
   */
  public static int getAllocationSite(boolean compileTime) {
    return RustContext.DEFAULT_SITE;
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
    if (traceAllocator) {
      VM.sysWrite("allocator for ");
      VM.sysWrite(type.getDescriptor());
      VM.sysWrite(": ");
    }
    if (method != null) {
      // We should strive to be allocation-free here.
      RVMClass cls = method.getDeclaringClass();
      byte[] clsBA = cls.getDescriptor().toByteArray();
      if (isPrefix("Lorg/jikesrvm/mm/", clsBA)) {
        if (traceAllocator) {
          VM.sysWriteln("NONMOVING");
        }
        return RustContext.ALLOC_NON_MOVING;
      }
      if (method.isNonMovingAllocation()) {
        return RustContext.ALLOC_NON_MOVING;
      }
    }
    if (traceAllocator) {
      VM.sysWriteln(type.getMMAllocator());
    }
    return type.getMMAllocator();
  }


  /**
   * Determine the default allocator to be used for a given type.
   *
   * @param type The type in question
   * @return The allocator to use for allocating instances of type
   * <code>type</code>.
   */
  @Interruptible
  private static int pickAllocatorForType(RVMType type) {
    int allocator = RustContext.ALLOC_DEFAULT;
    if (type.isArrayType()) {
      RVMType elementType = type.asArray().getElementType();
      if (elementType.isPrimitiveType() || elementType.isUnboxedType()) {
        allocator = RustContext.ALLOC_NON_REFERENCE;
      }
    }
    if (type.isNonMoving()) {
      allocator = RustContext.ALLOC_NON_MOVING;
    }
    byte[] typeBA = type.getDescriptor().toByteArray();
    if (isPrefix("Lorg/jikesrvm/tuningfork", typeBA) || isPrefix("[Lorg/jikesrvm/tuningfork", typeBA) ||
            isPrefix("Lcom/ibm/tuningfork/", typeBA) || isPrefix("[Lcom/ibm/tuningfork/", typeBA) ||
            isPrefix("Lorg/jikesrvm/mm/", typeBA) || isPrefix("[Lorg/jikesrvm/mm/", typeBA) ||
            isPrefix("Lorg/jikesrvm/jni/JNIEnvironment;", typeBA)) {
      allocator = RustContext.ALLOC_NON_MOVING;
    }
    return allocator;
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
    Address region = org.jikesrvm.runtime.Memory.alignUp(initialOffset.toWord().toAddress(), MIN_ALIGNMENT);
    return Selected.Mutator.alignAllocation(region, align, offset, MIN_ALIGNMENT, false).toWord().toOffset();
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
    if (bytes <= 0) {
      if (VM.VerifyAssertions) {
        VM.sysWrite("Invalid stack size: ");
        VM.sysWrite(bytes);
        VM.sysWriteln("!");
        VM._assert(VM.NOT_REACHED, "Attempted to create stack with size (in bytes) of 0 or smaller!");
      } else {
        bytes = StackFrameLayout.getStackSizeNormal();
      }
    }

    if (!VM.runningVM) {
      return new byte[bytes];
    } else {
      RVMArray stackType = RVMArray.ByteArray;
      int headerSize = ObjectModel.computeArrayHeaderSize(stackType);
      int align = ObjectModel.getAlignment(stackType);
      int offset = ObjectModel.getOffsetForAlignment(stackType, false);
      int width = stackType.getLogElementSize();
      TIB stackTib = stackType.getTypeInformationBlock();

      return (byte[]) allocateArray(bytes,
              width,
              headerSize,
              stackTib,
              RustContext.ALLOC_STACK,
              align,
              offset,
              RustContext.DEFAULT_SITE);
    }
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
    // fixme: code duplication for everything except the return
    if (!VM.runningVM) {
      return WordArray.create(size);
    }

    RVMArray arrayType = RVMType.WordArrayType;
    int headerSize = ObjectModel.computeArrayHeaderSize(arrayType);
    int align = ObjectModel.getAlignment(arrayType);
    int offset = ObjectModel.getOffsetForAlignment(arrayType, false);
    int width = arrayType.getLogElementSize();
    TIB arrayTib = arrayType.getTypeInformationBlock();

    return (WordArray) allocateArray(size,
            width,
            headerSize,
            arrayTib,
            RustContext.ALLOC_NON_MOVING,
            align,
            offset,
            RustContext.DEFAULT_SITE);

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
    //fixme: code duplication for everything except the return
    if (!VM.runningVM) {
      return new int[size];
    }

    RVMArray arrayType = RVMArray.IntArray;
    int headerSize = ObjectModel.computeArrayHeaderSize(arrayType);
    int align = ObjectModel.getAlignment(arrayType);
    int offset = ObjectModel.getOffsetForAlignment(arrayType, false);
    int width = arrayType.getLogElementSize();
    TIB arrayTib = arrayType.getTypeInformationBlock();

    return (int[]) allocateArray(size,
            width,
            headerSize,
            arrayTib,
            RustContext.ALLOC_NON_MOVING,
            align,
            offset,
            RustContext.DEFAULT_SITE);
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
    //fixme: code duplication for everything except the return
    if (!VM.runningVM) {
      return new short[size];
    }

    RVMArray arrayType = RVMArray.ShortArray;
    int headerSize = ObjectModel.computeArrayHeaderSize(arrayType);
    int align = ObjectModel.getAlignment(arrayType);
    int offset = ObjectModel.getOffsetForAlignment(arrayType, false);
    int width = arrayType.getLogElementSize();
    TIB arrayTib = arrayType.getTypeInformationBlock();

    return (short[]) allocateArray(size,
            width,
            headerSize,
            arrayTib,
            RustContext.ALLOC_NON_MOVING,
            align,
            offset,
            RustContext.DEFAULT_SITE);
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
    int elements = TIB.computeSize(numVirtualMethods);

    if (!VM.runningVM) {
      return TIB.allocate(elements, alignCode);
    }
    if (alignCode == AlignmentEncoding.ALIGN_CODE_NONE) {
      return (TIB)newRuntimeTable(elements, RVMType.TIBType);
    }

    RVMType type = RVMType.TIBType;
    if (VM.VerifyAssertions) VM._assert(VM.runningVM);

    TIB realTib = type.getTypeInformationBlock();
    RVMArray fakeType = RVMType.WordArrayType;
    TIB fakeTib = fakeType.getTypeInformationBlock();
    int headerSize = ObjectModel.computeArrayHeaderSize(fakeType);
    int align = ObjectModel.getAlignment(fakeType);
    int offset = ObjectModel.getOffsetForAlignment(fakeType, false);
    int width = fakeType.getLogElementSize();
    int elemBytes = elements << width;
    if (elemBytes < 0 || (elemBytes >>> width) != elements) {
      /* asked to allocate more than Integer.MAX_VALUE bytes */
      throwLargeArrayOutOfMemoryError();
    }
    int size = elemBytes + headerSize + AlignmentEncoding.padding(alignCode);
    Selected.Mutator mutator = Selected.Mutator.get();
    Address region = allocateSpace(mutator, size, align, offset, type.getMMAllocator(), RustContext.DEFAULT_SITE);

    region = AlignmentEncoding.adjustRegion(alignCode, region);

    Object result = ObjectModel.initializeArray(region, fakeTib, elements, size);
    // note: mutator must implement postAlloc
    mutator.postAlloc(ObjectReference.fromObject(result), ObjectReference.fromObject(fakeTib), size, type.getMMAllocator());

    /* Now we replace the TIB */
    ObjectModel.setTIB(result, realTib);

    if (traceAllocation) {
      VM.sysWrite("tib: ", ObjectReference.fromObject(result));
      VM.sysWrite(" ", region);
      VM.sysWriteln("-", region.plus(size));
    }
    return (TIB)result;
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
   * Header initialization
   */

  /**
   * Override the boot-time initialization method here, so that
   * the core MMTk code doesn't need to know about the
   * BootImageInterface type.
   *
   * @param bootImage the bootimage instance
   * @param ref the object's address
   * @param tib the object's TIB
   * @param size the number of bytes allocated by the GC system for
   *  the object
   * @param isScalar whether the header belongs to a scalar or an array
   */
  @Interruptible
  public static void initializeHeader(BootImageInterface bootImage, Address ref, TIB tib, int size,
                                      boolean isScalar) {
    byte status = 0;
    JavaHeader.writeAvailableByte(bootImage, ref, status);
  }

  /**
   * Installs a reference into the boot image.
   *
   * @param value the reference to install
   * @return the installed reference
   */
  @Interruptible
  public static Word bootTimeWriteBarrier(Word value) {
    return value;
  }

  /***********************************************************************
   *
   * Miscellaneous
   */

  @Interruptible
  public static void notifyClassResolved(RVMType vmType) {
    vmType.setMMAllocator(pickAllocatorForType(vmType));
  }

  /**
   * @return the number of specialized methods.
   */
  public static int numSpecializedMethods() {
    // note: check whether different RMMTk plans may have multiple specializedMethods
    // we may have to put this in selected
    return 0;
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
   * Entrypoints used by Rust
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

  @Interruptible
  @Entrypoint
  public static void spawnCollectorThread(Address workerInstance) {
    byte[] stack = MemoryManager.newStack(StackFrameLayout.getStackSizeCollector());
    CollectorThread t = new CollectorThread(stack);
    t.setWorker(workerInstance);
    t.start();
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

}

