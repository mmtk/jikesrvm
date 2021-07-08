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
package org.mmtk.plan;

import org.mmtk.utility.Log;
import org.mmtk.utility.options.Options;
import org.mmtk.utility.sanitychecker.SanityCheckerLocal;

import org.mmtk.vm.VM;

import org.vmmagic.pragma.*;

/**
 * This class (and its sub-classes) implement <i>per-collector thread</i>
 * behavior and state.<p>
 *
 * MMTk assumes that the VM instantiates instances of CollectorContext
 * in thread local storage (TLS) for each thread participating in
 * collection.  Accesses to this state are therefore assumed to be
 * low-cost during mutator time.
 *
 * @see CollectorContext
 */
@Uninterruptible
public class ThirdPartyCollector extends SimpleCollector {
}