package org.jikesrvm.mm.mminterface;

import org.vmmagic.Unboxed;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.*;
import org.jikesrvm.runtime.Memory;
import org.jikesrvm.runtime.Magic;

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

    public void setImmortalCursor(Address value) {
        Magic.setAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(4), value);
    }

    public Address getImmortalCursor() {
        return Magic.getAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(4));
    }

    public Address getImmortalSentinel() {
        return Magic.getAddressAtOffset(Magic.objectAsAddress(this), Offset.fromIntSignExtend(8));
    }

}
