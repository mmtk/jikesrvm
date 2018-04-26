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
import org.jikesrvm.runtime.Magic;

import static org.jikesrvm.runtime.EntrypointHelper.getField;
import static org.jikesrvm.runtime.SysCall.sysCall;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_WORD;

@Uninterruptible
public class NoGCContext extends BaseContext {

    @Entrypoint
    Address threadId;
    @Entrypoint
    Address cursor;
    @Entrypoint
    Address limit;
    @Entrypoint
    Address space;

    static final Offset threadIdOffset = getField(NoGCContext.class, "threadId", Address.class).getOffset();
    static final Offset cursorOffset = getField(NoGCContext.class, "cursor", Address.class).getOffset();
    static final Offset limitOffset = getField(NoGCContext.class, "limit", Address.class).getOffset();
    static final Offset spaceOffset = getField(NoGCContext.class, "space", Address.class).getOffset();

    @Override
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        Address region;

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
            cursor = newCursor;
            region = result;
        }
        return region;
    }

    public void setBlock(Address mmtkHandle) {
        if (VM.VerifyAssertions) {
            VM._assert(cursorOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(4));
            VM._assert(limitOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(8));
            VM._assert(spaceOffset.minus(threadIdOffset) == Offset.fromIntSignExtend(12));
        }
        threadId = mmtkHandle.loadAddress();
        cursor   = mmtkHandle.plus(BYTES_IN_WORD).loadAddress();
        limit    = mmtkHandle.plus(BYTES_IN_WORD * 2).loadAddress();
        space    = mmtkHandle.plus(BYTES_IN_WORD * 3).loadAddress();
    }


}
