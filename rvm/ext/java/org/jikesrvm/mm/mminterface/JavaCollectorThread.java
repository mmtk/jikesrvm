package org.jikesrvm.mm.mminterface;

import org.mmtk.plan.CollectorContext;
import org.vmmagic.pragma.*;

@NonMoving
public class JavaCollectorThread extends AbstractCollectorThread {
    /**
     * @param stack   The stack this thread will run on
     * @param context the context that will provide the thread's
     */
    public JavaCollectorThread(byte[] stack, CollectorContext context) {
        super(stack, context);
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
           rvmThread.collectorContext.run();
    }

}

