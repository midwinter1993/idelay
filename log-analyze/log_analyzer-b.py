#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
from litelog import LiteLog
from constraint import Variable, ConstaintSystem
from collections import defaultdict
from typing import Dict


def organize_by_obj_id(thread_log):
    obj_id_log = defaultdict(list)

    for log in thread_log.values():
        for log_entry in log:
            obj_id_log[log_entry.object_id_].append(log_entry)

    for obj_id in obj_id_log:
        obj_id_log[obj_id].sort(key=lambda log_entry: log_entry.tsc_)

    return obj_id_log


def close_enough(x, y):
    DISTANCE = 10000000

    if x > y:
        x, y = y, x

    return y < x + DISTANCE


def near_miss_encode(cs, thread_log, obj_id_log):

    for log in obj_id_log.values():
        for idx, end_log_entry in enumerate(log):
            for j in range(idx - 1, -1, -1):
                start_log_entry = log[j]

                start_tsc, end_tsc = start_log_entry.tsc_, end_log_entry.tsc_

                if not close_enough(start_tsc, end_tsc):
                    break

                if not start_log_entry.is_conflict(end_log_entry):
                    continue

                #
                # We just encode constraints for call operations now
                #
                rel_var_list = [
                    Variable.release_var(log_entry)
                    for log_entry in thread_log[start_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc)
                    if log_entry.is_candidate()
                ]
                cs.add_release_constraint(rel_var_list)

                #
                # For acquiring sites, implementing window + 1
                # Add a log whose tsc < start_tsc
                #
                acq_var_list = [
                    Variable.acquire_var(log_entry)
                    for log_entry in thread_log[end_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc, left_one_more=True)
                    if log_entry.is_candidate()
                ]

                cs.add_acquire_constraint(acq_var_list)

                #for debugging
                print()
                print("Find a nearmiss : ")
                print("Start op : ",start_log_entry.op_type_,"|",start_log_entry.location_, "|", start_log_entry.tsc_)
                print("Releasing window : ")
                for i in rel_var_list:
                    print(i.description_," ",i.loc_)
                print("End   op : ",end_log_entry.op_type_, "|", end_log_entry.location_, "|", end_log_entry.tsc_)
                print("Acquiring window : ")
                for i in acq_var_list:
                    print(i.description_," ",i.loc_)
                print()

    return cs


# def find_potential_delayed_acq(log_list, start_tsc):
#     for i in range(len(log_list) -1):
#         if log_list[i].tsc_ <= start_tsc and log_list[i+1].tsc_ >= start_tsc :
#             return log_list[i]
# return None

if __name__ == "__main__":

    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--batch', help='the log directory')
    args = dirparser.parse_args()

    constraints = ConstaintSystem()

    print(args.batch)
    log_dir = args.batch

    for test in os.listdir(log_dir):
        test_dir = os.path.join(log_dir, test)
        log_files = [f for f in os.listdir(test_dir) if f.endswith(".litelog")]
        print(f'Test {test_dir} log files size : {len(log_files)}')


        #
        # Load the lite log by thread ID
        # TODO: paralleled
        #
        thread_log: Dict[str, LiteLog] = {
            log_name: LiteLog.load_log(os.path.join(test_dir, log_name))
            for log_name in log_files 
        }

        #
        # Patch thread id for each log entry
        #
        for thread_id, log in thread_log.items():
            for log_entry in log:
                log_entry.thread_id_ = thread_id
            print(thread_id, " log size:", len(log))

        #
        # Organize the log by object ID
        # TODO: paralleled
        #
        obj_id_log = organize_by_obj_id(thread_log)

        #
        # Search the nearmiss
        # TODO: paralleled
        #
        near_miss_encode(constraints, thread_log, obj_id_log)

    constraints.print_system()
    print('===== LP solving =====')
    constraints.lp_solve()
    #print('===== Gurobi solving =====')
    #constrains.gurobi_solve()
