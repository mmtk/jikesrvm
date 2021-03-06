#!/usr/bin/env python3
# Code snippets from https://github.com/caizixian/menthol
import sys
import subprocess
import datetime
import pathlib
import socket
import os

here = os.path.dirname(os.path.abspath(__file__))


def mkdirp(path):
    pathlib.Path(path).mkdir(parents=True, exist_ok=True)


def main():
    n = int(sys.argv[1])
    app = sys.argv[2:]

    date_str = datetime.datetime.now().strftime("%Y-%m-%d-%H%M%S")
    hostname = socket.gethostname()
    log_dirname = "{}-{}".format(hostname, date_str)
    log_dir = os.path.join(here, "..", "results", "crashme", log_dirname)
    mkdirp(log_dir)

    for i in range(n):
        print(i, end="", flush=True)
        backtrace_env = os.environ.copy()
        backtrace_env["RUST_BACKTRACE"] = "1"
        p = subprocess.run(app, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                           env=backtrace_env)
        stdout_file = pathlib.Path(log_dir) / "{}[{}].stdout".format(i, p.returncode)
        stderr_file = pathlib.Path(log_dir) / "{}[{}].stderr".format(i, p.returncode)
        with stdout_file.open("wb") as stdoutfd, stderr_file.open("wb") as stderrfd:
            stdoutfd.write(p.stdout)
            stderrfd.write(p.stderr)
        if p.returncode == 0:
            print(".", end="", flush=True)
        else:
            print("*", end="", flush=True)

    print()


if __name__ == '__main__':
    main()
