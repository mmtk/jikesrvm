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

import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.HashMap;

import org.jikesrvm.VM;

/**
 * Provides methods supporting all JMX beans that relate to memory. Functionality
 * for memory management beans generally requires interfacing with MMTk which is
 * why this class belongs to the MMTk-VM interface.
 * <p>
 * In JMX terms, the Jikes RVM provides only one memory manager (the garbage collector)
 * and several memory pools (each of which corresponds to exactly one MMTk space).
 * <p>
 * NOTE: Features related to memory usage are currently not implemented.
 */
public class JMXSupport {

  /**
   * We only provide one garbage collector because the choice of garbage collector
   * for the VM is fixed at build time.
   */
  private static final String[] garbageCollectorNames = { Selected.name };

  /**
   * The names of all memory managers that are not garbage collectors.
   * All of our memory managers are collectors so this is empty.
   */
  private static final String[] memoryManagerNames = {};

  /**
   * Maps a space name to its index in the space array.
   */
  private static HashMap<String, Integer> pools;

  private static String[] poolNames;

  /**
   * The level of verbosity that was used in MMTk when verbosity was switched off.
   * It will be restored when verbosity is switched on again. We can do this
   * because no other part of the system ever switches the verbosity.
   */
  private static int lastMMTkVerbosity;

  /**
   * Initializes data structures.
   * This needs to called before the application starts.
   */
  public static void fullyBootedVM() {
    VM.sysFail("Have not finished implementing fullyBootedVM");
    int spaceCount = 1;  //Space.getSpaceCount();
    pools = new HashMap<String, Integer>(spaceCount * 2);
    for (int i = 0; i < spaceCount; i++) {
      pools.put("FakeSpace", i);
    }
    poolNames = pools.keySet().toArray(new String[spaceCount]);
  }

  public static String[] getGarbageCollectorNames() {
    return garbageCollectorNames;
  }

  public static String[] getMemoryManagerNames() {
    return memoryManagerNames;
  }

  /**
   * @param poolName the name of the pool
   * @return the name of the memory manager(s) of the pool (always
   *  our single garbage collector, i.e. active plan)
   */
  public static String[] getMemoryManagerNames(String poolName) {
    return garbageCollectorNames;
  }

  public static String[] getPoolNames() {
    return poolNames;
  }

  /**
   * Returns non-heap for immortal spaces and heap for non-immortal
   * spaces because objects can be added and remove from non-immortal
   * spaces.
   *
   * @param poolName the pool's name
   * @return the type of the memory pool
   */
  public static MemoryType getType(String poolName) {
    VM.sysFail("Have not finished implementing getType");
    return MemoryType.NON_HEAP;
  }

  /**
   * @param poolName a memory pool name
   * @return whether a space with the given name exists
   */
  public static boolean isValid(String poolName) {
    return pools.get(poolName) != null;
  }

  public static MemoryUsage getUsage(boolean immortal) {
    VM.sysFail("Have not finished implementing getUsage(boolean immortal)");
    long committed = 0, used = 0, max = 0;
    return new MemoryUsage(-1, used, committed, max);
  }

  public static MemoryUsage getUsage(String poolName) {
    VM.sysFail("Have not finished implementing getUsage(String poolName)");
    return new MemoryUsage(-1, 0, 0, 0);
  }

  public static int getObjectPendingFinalizationCount() {
    VM.sysFail("getObjectPendingFinalizationCount is not implemented yet");
    return 0;
  }

  public static synchronized boolean isMMTkVerbose() {
    //FIXME
    VM.sysFail("Rust MMTk cannot be verbose yet");
    return false;
  }

  /**
   * Sets the verbosity for MMTk. Verbosity in MMTk has several levels
   * so this method makes an effort to save the previous verbosity level
   * if possible.
   *
   * @param verbose {@code true} if verbosity is to be enabled,
   *  {@code false} otherwise
   */
  public static synchronized void setMMTkVerbose(boolean verbose) {
    //FIXME
    VM.sysFail("We don't have any setting to turn Rust MMTk Verbose yet");
  }

  public static long getCollectionCount() {
    //FIXME
    VM.sysFail("getCollectionCount is not implemented yet");
    return 0;
  }

  public static long getCollectionTime() {
    //FIXME
    VM.sysFail("getCollectionTime is not implemented yet");
    return 0;
  }

}
