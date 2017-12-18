import subprocess
import argparse
import sys

compilers = ["FastAdaptive", "BaseBase", "FullAdaptive"]
collectors = ["NoGC", "MarkSweep", "MarkCompact", "SemiSpace", "RefCount", "Poisoned", "GenImmix", "GenRC",
              "Immix", "ConcMS", "CopyMS", "GenMS", "GCTrace", "StickyImmix", "StickyMS"]

parser = argparse.ArgumentParser(description="Runs tests to verify it compiles and builds")
parser.add_argument("-c", dest="compiler", default="BaseBase", help="Specifies which compiler to use", type=str)
parser.add_argument("-g", dest="collector", default="NoGC", help="Specifies which garbage collector to use", type=str)
parser.add_argument("-n", default=1, dest="tests", help="Specifies the number of times to recompile and run the file", type=int)
parser.add_argument("-a", dest="args", default="-Xms500M", help="Specifies which arguments to pass when testing MMTk", type=str)
parser.add_argument("--build-only", dest="build_only", action="store_true", help="Only build the compiler but do not test it")
parser.add_argument("--test-only", dest="test_only",action="store_true", help="Do not build the compiler, only test it")
parser.add_argument("--java-build", dest="java_build", action="store_true", help="Build using the java MMTk instead of the rust MMTk")

args = parser.parse_args()

def exe(cmd, env=None):
    print ("{}".format(cmd))
    p = subprocess.Popen(cmd,
                         env=env,
                         stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT)
    stdout = []
    while True:
        line = p.stdout.readline()
        stdout.append(line)
        sys.stdout.write(line)
        if ("corrupted zip file") in line:
            return 3
        if line == '' and p.poll() != None:
            break
    p.wait()
    return p.returncode

# Check arguments passed are correct
if not args.compiler in compilers:
    print ("Compiler chosen is not a valid compiler")
    exit(1)
if not args.collector in collectors:
    print ("Garbage Collector chosen is not a valid collection scheme")
    exit(1)

# Check that the build and test flags are not both set
if args.build_only and args.test_only:
    print ("Must either build or test compiler")
    exit(1)

# Set the arguments depending on which MMTk
# version is being built
if args.java_build:
    args.java_build = ""
else:
    args.java_build = "R"

passes = 0

# Run tests
for _ in range(0, args.tests):
    # Build if not test only
    if not args.test_only:
        build = False
        # Corrupted zips return an error code of 3.
        # If it corrupts, rerun at most 3 times
        # All other build errors return errors as usual
        for _ in range (0, 3):
            rc = exe(("bin/buildit localhost -j /usr/lib/jvm/default-java --answer-yes " +
                       args.java_build + args.compiler + args.collector + " --nuke").split())
            if rc != 3:
                build = rc == 0 
                break
        if not build:
            print ("Build failed.")
            exit(1)
    # Test if not build only
    if not args.build_only:
        if exe(("dist/" + args.java_build + args.compiler + args.collector + "_x86_64-linux/rvm " 
                + args.args + " -jar benchmarks/dacapo-2006-10-MR2.jar fop").split()) != 0:
            if passes != 0:
                print ("Test failed. Succeeded previously " + str(passes) + " times.")
            else:
                print ("Test failed.")
            exit(1)
        passes += 1

# Check if the script has passed
if passes == args.tests:
   print ("Tests passed.")
elif build:
   print ("Build successful")
else:
   print ("Script failed.")
   exit(1)