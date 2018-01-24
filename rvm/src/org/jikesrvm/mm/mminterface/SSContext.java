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
@RawStorage(lengthInWords = true, length = 8)
public class SSContext {
    public void setBlock(Address src) {
        Memory.memcopy(Magic.objectAsAddress(this), src, 32);
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

    public void setImmortalCursor(Address value) {
        Magic.setAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(20), value);
    }

    public Address getImmortalCursor() {
        return Magic.getAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(20));
    }

    public Address getImmortalSentinel() {
        return Magic.getAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(24));
    }

    @Inline
    public Address alloc(int bytes, int align, int offset, int allocator, int site) {
        Address region;
        Address cursor;
        Address sentinel;
        if (allocator == Plan.ALLOC_DEFAULT) {
            cursor = this.getCursor();
            sentinel = this.getSentinel();
        } else {
            cursor = this.getImmortalCursor();
            sentinel = this.getImmortalSentinel();
        }

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
            if (allocator == Plan.ALLOC_DEFAULT) {
                this.setCursor(newCursor);
            } else {
                this.setImmortalCursor(newCursor);
            }
            region = result;
        }
        return region;
    }
}
