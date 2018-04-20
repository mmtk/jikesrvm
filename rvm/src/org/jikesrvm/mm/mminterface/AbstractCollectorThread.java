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

import org.jikesrvm.scheduler.SystemThread;
import org.vmmagic.pragma.*;
import org.vmmagic.unboxed.Address;

/**
 * System thread used to perform garbage collection work.
 */
@NonMoving
abstract class AbstractCollectorThread extends SystemThread {

  /***********************************************************************
   *
   * Class variables
   */

  /***********************************************************************
   *
   * Instance variables
   */

  @Entrypoint
  private Address workerInstance = Address.zero();

  public void setWorker(Address worker) {
    rvmThread.assertIsCollector();
    workerInstance = worker;
  }

  /***********************************************************************
   *
   * Initialization
   */

  /**
   * @param stack The stack this thread will run on
   *  functionality
   */
  public AbstractCollectorThread(byte[] stack) {
    super(stack, "AbstractCollectorThread");
    rvmThread.collectorContext.initCollector(nextId);
    nextId++;
  }

  /** Next collector thread id. Collector threads are not created concurrently. */
  private static int nextId = 0;

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

