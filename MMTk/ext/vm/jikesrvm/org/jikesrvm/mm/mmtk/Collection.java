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
package org.jikesrvm.mm.mmtk;

import org.mmtk.plan.CollectorContext;
import org.mmtk.plan.MutatorContext;

import org.jikesrvm.architecture.StackFrameLayout;
import org.jikesrvm.mm.mminterface.MemoryManager;
import org.jikesrvm.mm.mminterface.Selected;
import org.jikesrvm.mm.mminterface.CollectorThread;
import org.jikesrvm.runtime.SysCall;
import org.jikesrvm.scheduler.RVMThread;
import org.jikesrvm.scheduler.FinalizerThread;

import org.vmmagic.pragma.Interruptible;
import org.vmmagic.pragma.Uninterruptible;
import org.vmmagic.pragma.UninterruptibleNoWarn;
import org.vmmagic.pragma.Unpreemptible;

@Uninterruptible
public class Collection extends org.mmtk.vm.Collection {

  /****************************************************************************
   *
   * Class variables
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @Interruptible
  public void spawnCollectorContext(CollectorContext context) {
    byte[] stack = MemoryManager.newStack(StackFrameLayout.getStackSizeCollector());
    CollectorThread t = new CollectorThread(stack, context);
    t.start();
  }

  @Override
  public int getDefaultThreads() {
    return SysCall.sysCall.sysNumProcessors();
  }

  @Override
  public int getActiveThreads() {
    return RVMThread.getNumActiveThreads() - RVMThread.getNumActiveDaemons();
  }

  @Override
  @Unpreemptible
  public void blockForGC() {
    org.jikesrvm.mm.mminterface.MemoryManager.blockForGC();
  }

  /***********************************************************************
   *
   * Initialization
   */

  /**
   * {@inheritDoc}
   */
  @Override
  @UninterruptibleNoWarn
  public void outOfMemory() {
    throw RVMThread.getOutOfMemoryError();
  }

  @Override
  public final void prepareMutator(MutatorContext m) {
    RVMThread t = ((Selected.Mutator) m).getThread();
    org.jikesrvm.mm.mminterface.MemoryManager.prepareMutator(t);
  }

  @Override
  @Unpreemptible
  public void stopAllMutators() {
    RVMThread.blockAllMutatorsForGC();
  }

  @Override
  @Unpreemptible
  public void resumeAllMutators() {
    RVMThread.unblockAllMutatorsForGC();
  }

  private static RVMThread.SoftHandshakeVisitor mutatorFlushVisitor =
    new RVMThread.SoftHandshakeVisitor() {
      @Override
      @Uninterruptible
      public boolean checkAndSignal(RVMThread t) {
        t.flushRequested = true;
        return true;
      }
      @Override
      @Uninterruptible
      public void notifyStuckInNative(RVMThread t) {
        t.flush();
        t.flushRequested = false;
      }
    };

  @Override
  @UninterruptibleNoWarn("This method is really unpreemptible, since it involves blocking")
  public void requestMutatorFlush() {
    Selected.Mutator.get().flush();
    RVMThread.softHandshake(mutatorFlushVisitor);
  }

  /***********************************************************************
   *
   * Finalizers
   */

  /**
   * Schedule the finalizerThread, if there are objects to be
   * finalized and the finalizerThread is on its queue (ie. currently
   * idle).  Should be called at the end of GC after moveToFinalizable
   * has been called, and before mutators are allowed to run.
   */
  @Uninterruptible
  public static void scheduleFinalizerThread() {
    int finalizedCount = FinalizableProcessor.countReadyForFinalize();
    if (finalizedCount > 0) {
      FinalizerThread.schedule();
    }
  }
}

