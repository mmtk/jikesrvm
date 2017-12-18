import subprocess
import argparse
import sys

compilers = ["FastAdaptive", "BaseBase", "FullAdaptive"]

parser = argparse.ArgumentParser(description="Runs tests to verify it compiles and builds")
parser.add_argument("-c", dest="compiler", default="BaseBase", help="Specifies which compiler to use", type=str)
parser.add_argument("-n", default=1, dest="tests", help="Specifies the number of times to recompile and run the file", type=int)
parser.add_argument("--build-only", dest="build_only", action="store_true", help="Only build the compiler but do not test it")
parser.add_argument("--test-only", dest="test_only",action="store_true", help="Do not build the compiler, only test it")

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

# Check values were correct
if not args.compiler in compilers:
    print ("Compiler chosen is not a valid compiler")
    exit(1)

# Check that the build and test flags are not both set
if args.build_only and args.test_only:
    print ("Must either build or test compiler")
    exit(1)

passes = 0

# Run tests
for _ in range(0, args.tests):
    if not args.test_only:
        build = False
        # Build the program. Since it sometimes fails because of corrupted zip, run it 3 times at most
        for _ in range (0, 3):
            rc = exe(("bin/buildit localhost -j /usr/lib/jvm/default-java --answer-yes R" + args.compiler + "NoGC --nuke").split())
            if rc != 3:
                build = rc == 0 
                break
        if not build:
            print ("Build failed.")
            exit(1)
    if not args.build_only:
        if exe(("dist/R" + args.compiler + "NoGC_x86_64-linux/rvm -Xms500M -jar benchmarks/dacapo-2006-10-MR2.jar fop").split()) != 0:
            if passes != 0:
                print ("Test failed. Succeeded previously " + str(passes) + " times.")
            else:
                print ("Test failed.")
            exit(1)
        passes += 1

if passes == args.tests:
   print ("Tests passed.")
elif build:
   print ("Build successful")
else:
   print ("Script failed.")
   exit(1)