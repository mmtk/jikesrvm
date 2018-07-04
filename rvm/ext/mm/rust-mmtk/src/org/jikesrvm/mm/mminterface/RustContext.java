package org.jikesrvm.mm.mminterface;

import org.jikesrvm.scheduler.RVMThread;
import org.jikesrvm.VM;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.Word;

import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MAX_ALIGNMENT;
import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MIN_ALIGNMENT;
import static org.jikesrvm.objectmodel.JavaHeaderConstants.ALIGNMENT_VALUE;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_INT;
import static org.jikesrvm.runtime.SysCall.sysCall;

@Uninterruptible
public abstract class RustContext {

  /****************************************************************************
   * Constants
   */

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
  public static final int ALLOCATORS = 9;
  public static final int DEFAULT_SITE = -1;
  public static final int ALLOC_STACK = ALLOC_LOS;

  // todo
  // -> alloc
  // -> postalloc

  @Entrypoint
  private Address mmtkHandle;

  @Inline
  public static Selected.Mutator get() {
    return RVMThread.getCurrentThread();
  }

  @Entrypoint
  public void initMutator(int id){
    if (id != 1) {
      mmtkHandle = sysCall.sysBindMutator(id);
      this.setBlock(mmtkHandle);
    }
  }
  public void setHandle(Address handle){
    mmtkHandle = handle;
    this.setBlock(mmtkHandle);
  }

  /**
   * Aligns up an allocation request. The allocation request accepts a
   * region, that must be at least particle aligned, an alignment
   * request (some power of two number of particles) and an offset (a
   * number of particles). There is also a knownAlignment parameter to
   * allow a more optimised check when the particular allocator in use
   * always aligns at a coarser grain than individual particles, such
   * as some free lists.
   *
   * @param region The region to align up.
   * @param alignment The requested alignment
   * @param offset The offset from the alignment
   * @param knownAlignment The statically known minimum alignment.
   * @param fillAlignmentGap whether to fill up holes in the alignment
   *  with the alignment value ({@link Constants#ALIGNMENT_VALUE})
   * @return The aligned up address.
   */
  @Inline
  public static Address alignAllocation(Address region, int alignment, int offset, int knownAlignment, boolean fillAlignmentGap) {
    if (VM.VerifyAssertions) {
      VM._assert(knownAlignment >= MIN_ALIGNMENT);
      VM._assert(MIN_ALIGNMENT >= BYTES_IN_INT);
      VM._assert(!(fillAlignmentGap && region.isZero()));
      VM._assert(alignment <= MAX_ALIGNMENT);
      VM._assert(offset >= 0);
      VM._assert(region.toWord().and(Word.fromIntSignExtend(MIN_ALIGNMENT - 1)).isZero());
      VM._assert((alignment & (MIN_ALIGNMENT - 1)) == 0);
      VM._assert((offset & (MIN_ALIGNMENT - 1)) == 0);
    }

    // No alignment ever required.
    if (alignment <= knownAlignment || MAX_ALIGNMENT <= MIN_ALIGNMENT)
      return region;

    // May require an alignment
    Word mask = Word.fromIntSignExtend(alignment - 1);
    Word negOff = Word.fromIntSignExtend(-offset);
    Offset delta = negOff.minus(region.toWord()).and(mask).toOffset();

    if (fillAlignmentGap && ALIGNMENT_VALUE != 0) {
      fillAlignmentGap(region, region.plus(delta));
    }

    return region.plus(delta);
  }

  /**
   * Fill the specified region with the alignment value.
   *
   * @param start The start of the region.
   * @param end A pointer past the end of the region.
   */
  @Inline
  public static void fillAlignmentGap(Address start, Address end) {
    if ((MAX_ALIGNMENT - MIN_ALIGNMENT) == BYTES_IN_INT) {
      // At most a single hole
      if (!end.diff(start).isZero()) {
        start.store(ALIGNMENT_VALUE);
      }
    } else {
      while (start.LT(end)) {
        start.store(ALIGNMENT_VALUE);
        start = start.plus(BYTES_IN_INT);
      }
    }
  }

  public Address getHandle() {
    return mmtkHandle;
  }

  public void deinitMutator() {
    return;
  }

  public abstract int getId();

  public abstract void setBlock(Address mmtkHandle);

  public abstract Address alloc(int bytes, int align, int offset, int allocator, int site);

  public void postAlloc(ObjectReference object, ObjectReference typeRef, int bytes, int allocator) {
    return;
  }
}
