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
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.MutatorContext;
import org.mmtk.plan.Plan;
import org.mmtk.plan.PlanConstraints;
import org.vmmagic.pragma.Entrypoint;
import org.vmmagic.pragma.Inline;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;
import org.vmmagic.unboxed.Offset;
import org.vmmagic.unboxed.Word;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class GenCopyContext extends MutatorContext {
    @Uninterruptible
    public static class Constraints extends PlanConstraints {

//        @Override
//        public boolean generational() {
//            return true;
//        }
//
//        @Override
//        public boolean needsObjectReferenceNonHeapWriteBarrier() {
//            return false;
//        }
//
//        @Override
//        public boolean objectReferenceBulkCopySupported() {
//            return false;
//        }
//
//        @Override
//        public boolean needsLogBitInHeader() {
//            return false;
//        }

        @Override
        public boolean movesObjects() {
            return true;
        }
        @Override
        public int gcHeaderBits() {
            return 2;
        }
        @Override
        public int gcHeaderWords() {
            return 0;
        }
        @Override
        public int numSpecializedScans() {
            return 1;
        }
        @Override
        public boolean needsObjectReferenceWriteBarrier() {
            return true;
        }
    }


    @Entrypoint Address threadId;
    @Entrypoint Address cursor;
    @Entrypoint Address limit;
    @Entrypoint Address space;
    @Entrypoint Address threadIdImmortal;
    @Entrypoint Address cursorImmortal;
    @Entrypoint Address limitImmortal;
    @Entrypoint Address spaceImmortal;
    @Entrypoint Address threadIdLos;
    @Entrypoint Address spaceLos;
    @Entrypoint Address remset;

    static final Offset threadIdOffset = getField(GenCopyContext.class, "threadId", Address.class).getOffset();
    static final Offset cursorOffset = getField(GenCopyContext.class, "cursor", Address.class).getOffset();
    static final Offset limitOffset = getField(GenCopyContext.class, "limit", Address.class).getOffset();
    static final Offset spaceOffset = getField(GenCopyContext.class, "space", Address.class).getOffset();
    static final Offset threadIdImmortalOffset = getField(GenCopyContext.class, "threadIdImmortal", Address.class).getOffset();
    static final Offset cursorImmortalOffset = getField(GenCopyContext.class, "cursorImmortal", Address.class).getOffset();
    static final Offset limitImmortalOffset = getField(GenCopyContext.class, "limitImmortal", Address.class).getOffset();
    static final Offset spaceImmortalOffset = getField(GenCopyContext.class, "spaceImmortal", Address.class).getOffset();
    static final Offset threadIdLosOffset = getField(GenCopyContext.class, "threadIdLos", Address.class).getOffset();
    static final Offset spaceLosOffset = getField(GenCopyContext.class, "spaceLos", Address.class).getOffset();
    static final Offset remsetOffset = getField(GenCopyContext.class, "remset", Address.class).getOffset();

    public Address setBlock(Address mmtkHandle) {
        if (VM.VerifyAssertions) {
            VM._assert(cursorOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD));
            VM._assert(limitOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 2));
            VM._assert(spaceOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 3));
            VM._assert(threadIdImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 4));
            VM._assert(cursorImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 5));
            VM._assert(limitImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 6));
            VM._assert(spaceImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 7));
            VM._assert(threadIdLosOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 8));
            VM._assert(spaceLosOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 9));
        }
        threadId         = mmtkHandle.plus(BYTES_IN_WORD * 0).loadAddress();
        cursor           = mmtkHandle.plus(BYTES_IN_WORD * 1).loadAddress();
        limit            = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space            = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
        threadIdImmortal = mmtkHandle.plus(BYTES_IN_WORD * 4).loadAddress();
        cursorImmortal   = mmtkHandle.plus(BYTES_IN_WORD * 5).loadAddress();
        limitImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 6).loadAddress();
        spaceImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 7).loadAddress();
        threadIdLos      = mmtkHandle.plus(BYTES_IN_WORD * 8).loadAddress();
        spaceLos         = mmtkHandle.plus(BYTES_IN_WORD * 9).loadAddress();
        remset           = mmtkHandle.plus(BYTES_IN_WORD * 10).loadAddress();
        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Override
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        if (allocator == Plan.ALLOC_LOS) {
            Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
            return sysCall.sysAlloc(handle, bytes, align, offset, allocator);
        }

        Address region;
        Address cursor;
        Address sentinel;
        if (allocator == Plan.ALLOC_DEFAULT) {
            cursor = this.cursor;
            sentinel = this.limit;
        } else {
            cursor = this.cursorImmortal;
            sentinel = this.limitImmortal;
        }

        // Align allocation
        Word mask = Word.fromIntSignExtend(align - 1);
        Word negOff = Word.fromIntSignExtend(-offset);

        Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();

        Address result = cursor.plus(delta);

        Address newCursor = result.plus(bytes);

        if (newCursor.GT(sentinel)) {
            Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
            region = sysCall.sysAllocSlow(handle, bytes, align, offset, allocator);
        } else {
            if (allocator == Plan.ALLOC_DEFAULT) {
                this.cursor = newCursor;
            } else {
                this.cursorImmortal = newCursor;
            }
            region = result;
        }
        return region;
    }

    @Override
    public void postAlloc(ObjectReference ref, ObjectReference typeRef,
                          int bytes, int allocator) {
        Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
        sysCall.sysPostAlloc(handle, ref, typeRef, bytes, allocator);
    }

    @Override
    public void collectionPhase(short phaseId, boolean primary) {
        VM.sysFail("unreachable");
    }

    static final Address NURSERY_START = Address.fromIntZeroExtend(0xa5400000);

    @Inline
    static boolean inNursery(Address addr) {
        return addr.GE(NURSERY_START);
    }

    @Inline
    static boolean inNursery(ObjectReference obj) {
        return inNursery(obj.toAddress());
    }


    @Inline
    void fastPath(ObjectReference src, Address slot, ObjectReference value) {
        if (!inNursery(slot) && inNursery(value)) {
            Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
            sysCall.sysObjectReferenceWriteSlow(handle, src, slot, value, 0);
        }
    }

    @Override
    @Inline
    public void objectReferenceWrite(ObjectReference src, Address slot, ObjectReference value, Word metaDataA, Word metaDataB, int mode) {
        fastPath(src, slot, value);
        org.mmtk.vm.VM.barriers.objectReferenceWrite(src, value, metaDataA, metaDataB, mode);
    }

    @Override
    @Inline
    public boolean objectReferenceTryCompareAndSwap(ObjectReference src, Address slot, ObjectReference old, ObjectReference tgt, Word metaDataA, Word metaDataB, int mode) {
        boolean result = org.mmtk.vm.VM.barriers.objectReferenceTryCompareAndSwap(src, old, tgt, metaDataA, metaDataB, mode);
//        if (result) {
            fastPath(src, slot, tgt);
//        }
        return result;
    }
}
