import logging
import math
from pathlib import Path, PurePath
import subprocess
import sys
import shlex

def set_verbosity_level(level):
    """
    Sets the verbosity level
    Parameters:
        level: The level to set the verbosity at
    """
    levels = {
        "debug": logging.DEBUG,
        "info": logging.INFO,
        "warning": logging.WARNING,
        "error": logging.ERROR,
        "critical": logging.CRITICAL
    }

    log_level = levels.get(level.lower(), -1)

    if log_level != -1:
        logging.basicConfig(stream=sys.stderr, level=log_level)
    else:
        print("Logging level must be a value from", levels.keys())
        exit(1)

def write_result(file, test_name, result, build):
    """Write data to file"""
    name_output, info_output = get_header(test_name, result, build)
    file.write(name_output + "\n")
    file.write(info_output + "\n\n")
    file.write(result["output"] + "\n")

def get_header(test_name, result, build):
    if result["result"]:
        success = "SUCCEEDED"
    else:
        success = "FAILED"
    info = "{} {}, returned {}".format(build, success, result["exit-code"])
    return (append_equals(test_name, max(len(test_name), len(info) + 10)),
            append_equals(info, max(len(test_name), len(info) + 10)))
    
def append_equals(text, length):
    equals_length = length - len(text)
    return "=" * math.floor(equals_length / 2) + text + "=" * math.ceil(equals_length / 2)

def run(cmd, env=None):
    """
    Runs a command
    Parameters:
        cmd: The command to execute. Must already be split by shlex
        env: The environment to run it in
    Returns:
        A return code of the command and the stdout of the command
    """
    print("{}".format(cmd))
    p = subprocess.Popen(cmd,
                         env=env,
                         stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT)
    stdout = []
    while True:
        line = p.stdout.readline().decode("utf-8")
        stdout.append(line)
        sys.stdout.write(line)
        if line == '' and p.poll() != None:
            break
    p.wait()
    return p.returncode, stdout

def buildit(collector, host ="localhost", build_args = "", test_run = "",
              test_suite="", java_home="/usr/lib/jvm/default-java", rerun_corrupted=3):
    """
    Runs buildit with particular collector
    Args:
        host: Which host to use
        build_args: The build arguments to use
        test_run: Which test to run
        test_suite: Which test suite to run
        java_home: Location of java
        rerun_corrupted: Number of times to rerun if corrupted zip is found
    Returns:
        Whether or not the JVM built successfully or not
    """
    assert rerun_corrupted >= 0, "Number of times to rerun corrupted must be greater than 0"
    build = False
    for _ in range(0, rerun_corrupted):
        rerun = False
        cmd = shlex.split("bin/buildit {} -j {} --answer-yes {} {} --nuke".format(host, java_home, build_args, collector))
        exit_code, stdout = run(cmd)
        for line in stdout:
            if ("corrupted zip file") in line:
                rerun = True
        if not rerun:
            break
    if exit_code == 0:
        build = True
    directory = ""
    for line in stdout:
        if "jikesrvm/results/buildit" in line:
            for word in line.split():
                if "jikesrvm/results/buildit" in word:
                    directory = word
                    break
    results_dir = directory.split("/")
    buildit_index = results_dir.index("buildit")
    return build, results_dir[buildit_index + 1]

def test_jvm(args):
    """
    Tests the particular build configuration

    Args:
        args: List of test arguments
    Returns:
        The exit-code of the test
    """
    cmd = ("dist/" + args.collector + "_x86_64-linux/rvm "
                   + args.args + " -jar benchmarks/dacapo-2006-10-MR2.jar fop").split()
    return buildit(cmd)

def get_builds(args, jikesrvm_dir, path=Path("build")/"configs"):
    """
    Gets all garbage collection schemes

    Args:
        jikesrvm_dir: The root directory of JikesRVM
        path: The path to look at for build configurations
    Returns:
        The exit-code of the test
    """
    if args.test_run != "":
        garbage_collector_list = [""]
    else:
        garbage_collector_list = []
    conf = jikesrvm_dir / path
    for file in conf.glob("*.properties"):
        name = file.name
        build_conf, mach_conf = name.split(".")
        garbage_collector_list.append(build_conf)
    return garbage_collector_list