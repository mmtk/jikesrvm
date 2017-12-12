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

# this doesnt work :(

for x in range(0, 5):
    build = 0
    for y in range(0, 3):
        if exe("bin/buildit localhost -j /usr/lib/jvm/default-java RBaseBaseNoGC --nuke".split()) == 0:
            break
        build += 1
    if build == 3:
        print ("build failure :(")
        break
    tests += 1
    if exe("dist/RBaseBaseNoGC_x86_64-linux/rvm -Xms500M -jar benchmarks/dacapo-2006-10-MR2.jar fop".split()) != 0:
        break
    passes += 1

print ("Score: " + str(passes) + "/" + str(tests))
