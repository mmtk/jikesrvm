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

import org.mmtk.plan.g1.G1Mutator;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.Plan;
import org.mmtk.plan.PlanConstraints;
import org.jikesrvm.VM;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class G1Context extends G1Mutator {
    @Uninterruptible
    public static class Constraints extends PlanConstraints {
        @Override
        public boolean movesObjects() {
            return true;
        }
        @Override
        public int gcHeaderBits() {
            return 4;
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
        public int maxNonLOSDefaultAllocBytes() {
          return 512 * 1024;
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
    @Entrypoint Address refills;
    @Entrypoint Address tlabSize;
    @Entrypoint Address gen;

    @Entrypoint Address threadIdLos;
    @Entrypoint Address spaceLos;

    @Entrypoint Address threadIdImmortal;
    @Entrypoint Address cursorImmortal;
    @Entrypoint Address limitImmortal;
    @Entrypoint Address spaceImmortal;

    @Entrypoint Address modbuf;
    @Entrypoint Address dirtyCardQueue;
    @Entrypoint Address barrierActive;

    static final Offset threadIdOffset = getField(G1Context.class, "threadId", Address.class).getOffset();

    public Address setBlock(Address mmtkHandle) {
        threadId         = mmtkHandle.plus(BYTES_IN_WORD * 0).loadAddress();
        cursor           = mmtkHandle.plus(BYTES_IN_WORD * 1).loadAddress();
        limit            = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space            = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
        refills          = mmtkHandle.plus(BYTES_IN_WORD * 4).loadAddress();
        tlabSize         = mmtkHandle.plus(BYTES_IN_WORD * 5).loadAddress();
        gen              = mmtkHandle.plus(BYTES_IN_WORD * 6).loadAddress();

        threadIdLos      = mmtkHandle.plus(BYTES_IN_WORD * 7).loadAddress();
        spaceLos         = mmtkHandle.plus(BYTES_IN_WORD * 8).loadAddress();

        threadIdImmortal = mmtkHandle.plus(BYTES_IN_WORD * 9).loadAddress();
        cursorImmortal   = mmtkHandle.plus(BYTES_IN_WORD * 10).loadAddress();
        limitImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 11).loadAddress();
        spaceImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 12).loadAddress();

        modbuf           = mmtkHandle.plus(BYTES_IN_WORD * 13).loadAddress();
        dirtyCardQueue   = mmtkHandle.plus(BYTES_IN_WORD * 14).loadAddress();
        barrierActive    = mmtkHandle.plus(BYTES_IN_WORD * 15).loadAddress();

        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Override
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        // Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
        // return sysCall.sysAlloc(handle, bytes, align, offset, allocator);
        if (VM.VerifyAssertions) {
            VM._assert(bytes < 3 * 1024 * 1024);
            // VM._assert(limitOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 2));
            // VM._assert(spaceOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 3));
            // VM._assert(threadIdLosOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 4));
            // VM._assert(spaceLosOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 5));
        }
        if (allocator == Plan.ALLOC_DEFAULT) {
            Address region, cursor = this.cursor, limit = this.limit;
            // Align allocation
            Word mask = Word.fromIntSignExtend(align - 1);
            Word negOff = Word.fromIntSignExtend(-offset);
            Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();
            Address result = cursor.plus(delta);
            Address newCursor = result.plus(bytes);

            if (newCursor.GT(limit)) {
                Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
                region = sysCall.sysAllocSlow(handle, bytes, align, offset, allocator);
            } else {
                org.mmtk.utility.alloc.Allocator.fillAlignmentGap(this.cursor, result);
                this.cursor = newCursor;
                region = result;
            }
            return region;
        } else if (allocator == Plan.ALLOC_LOS) {
            Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
            return sysCall.sysAllocSlow(handle, bytes, align, offset, allocator);
        } else {
            Address region, cursor = this.cursorImmortal, limit = this.limitImmortal;
            // Align allocation
            Word mask = Word.fromIntSignExtend(align - 1);
            Word negOff = Word.fromIntSignExtend(-offset);
            Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();
            Address result = cursor.plus(delta);
            Address newCursor = result.plus(bytes);

            if (newCursor.GT(limit)) {
                // VM.sysWriteln("[allocslow]");
                Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
                region = sysCall.sysAllocSlow(handle, bytes, align, offset, allocator);
            } else {
                // VM.sysWriteln("fillAlignmentGap ", this.cursorImmortal, " ", result);
                org.mmtk.utility.alloc.Allocator.fillAlignmentGap(this.cursorImmortal, result);
                this.cursorImmortal = newCursor;
                region = result;
            }
            // VM.sysWriteln("VS ", region);
            return region;
        }
    }

    @Override
    public void postAlloc(ObjectReference ref, ObjectReference typeRef, int bytes, int allocator) {
        // if (allocator != 0) {
            sysCall.sysPostAlloc(handle(), ref, typeRef, bytes, allocator);
        // }
    }

    @Inline
    public boolean barrierActive() {
        return !barrierActive.isZero();
    }

    @Inline
    public Address handle() {
        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Inline
    @Override
    public void objectReferenceWrite(ObjectReference src, Address slot, ObjectReference value, Word metaDataA, Word metaDataB, int mode) {
        // if (!this.barrierActive()) org.mmtk.vm.VM.barriers.objectReferenceWrite(src, value, metaDataA, metaDataB, mode);
        // else sysCall.sysObjectReferenceWriteSlow(handle(), src, slot, value);
        sysCall.sysObjectReferenceWriteSlow(handle(), src, slot, value);
    }

    @Inline
    @Override
    public boolean objectReferenceTryCompareAndSwap(ObjectReference src, Address slot, ObjectReference old, ObjectReference tgt, Word metaDataA, Word metaDataB, int mode) {
        // if (!this.barrierActive()) return org.mmtk.vm.VM.barriers.objectReferenceTryCompareAndSwap(src, old, tgt, metaDataA, metaDataB, mode);
        return sysCall.sysObjectReferenceTryCompareAndSwapSlow(handle(), src, slot, old, tgt);
    }

    @Inline
    @Override
    public ObjectReference javaLangReferenceReadBarrier(ObjectReference ref) {
        if (!this.barrierActive()) return ref;
        return sysCall.sysJavaLangReferenceReadSlow(handle(), ref);
    }
}
