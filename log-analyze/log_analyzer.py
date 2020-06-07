#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os

from litelog import LiteLog, LogEntry
from constraint import Variable, ConstaintSystem
from APISpecification import APISpecification
from nearmiss import near_miss_encode, organize_by_obj_id

from collections import defaultdict
from typing import Dict

import time

checkpoint_dir = 'E:/Sherlock/idelay/log-analyze/temp'

def generate_constraints_for_every_test(log_dir, test, constraints):

    test_dir = os.path.join(log_dir, test)

    log_files = [f for f in os.listdir(test_dir) if f.endswith(".litelog")]

    if len(log_files) < 2:
        return
    #print(f'Test {test_dir} log files size : {len(log_files)}')

    thread_log: Dict[str, LiteLog] = {
        log_name: LiteLog.load_log(os.path.join(test_dir, log_name))
        for log_name in log_files
    }

    for thread_id, log in thread_log.items():
        for log_entry in log:
            log_entry.thread_id_ = thread_id

    obj_id_log, obj_id_threadlist = organize_by_obj_id(thread_log)

    near_miss_encode(constraints, thread_log, obj_id_log, obj_id_threadlist)


if __name__ == "__main__":

    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--batch', help='the log directory')
    dirparser.add_argument('-refine',  action='store_true')
    args = dirparser.parse_args()

    constraints = ConstaintSystem()
    APISpecification.Initialize()

    if args.refine :
        constraints.load_checkpoint(checkpoint_dir)
        constraints._lp_solve()
        constraints.print_compare_result(constraints.pre_rel_vars_, constraints.pre_acq_vars_)

    else:
        print(args.batch)
        log_dir = args.batch
        for test in os.listdir(log_dir):
            generate_constraints_for_every_test(log_dir, test, constraints)
        constraints._lp_solve()
        constraints.print_result()
        print("Load Checkpoint")
        constraints.save_info(checkpoint_dir)
