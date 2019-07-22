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

import org.mmtk.plan.PlanConstraints;
import org.mmtk.plan.semispace.SSMutator;
import org.mmtk.policy.CopySpace;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.Plan;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class ConcSSContext extends SSMutator {
    @Uninterruptible
    public static class Constraints extends PlanConstraints {
        @Override
        public boolean movesObjects() {
            return true;
        }
        @Override
        public int gcHeaderBits() {
            return CopySpace.LOCAL_GC_BITS_REQUIRED;
        }
        @Override
        public int gcHeaderWords() {
            return 1;
        }
        @Override
        public int numSpecializedScans() {
            return 1;
        }
        @Override
        public boolean needsObjectReferenceReadBarrier() {
            return true;
        }
        @Override
        public boolean needsObjectReferenceWriteBarrier() {
            return true;
        }
        @Override
        public boolean needsJavaLangReferenceReadBarrier() {
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
    @Entrypoint Address modbuf;
    @Entrypoint Address trace;
    @Entrypoint Address barrierActive;

    static final Offset threadIdOffset = getField(ConcSSContext.class, "threadId", Address.class).getOffset();

    @Inline
    public boolean barrierActive() {
        return !barrierActive.isZero();
    }

    public Address setBlock(Address mmtkHandle) {
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
        modbuf           = mmtkHandle.plus(BYTES_IN_WORD * 10).loadAddress();
        trace            = mmtkHandle.plus(BYTES_IN_WORD * 11).loadAddress();
        barrierActive    = mmtkHandle.plus(BYTES_IN_WORD * 12).loadAddress();
        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Inline
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

    @Inline
    @Override
    public void postAlloc(ObjectReference ref, ObjectReference typeRef, int bytes, int allocator) {
        if (allocator == Plan.ALLOC_DEFAULT) return;
        sysCall.sysPostAlloc(handle(), ref, typeRef, bytes, allocator);
    }

    @Inline
    @Override
    public void objectReferenceWrite(ObjectReference src, Address slot, ObjectReference value, Word metaDataA, Word metaDataB, int mode) {
        if (!this.barrierActive()) org.mmtk.vm.VM.barriers.objectReferenceWrite(src, value, metaDataA, metaDataB, mode);
        else sysCall.sysObjectReferenceWriteSlow(handle(), src, slot, value);
        // org.mmtk.vm.VM.barriers.objectReferenceWrite(src, value, metaDataA, metaDataB, mode);
    }

    @Inline
    @Override
    public boolean objectReferenceTryCompareAndSwap(ObjectReference src, Address slot, ObjectReference old, ObjectReference tgt, Word metaDataA, Word metaDataB, int mode) {
        if (!this.barrierActive()) return org.mmtk.vm.VM.barriers.objectReferenceTryCompareAndSwap(src, old, tgt, metaDataA, metaDataB, mode);
        return sysCall.sysObjectReferenceTryCompareAndSwapSlow(handle(), src, slot, old, tgt);
    }

    @Inline
    @Override
    public ObjectReference javaLangReferenceReadBarrier(ObjectReference ref) {
        if (!this.barrierActive()) return ref;
        return sysCall.sysJavaLangReferenceReadSlow(handle(), ref);
    }

    @Inline
    public Address handle() {
        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @NoInline
    public ObjectReference objectReferenceReadSlow(ObjectReference src, Address slot) {
        return sysCall.object_reference_read_slow(Magic.objectAsAddress(this).plus(threadIdOffset), src, slot);
    }

    @Inline
    @Override
    public ObjectReference objectReferenceRead(ObjectReference src, Address slot, Word metaDataA, Word metaDataB, int mode) {
        if (barrierActive.isZero()) return slot.loadObjectReference();
        return objectReferenceReadSlow(src, slot);
    }
}
