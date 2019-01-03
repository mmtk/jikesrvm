package org.jikesrvm.mm.mminterface;

import org.jikesrvm.runtime.Magic;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Address;

import static org.jikesrvm.runtime.SysCall.sysCall;
@NonMoving
public class RustCollectorThread extends AbstractCollectorThread {

    /***********************************************************************
     *
     * Class variables
     */

    /***********************************************************************
     *
     * Instance variables
     */
    /**
     * @param stack   The stack this thread will run on
     */
    public RustCollectorThread(byte[] stack) {
        super(stack);
    }

    /**
     * Collection entry point. Delegates the real work to MMTk.
     */
    @Override
    @NoOptCompile
    // refs stored in registers by opt compiler will not be relocated by GC
    @BaselineNoRegisters
    // refs stored in registers by baseline compiler will not be relocated by GC, so use stack only
    @BaselineSaveLSRegisters
    // and store all registers from previous method in prologue, so that we can stack access them while scanning this thread.
    @Unpreemptible
    public void run() {
            if (workerInstance.EQ(Address.zero())) {
                sysCall.sysStartControlCollector(Magic.objectAsAddress(rvmThread));
            } else {
                sysCall.sysStartWorker(Magic.objectAsAddress(rvmThread), workerInstance);
            }
    }


    @Entrypoint
    private Address workerInstance = Address.zero();

    public void setWorker(Address worker) {
        rvmThread.assertIsCollector();
        workerInstance = worker;
    }
}


