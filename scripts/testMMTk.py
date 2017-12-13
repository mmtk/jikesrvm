import subprocess
import argparse

compilers = ["FastAdaptive", "BaseBase", "FullAdaptive"]

parser = argparse.ArgumentParser(description="Runs tests to verify it compiles and builds")
parser.add_argument("-c", dest="compiler", default="FullAdaptive", help="Specifies which compiler to use", type=str)
parser.add_argument("-t", default=1, dest="tests", help="Specifies the number of times to recompile and run the file", type=int)

args = parser.parse_args()

def exe(cmd, env=None):
    print "{}".format(cmd)
    p = subprocess.Popen(cmd,
                         env=env)
    p.wait()
    return p.returncode

# Check values were correct
if not args.compiler in compilers:
    print ("Compiler chosen is not a valid compiler")
    exit(1)

passes = 0

# Run tests
for _ in range(0, args.tests):
    build = False
    # Build the program. Since it sometimes fails because of corrupted zip, run it 3 times at most
    for _ in range (0, 3):
        if exe(("bin/buildit localhost -j /usr/lib/jvm/default-java --answer-yes R" + args.compiler + "NoGC --nuke").split()) == 0:
            build = True 
            break
    if not build:
        print ("Build failed.")
        exit(1)
    if exe(("dist/R" + args.compiler + "NoGC_x86_64-linux/rvm -Xms500M -jar benchmarks/dacapo-2006-10-MR2.jar fop").split()) != 0:
        if passes != 0:
            print ("Test failed. Succeeded previously " + str(passes) + " times.")
        else:
            print ("Test failed.")
        exit(1)
    passes += 1

if passes == args.tests:
   print ("Tests passed.")
else:
   print ("Script failed.")
   exit(1)