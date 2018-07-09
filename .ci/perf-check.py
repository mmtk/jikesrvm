# if a benchmark's run-time for Rust MMTk is longer than the stock MMTk
# by 5%, we mark it as fail.
TOLERANCE=0.05

import sys
import os

if len(sys.argv) != 3:
    print "Usage: python perf-check.py results_path PlanName"
    sys.exit(2)

path = sys.argv[1]
plan = sys.argv[2]
rplan = "R" + plan

# all the logs
all_logs_dir = path

# log directories
log_dirs = [x[0] for x in os.walk(all_logs_dir)][1:]

# list all files in log directories, and find related ones
logs = []
for d in log_dirs:
    for f in os.listdir(d):
        if plan in f:
            logs.append(os.path.join(d, f))
print logs

# collect all the benchmarks
benchmarks = set()
for l in logs:
    bm = l[l.rfind('/') + 1:l.find('.')]
    benchmarks.add(bm)

expected_iteration = 0
has_errors = False

# we want to check results for every benchmark
for bm in benchmarks:
    # recording error messages for analysing this benchmark
    error_messages = []

    def record_error(msg):
        error_messages.append(msg)

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
                record_error("Did not find any result for %s in %s" % (bm, log_file))
                return None
            else:
                # check how many iterations we have in the log
                global expected_iteration
                if expected_iteration == 0:
                    expected_iteration = len(execution_times)

                if len(execution_times) != expected_iteration:
                    record_error("Expected all benchmark runs have the same iteration counts. Found %d for %s, while expecting %d" \
                        % (len(execution_times), log_file, expected_iteration))
                    return None
                else:
                    # get average
                    return sum(execution_times) / len(execution_times)


    stock_time = get_execution_time_for_log(stock_mmtk_log)
    rust_time = get_execution_time_for_log(rust_mmtk_log)

    print "-------%s-------" % bm

    if stock_time and rust_time:
        diff = (rust_time - stock_time) / stock_time

        print "Results of %s on %s" % (plan, bm)
        print "Stock: %.2f" % stock_time
        print "Rust : %.2f (%s)" % (rust_time, "{0:.0%}".format(diff))

        if diff > TOLERANCE:
            record_error("The performance difference exceed the tolerance: %f, failed" % diff)

    if len(error_messages) > 0:
        print "Errors: "
        for msg in error_messages:
            print msg
        has_errors = True

print "-------END-------"
if has_errors:
    print "Had errors in logs. Failed. "
    sys.exit(1)
else:
    print "Succeeded. "
    sys.exit(0)
