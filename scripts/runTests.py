#!/usr/bin/env python
import subprocess, shlex
import argparse 
import sys 
import logging
import os, glob
import helper
from gen_differences import gen_differences
from parse_results import parse_results
from generate_html import generate_html
from pathlib import Path

def main():
    # Arguments
    parser = argparse.ArgumentParser(description="Runs a set of tests to verify the correctness of a Rust GC")
    parser.add_argument("-g", dest="collector", default="", help="Specifies which set of garbage collectors to use", type=str)
    parser.add_argument("--g1", dest="c1", default="", help="Specifies which garbage collector to use", type=str)
    parser.add_argument("--g2", dest="c2", default="", help="Specifies which garbage collector to use", type=str)
    parser.add_argument("-T", dest="test_run", default="commit",
                        help="Specifies which group of configured tests to run",
                        type=str)
    parser.add_argument('--host', dest='host', default="ox.moma", type=str, help="Specifies which host to use")
    parser.add_argument('-v', dest='verbosity', default="warning", type=str, help="Specifies the verbosity level")
    parser.add_argument('--command', dest='command', default="", type=str, help="Specifies a command to run after the processing is done.")
    args = parser.parse_args()

    assert (args.c1 != "" and args.c2 != "") != (args.collector != "")

    helper.set_verbosity_level(args.verbosity)

    # Valid test runs
    tests = {"core": "SPECjvm98 SPECjbb2005 basic gctest opttests jBYTEmark CaffeineMark javalex jni xerces soot dacapo ipsixql SPECjbb2000", 
            "commit": "basic gctest"}

    # Change directory to base directory pointing to JikesRVM
    JIKESRVM_DIR = Path(os.path.abspath(os.path.join(__file__, "..", "..")))
    os.chdir(str(JIKESRVM_DIR))  # change to base directory pointing to JikesRVM

    # Generates a list of all garbage collectors supported by MMTk
    garbage_collector_list = []
    for file in glob.glob(os.path.join("build", "configs", "*.properties")):
        name = os.path.basename(file)
        fname, lname = name.split(".")
        garbage_collector_list.append(fname)

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

    # Check valid test runs
    assert args.test_run in tests, "Test suite must be either core or commit"
    tests = tests[args.test_run]

    # Run tests
    b1, r1, results1 = run_and_parse(javagc, args.host, tests)
    b2, r2, results2 = run_and_parse(rustgc, args.host, tests)

    #logging.info("Running tests for {}".format(javagc))
    #b1, r1 = run_tests(javagc, host=args.host, tests=tests)
    #logging.info("Parsing {}".format(r1))
    #results1 = parse_results(r1, dump=True)
    #logging.info("Running tests for {}".format(rustgc))
    #b2, r2 = run_tests(rustgc, host=args.host, tests=tests)
    #logging.info("Parsing {}".format(r2))
    #results2 = parse_results(r2, dump=True)

    assert b1 and b2

    results = gen_differences(javagc, rustgc)

    generate_html(javagc, rustgc)

    if args.command != "":
        command = shlex.split(args.command)
        helper.run(command)

    if results['fails'] == 0:
        print("Passed")
        exit(0)
    else:
        print("Failed")
        exit(1)

def run_and_parse(gc, host, tests):
    logging.info("Running tests for {}".format(gc))
    b1, r1 = run_tests(gc, host=host, tests=tests)
    logging.info("Parsing {}".format(r1))
    results1 = parse_results(r1, dump=True)
    return b1, r1, results1

def run_tests(gc, tests, host):
    return helper.buildit(gc, build_args=(" -t \"{}\" ".format(tests)), host=host)

if __name__ == '__main__':
    main()
