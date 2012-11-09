#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
Wrapper for testing mixed workload vs pure OLTP and OLAP
workload.

Usage:
 ./run_mix_vs_single [--no-test]
 ./run_mix_vs_single -h | --help

Options:
    --no-test       Don't run oltpbenchmark,
                    use existing saved .res files.
    -h --help     Show this screen.
"""
from subprocess import check_call
from contextlib import contextmanager
import os
import sys
import pylab as p
import shutil

try:
    from docopt import docopt
except ImportError:
    print "You need docopt to specify options"

PATH_TO_OLTP = ".."
PATH_TO_PLOTTER = os.path.abspath("plot/")
sys.path.insert(0, PATH_TO_PLOTTER)

from plot_latencies import ThroughputExtractor, LatencyExtractor

class WorkloadConfig(object):
    """Contains information on workload"""

    def __init__(self, workloads, config_path, query_ranges={}):
        self.workloads = workloads
        self.config_path = config_path
        self.query_ranges = query_ranges


CONFIGS = {'TPCC': WorkloadConfig('tpcc', 'config/tpcc_config_postgres.xml',
                     {'OLTP': (2, 6)},
                ),
        'CH': WorkloadConfig('chbenchmark', 'config/ch_config_postgres.xml',
                    {'OLAP': (2, 23)},
                ),
        'MIXED': WorkloadConfig('tpcc,chbenchmark',
                         'config/mix_config_postgres.xml',
                            {'OLTP': (2, 6),
                             'OLAP': (7, 28),
                            },
                ),
            }


def run_test(name, config):
    """Runs tpcc and returns throughput and latency extractor objects"""

    check_call(["./oltpbenchmark",
                    '-b', config.workloads,
                    '-c', config.config_path,
                    '--create=false',
                    '--load=false',
                    '--execute=true',
                    '-s', "5",
                    '-o', 'output',
                    '--histograms',
                    ])

    shutil.copyfile("output.raw", name + ".res")


def create_latency_diagrams(data):
    """Creates latency diagrams"""
    latency_figure = p.figure(figsize=(20, 6), dpi=80)
    ymax = data['MIXED']['LATENCY'].get_ymax()

    for number, config_name in enumerate(('CH', 'MIXED')):
        config = CONFIGS[config_name]

        subplot = latency_figure.add_subplot(1, 2, number + 1)
        latency_results = \
                     data[config_name]['LATENCY'].\
                        extract(config.query_ranges['OLAP'])

        LatencyExtractor.decorate_subplot(subplot,
                                 latency_results,
                                 ymax=ymax,
                                 title=config_name,
                                 )
    p.savefig("OLAP.svg")
    p.show()


def create_throughput_diagrams(data):
    """Creates throughput diagrams"""

    throughput_figure = p.figure()
    subplot = throughput_figure.add_subplot(111)

    for config_name in ('TPCC', 'MIXED'):
        config = CONFIGS[config_name]

        throughput_data = \
                data[config_name]['THROUGHPUT'].\
                    extract(config.query_ranges['OLTP'])

        ThroughputExtractor.decorate_subplot(subplot,
                                            throughput_data,
                                            "Throughput",
                                            config_name)

    p.savefig("OLTP.svg")
    p.show()


@contextmanager
def chdir(dest):
    """To the dest and back again"""
    old_dir = os.getcwd()
    os.chdir(dest)
    yield
    os.chdir(old_dir)


def main():
    """Main runner"""

    if "docopt" in globals():
        arguments = docopt(__doc__)
        print "found"
    else:
        arguments = {}
        
    if arguments.get("--help"):
        print __doc__
        sys.exit(0)
    with chdir(PATH_TO_OLTP):
        results = {}
        for name, config in CONFIGS.items():
            if not arguments.get("--no-test"):
                run_test(name, config)
            results[name] = {'LATENCY': LatencyExtractor(name + ".res"),
                            'THROUGHPUT': ThroughputExtractor(name + ".res")}

        create_latency_diagrams(results)
        create_throughput_diagrams(results)

    raw_input("Press enter to EXIT")
    sys.exit(0)

if __name__ == "__main__":
    main()
