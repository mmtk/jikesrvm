package rust.org.jikesrvm.mm.mminterface;

import org.jikesrvm.VM;
import org.jikesrvm.mm.mminterface.AbstractDebugUtil;
import org.jikesrvm.objectmodel.ObjectModel;
import org.jikesrvm.objectmodel.TIB;
import org.jikesrvm.runtime.Magic;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.ObjectReference;

import static org.jikesrvm.mm.mminterface.MemoryManagerConstants.MOVES_OBJECTS;
import static org.jikesrvm.runtime.SysCall.sysCall;

public class RustDebugUtil extends AbstractDebugUtil {
    @Uninterruptible
    public static boolean validRef(ObjectReference ref) {

        if (ref.isNull()) return true;
        if (MOVES_OBJECTS) {
      /*
      TODO: Work out how to check if forwarded
      if (Plan.isForwardedOrBeingForwarded(ref)) {
        // TODO: actually follow forwarding pointer
        // (need to bound recursion when things are broken!!)
        return true;
      }
      */
        }
        TIB tib = ObjectModel.getTIB(ref);
        Address tibAddr = Magic.objectAsAddress(tib);
        if (tibAddr.isZero()) {
            VM.sysWrite("validRef: TIB is Zero! ");
            VM.sysWrite(ref);
            VM.sysWriteln();
            ObjectModel.dumpHeader(ref);
            return false;
        }
        if (tib.length() == 0) {
            VM.sysWrite("validRef: TIB length zero, ref = ");
            VM.sysWrite(ref);
            VM.sysWrite(" tib = ");
            VM.sysWrite(tibAddr);
            VM.sysWriteln();
            ObjectModel.dumpHeader(ref);
            return false;
        }
        ObjectReference type = ObjectReference.fromObject(tib.getType());
        if (!validType(type)) {
            VM.sysWrite("validRef: invalid TYPE, ref = ");
            VM.sysWrite(ref);
            VM.sysWrite(" tib = ");
            VM.sysWrite(Magic.objectAsAddress(tib));
            VM.sysWrite(" type = ");
            VM.sysWrite(type);
            VM.sysWriteln();
            ObjectModel.dumpHeader(ref);
            return false;
        }
        return true;
    }  // validRef

    @Uninterruptible
    public static boolean mappedVMRef(ObjectReference ref) {
            return sysCall.sysIsMappedObject(ref);
    }
}
