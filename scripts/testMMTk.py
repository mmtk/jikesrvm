#!/usr/bin/env python
import subprocess  # run program
import argparse  # parsing arguments to python
import sys  # run program
import glob, os  # retrieve files in directory
from pathlib import Path
import logging
import helper

jikesrvm_dir = Path(os.path.abspath(os.path.dirname(__file__))) / ".."

# Set logging level. Levels are debug, info, warning, error, critical
logging.basicConfig(stream=sys.stderr, level=logging.ERROR)

######################
# Arguments to parse #
######################

parser = argparse.ArgumentParser(description="Runs tests to verify whether it compiles and builds")
parser.add_argument("-g", dest="collector", default="", help="Specifies which garbage collector to use", type=str)
parser.add_argument("-n", default=1, dest="tests", help="Specifies the number of times to recompile and run the file",
                    type=int)
parser.add_argument("-T", dest="test_run", default="",
                    help="Specifies what group of configured tests to run. Do not specify a garbage collector with this",
                    type=str)
parser.add_argument("-t", dest="test", default="",
                    help="Specifies what specific test to run when building. This one requires a specific garbage collector to run the tests on",
                    type=str)
parser.add_argument("-j", dest="java_home", default="/usr/lib/jvm/default-java", 
                    help="Specifies the location of JAVA_HOME", type=str)
parser.add_argument("-a", dest="args", default="Xms1024M", help="Specifies which arguments to pass when testing MMTk",
                    type=str)
parser.add_argument("--build-only", dest="build_only", action="store_true",
                    help="Only build the compiler but do not test it")
parser.add_argument("-b", dest="build_args", default="", help="Specifies which arguments to pass when building JikesRVM", type=str)
parser.add_argument("--test-only", dest="test_only", action="store_true",
                    help="Do not build the compiler, only test it"),
parser.add_argument("--logging", dest="log", help="Specifies the verbosity of logging", type=str)
args = parser.parse_args()

########################################
# Generate list of all valid GC schemes#
########################################

garbage_collector_list = helper.get_builds(args, jikesrvm_dir)

########################################
# Check arguments are passed correctly #
########################################

# Valid GC
if not args.collector in garbage_collector_list:
    print ("Garbage Collector chosen is not a valid collection scheme")
    exit(1)

# Check that the build and test flags are not both set
if args.build_only and args.test_only:
    print ("Must either build or test compiler")
    exit(1)

if args.test_run != "":
    args.test_run = "--test-run " + args.test_run

if args.test != "":
    args.test = " -t " + args.test

if args.args != "":
    args.args = args.args.replace(" ", " -")
    args.args = "-" + args.args

if args.build_args != "":
    args.build_args = args.build_args.replace(" ", " -")
    args.build_args = "-" + args.build_args + " "
    print (args.build_args)

#############
# Run tests #
#############

build = False
test_passed = 0
for _ in range(0, args.tests):
    if not args.test_only:
        #################
        # Build the jvm #
        #################
        build = helper.build_jvm(args)
        if not build:
            print ("Build failed.")
            exit(1)
    if not args.build_only and args.collector != "":
        ##################
        # Test the build #
        ##################
        test = helper.test_jvm(args)
        if test != 0:
            if test_passed != 0:
                print("Test failed. Succeeded previously " + str(test_passed) + " times.")
            else:
                print("Test failed.")
            exit(1)
        test_passed += 1


if args.collector == "":
    print ("Tests performed.")
elif test_passed == args.tests:
    print ("Tests passed.")
elif build:
    print ("Build successful")
else:
    print ("Script failed.")
    exit(1)
