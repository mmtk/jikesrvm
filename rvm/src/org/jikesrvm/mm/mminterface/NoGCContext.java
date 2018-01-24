package org.jikesrvm.mm.mminterface;

import org.vmmagic.Unboxed;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Memory;
import org.jikesrvm.runtime.Magic;
import org.mmtk.plan.Plan;
import static org.jikesrvm.runtime.SysCall.sysCall;

@Unboxed
@Uninterruptible
@RawStorage(lengthInWords = true, length = 4)
public class NoGCContext {
    public void setBlock(Address src) {
        Memory.memcopy(Magic.objectAsAddress(this), src, 16);
    }

    public void setCursor(Address value) {
        Magic.setAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(4), value);
    }

    public Address getCursor() {
        return Magic.getAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(4));
    }

    public Address getSentinel() {
        return Magic.getAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(8));
    }

    @Inline
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        Address region;
        Address cursor = this.getCursor();
        Address sentinel = this.getSentinel();

        // Align allocation
        Word mask = Word.fromIntSignExtend(align - 1);
        Word negOff = Word.fromIntSignExtend(-offset);

        Offset delta = negOff.minus(cursor.toWord()).and(mask).toOffset();

        Address result = cursor.plus(delta);

        Address newCursor = result.plus(bytes);

        if (newCursor.GT(sentinel)) {
            Address handle = Magic.objectAsAddress(this);
            region = sysCall.sysAllocSlow(handle, bytes, align, offset, allocator);
        } else {
            this.setCursor(newCursor);
            region = result;
        }
        return region;
    }
}
