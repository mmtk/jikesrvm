#!/usr/bin/env python

import subprocess, shlex
import argparse 
import sys 
import logging
import os, glob
import helper
from gen_differences import gen_differences

def run_tests(gc, ci=False):
    host = "localhost" if ci else "deer.moma"
    return helper.build_jvm(gc, test_suite=" -t \"{}\" ".format(tests), host=host)

if __name__ == '__main__':

    # Set logging level. Levels are debug, info, warning, error, critical
    logging.basicConfig(stream=sys.stderr, level=logging.ERROR)

    # Change directory to base directory pointing to JikesRVM
    a = os.path.abspath(os.path.join(__file__, "..", ".."))
    os.chdir(a)  # change to base directory pointing to JikesRVM

    # Arguments
    parser = argparse.ArgumentParser(description="Runs a set of tests to verify the correctness of a Rust GC")
    parser.add_argument("-g", dest="collector", default="", help="Specifies which garbage collector to use", type=str)
    parser.add_argument("--g1", dest="c1", default="", help="Specifies which garbage collector to use", type=str)
    parser.add_argument("--g2", dest="c2", default="", help="Specifies which garbage collector to use", type=str)
    parser.add_argument("-T", dest="test_run", default="commit",
                        help="Specifies which group of configured tests to run",
                        type=str)
    parser.add_argument('--ci', dest='ci', action='store_true', default=False)
    args = parser.parse_args()

    # Only either the g or both the g1 and g2 flags can be set
    assert (args.c1 != "" and args.c2 != "") != (args.collector != "")

    # Generates a list of all garbage collectors supported by MMTk
    garbage_collector_list = []
    for file in glob.glob(os.path.join("build", "configs", "*.properties")):
        name = os.path.basename(file)
        fname, lname = name.split(".")
        garbage_collector_list.append(fname)

    # Valid test runs
    tests = {"core": "SPECjvm98 SPECjbb2005 basic gctest opttests jBYTEmark CaffeineMark javalex jni xerces soot dacapo ipsixql SPECjbb2000", 
            "commit": "basic gctest"}

    # Check valid test runs
    assert args.test_run in tests, "Test suite must be either core or commit"
    tests = tests[args.test_run]

    # Check valid garbage collector
    if args.c1 == "" or args.c2 == "":
        javagc = args.collector
        rustgc = "R" + args.collector
        assert args.collector in garbage_collector_list and "R"+args.collector in garbage_collector_list, \
            "Garbage collector must be valid and have corresponding Rust collector"
    if args.collector == "":
        javagc = args.c1
        rustgc = args.c2
        assert args.c1 in garbage_collector_list and args.c2 in garbage_collector_list

    # Run tests
    b1, r1 = run_tests(javagc, ci=args.ci)
    b2, r2 = run_tests(rustgc, ci=args.ci)

    assert b1 and b2

    # Parse Results
    helper.run(shlex.split("scripts/parse_results.py -r {}".format(r1)))
    helper.run(shlex.split("scripts/parse_results.py -r {}".format(r2)))

    results = gen_differences(javagc, rustgc)

