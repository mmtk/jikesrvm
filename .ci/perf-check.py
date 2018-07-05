# if a benchmark's run-time for Rust MMTk is longer than the stock MMTk
# by 5%, we mark it as fail.
TOLERANCE=0.05

import sys
import os

if len(sys.argv) <= 1:
    print "Need a plan name. "
plan = sys.argv[1]
rplan = "R" + plan

# the root dir of 'running' script
running_root = os.path.join(os.path.dirname(os.path.realpath(__file__)), "running")
# all the logs
all_logs_dir = os.path.join(running_root, "results", "log")

# we need to assert if there is only one valid result directory
results_dir = [x[0] for x in os.walk(all_logs_dir)][1:]
assert len(results_dir) == 1, \
    "Expected only one folder to contain benchmark results, found more than one: %d " % len(results_dir)

# this is the result dir we wil use
result_dir = results_dir[0]
# logs
logs = [f for f in os.listdir(result_dir) if plan in f]

# collect all the benchmarks
benchmarks = set()
for l in logs:
    bm = l[:l.find('.')]
    benchmarks.add(bm)

expected_iteration = 0

# we want to check results for every benchmark
for bm in benchmarks:
    stock_mmtk_logs = [l for l in logs if plan in l and rplan not in l and bm in l]
    assert len(stock_mmtk_logs) == 1, \
        "Expected only 1 log file for %s on %s: found %d" % (plan, bm, len(stock_mmtk_logs))

    rust_mmtk_logs = [l for l in logs if rplan in l and bm in l]  #Rust plan starts with R
    assert len(rust_mmtk_logs) == 1, \
        "Expected only 1 log file for %s on %s: found %d" % (rplan, bm, len(rust_mmtk_logs))

    stock_mmtk_log = stock_mmtk_logs[0]
    rust_mmtk_log = rust_mmtk_logs[0]

    def get_execution_time_for_log(log_file):
        import gzip
        with gzip.open(log_file, 'r') as f:
            content = f.read()
            lines = content.splitlines()
            execution_times = []
            for line in lines:
                import re
                matcher = re.match(".*PASSED in (\d+) msec.*", line)
                if matcher:
                    execution_times.append(float(matcher.group(1)))

            if len(execution_times) == 0:
                print "Did not find any result for %s in %s" % (bm, log_file)
                sys.exit(1)

            # check how many iterations we have in the log
            global expected_iteration
            if expected_iteration == 0:
                expected_iteration = len(execution_times)
            assert len(execution_times) == expected_iteration, \
                "Expected all benchmark runs have the same iteration counts. Found %d for %s, while expecting %d" \
                % (len(execution_times), log_file, expected_iteration)

            # get average
            return sum(execution_times) / len(execution_times), execution_times


    stock_time, _ = get_execution_time_for_log(os.path.join(result_dir, stock_mmtk_log))
    rust_time, _ = get_execution_time_for_log(os.path.join(result_dir, rust_mmtk_log))

    diff = (rust_time - stock_time) / stock_time

    print "Results of %s on %s" % (plan, bm)
    print "Stock: %.2f" % stock_time
    print "Rust : %.2f (%s)" % (rust_time, "{0:.0%}".format(diff))

    if diff > TOLERANCE:
        print "The performance difference exceed the tolerance: %f, failed" % diff
        sys.exit(1)