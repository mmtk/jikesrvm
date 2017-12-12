import subprocess

def exe(cmd, env=None):
    print "{}".format(cmd)
    p = subprocess.Popen(cmd,
                         env=env)
    p.wait()
    return p.returncode

build = 0
passes = 0
tests = 0

build = False
for y in range(0, 2):
    if exe("bin/buildit localhost -j /usr/lib/jvm/default-java RBaseBaseNoGC --nuke".split()) == 0:
			build = True        
			break
if build == False:
    print ("build failure :(")
    exit(1)
elif exe("dist/RBaseBaseNoGC_x86_64-linux/rvm -Xms500M -jar benchmarks/dacapo-2006-10-MR2.jar fop".split()) != 0:
    print ("Passed FastAdaptive")
else:
    print ("Failed FastAdaptive") 
    exit(1)
