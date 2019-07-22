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
import org.mmtk.plan.g1.G1Mutator;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.Plan;
import org.mmtk.plan.PlanConstraints;
import org.mmtk.policy.CopySpace;

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
            return CopySpace.LOCAL_GC_BITS_REQUIRED;
        }
        @Override
        public int gcHeaderWords() {
            return CopySpace.GC_HEADER_WORDS_REQUIRED;
        }
        @Override
        public int numSpecializedScans() {
            return 1;
        }
        @Override
        public int maxNonLOSDefaultAllocBytes() {
          return (1 << (6 + 12)) * 3 / 4;
        }
    }

    @Entrypoint Address threadId;
    @Entrypoint Address cursor;
    @Entrypoint Address limit;
    @Entrypoint Address space;
    @Entrypoint Address threadIdLos;
    @Entrypoint Address spaceLos;
    @Entrypoint Address threadIdImmortal;
    @Entrypoint Address cursorImmortal;
    @Entrypoint Address limitImmortal;
    @Entrypoint Address spaceImmortal;

    static final Offset threadIdOffset = getField(SSContext.class, "threadId", Address.class).getOffset();
    
    public Address setBlock(Address mmtkHandle) {
        threadId         = mmtkHandle.plus(BYTES_IN_WORD * 0).loadAddress();
        cursor           = mmtkHandle.plus(BYTES_IN_WORD * 1).loadAddress();
        limit            = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space            = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
        threadIdLos      = mmtkHandle.plus(BYTES_IN_WORD * 4).loadAddress();
        spaceLos         = mmtkHandle.plus(BYTES_IN_WORD * 5).loadAddress();
        threadIdImmortal = mmtkHandle.plus(BYTES_IN_WORD * 6).loadAddress();
        cursorImmortal   = mmtkHandle.plus(BYTES_IN_WORD * 7).loadAddress();
        limitImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 8).loadAddress();
        spaceImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 9).loadAddress();
        return Magic.objectAsAddress(this).plus(threadIdOffset);
    }

    @Override
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        // Address handle = Magic.objectAsAddress(this).plus(threadIdOffset);
        // return sysCall.sysAlloc(handle, bytes, align, offset, allocator);
        Address region;
        Address cursor;
        Address limit;
        if (allocator == Plan.ALLOC_DEFAULT) {
            cursor = this.cursor;
            limit = this.limit;
        } else {
            cursor = this.cursorImmortal;
            limit = this.limitImmortal;
        }

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
}
