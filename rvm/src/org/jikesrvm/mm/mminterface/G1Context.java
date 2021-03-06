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

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class G1Context extends G1Mutator {
    @Entrypoint
    Address threadId;
    @Entrypoint
    Address cursor;
    @Entrypoint
    Address limit;
    @Entrypoint
    Address space;
    @Entrypoint
    Address threadIdImmortal;
    @Entrypoint
    Address cursorImmortal;
    @Entrypoint
    Address limitImmortal;
    @Entrypoint
    Address spaceImmortal;

    static final Offset threadIdOffset = getField(SSContext.class, "threadId", Address.class).getOffset();
    static final Offset cursorOffset = getField(SSContext.class, "cursor", Address.class).getOffset();
    static final Offset limitOffset = getField(SSContext.class, "limit", Address.class).getOffset();
    static final Offset spaceOffset = getField(SSContext.class, "space", Address.class).getOffset();
    static final Offset threadIdImmortalOffset = getField(SSContext.class, "threadIdImmortal", Address.class).getOffset();
    static final Offset cursorImmortalOffset = getField(SSContext.class, "cursorImmortal", Address.class).getOffset();
    static final Offset limitImmortalOffset = getField(SSContext.class, "limitImmortal", Address.class).getOffset();
    static final Offset spaceImmortalOffset = getField(SSContext.class, "spaceImmortal", Address.class).getOffset();

    public Address setBlock(Address mmtkHandle) {
        if (VM.VerifyAssertions) {
            VM._assert(cursorOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD));
            VM._assert(limitOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 2));
            VM._assert(spaceOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 3));
            VM._assert(threadIdImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 4));
            VM._assert(cursorImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 5));
            VM._assert(limitImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 6));
            VM._assert(spaceImmortalOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(BYTES_IN_WORD * 7));
        }
        threadId = mmtkHandle.loadAddress();
        cursor   = mmtkHandle.plus(BYTES_IN_WORD).loadAddress();
        limit    = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space    = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
        threadIdImmortal = mmtkHandle.plus(BYTES_IN_WORD * 4).loadAddress();
        cursorImmortal   = mmtkHandle.plus(BYTES_IN_WORD * 5).loadAddress();
        limitImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 6).loadAddress();
        spaceImmortal    = mmtkHandle.plus(BYTES_IN_WORD * 7).loadAddress();
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
