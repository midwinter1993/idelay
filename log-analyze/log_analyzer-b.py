#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
from litelog import LiteLog, LogEntry
from constraint import Variable, ConstaintSystem
from collections import defaultdict
from typing import Dict
from APISpecification import APISpecification

import time

import multiprocessing 
from joblib import Parallel, delayed
from joblib.externals.loky import set_loky_pickler
from joblib import parallel_backend
from joblib import wrap_non_picklable_objects
#import dill as pickle

def organize_by_obj_id(thread_log):
    obj_id_log = defaultdict(list)
    obj_id_threadlist = defaultdict(list)

    for log in thread_log.values():
        for log_entry in log:
            if log_entry.is_write() or log_entry.is_read():
                obj_id_log[log_entry.object_id_].append(log_entry)
                if log_entry.thread_id_ not in obj_id_threadlist[log_entry.object_id_]:
                    obj_id_threadlist[log_entry.object_id_].append(log_entry.thread_id_)

    for obj_id in obj_id_log:
        #for tid in obj_id_log[obj_id]:
        obj_id_log[obj_id].sort(key=lambda log_entry: log_entry.tsc_)

    return obj_id_log, obj_id_threadlist 


def close_enough(x, y):
    DISTANCE = 10000000

    if x > y:
        x, y = y, x

    return y < x + DISTANCE


def near_miss_encode(cs, thread_log, obj_id_log, obj_id_threadlist):
    
    klen = 5
    
    near_miss_dict = {}

    if 'null' in obj_id_log:
        del obj_id_log['null']
    
    #for obj_id in obj_id_log:
    #    if "Call" in obj_id_log[obj_id][0].op_type_ and len(obj_id_log[obj_id]) >1:
    #        for log in obj_id_log[obj_id]:
    #           print(obj_id,"TID =",log.thread_id_, " api ", log.description_)
    progress = 0

    for log in obj_id_log.values():
        progress += 1

        if len(log) < 2:
            continue
        
        ex_entry = log[0]
        if len(obj_id_threadlist[ex_entry.object_id_]) < 2:
            continue
        #print("obj_id_log size ",len(log), " thread number ", len(obj_id_threadlist[ex_entry.object_id_]), ex_entry.object_id_)
        #print(progress, " / ", len(obj_id_log))
        # nm_cnt = 0
        
        for idx, end_log_entry in enumerate(log):
            
            for j in range(idx - 1, max(idx - klen-1,-1), -1):
            #for j in range(idx - 1, -1, -1):
                start_log_entry = log[j]

                start_tsc, end_tsc = start_log_entry.tsc_, end_log_entry.tsc_

                if not close_enough(start_tsc, end_tsc):
                    break

                if not start_log_entry.is_conflict(end_log_entry):
                    continue
                
                sig = start_log_entry.description_ + "!" + end_log_entry.description_

                if sig in near_miss_dict:
                    continue
                near_miss_dict[sig] = 1

                start = time.time()

                #
                # We just encode constraints for call operations now
                #
                rel_var_list = [
                    Variable.release_var(log_entry)
                    for log_entry in thread_log[start_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc, left_one_more = False, right_one_less = True)
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
                    range_by(start_tsc, end_tsc, left_one_more=True, right_one_less = False)
                    if log_entry.is_candidate()
                ]

                cs.add_acquire_constraint(acq_var_list)
                
                end = time.time()
                #print("one time near miss constraints time",end - start)

                #
                # only one pair
                #
                #break  

                #for debugging
                #'''
                #if "Call" not in start_log_entry.op_type_:
                #    continue
                #if 'Finalize-Begin' in acq_var_list[0].description_:
                #    continue
                    
                print()
                print("Find a nearmiss : ")

                print("Start op : ",start_log_entry.op_type_,"|",start_log_entry.operand_,"|",start_log_entry.location_, "|", start_log_entry.tsc_)
                print("Releasing window : ")
                s = '1 <= '
                for i in rel_var_list:
                    print(i.description_," ",i.loc_)
                    s += 'R' + str(i.uid_) + ' + '
                print(s)

                print("End   op : ",end_log_entry.op_type_, "|",end_log_entry.operand_,"|", end_log_entry.location_, "|", end_log_entry.tsc_)
                print("Acquiring window : ")
                s = '1 <= '
                for i in acq_var_list:
                    print(i.description_," ",i.loc_)
                    s += 'A' + str(i.uid_) + ' + '
                print(s)
                print()
                #'''
        #print("near-miss count ", nm_cnt)
    return cs

def generate_constraints_for_every_test(log_dir, test, constraints):
    
    test_dir = os.path.join(log_dir, test)
    
    log_files = [f for f in os.listdir(test_dir) if f.endswith(".litelog")]
    print(f'Test {test_dir} log files size : {len(log_files)}')
    
    if len(log_files) < 2:
        return
    #
    # Load the lite log by thread ID
    # TODO: paralleled
    #
    start = time.time()
    
    thread_log: Dict[str, LiteLog] = {
        log_name: LiteLog.load_log(os.path.join(test_dir, log_name))
        for log_name in log_files
    }
    
    end = time.time()
    
    print(test, "load log time",end - start)
    
    #
    # Patch thread id for each log entry
    #
    start = time.time()
    
    for thread_id, log in thread_log.items():
        for log_entry in log:
            log_entry.thread_id_ = thread_id
        # print(thread_id, " log size:", len(log))
             
    
    end = time.time()
    print(test, "assign thread id time",end - start)
    
    #
    # Organize the log by object ID
    # TODO: paralleled
    #
    
    start = time.time()
    obj_id_log, obj_id_threadlist = organize_by_obj_id(thread_log)
             
    end = time.time()
    print(test, "reconstruct the log time",end - start)
    
    #
    # Search the nearmiss
    # TODO: paralleled
    #
    start = time.time()
    
    near_miss_encode(constraints, thread_log, obj_id_log, obj_id_threadlist)
    end = time.time()
    
    print(test, "near miss encode time",end - start)

if __name__ == "__main__":

    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--batch', help='the log directory')
    args = dirparser.parse_args()

    constraints = ConstaintSystem()
    APISpecification.Initialize()
    print(args.batch)
    log_dir = args.batch

    for test in os.listdir(log_dir):
        generate_constraints_for_every_test(log_dir, test, constraints)
    
    #parallel_results = Parallel(n_jobs = 1, backend="threading") \
    #        (delayed(generate_constraints_for_every_test)(log_dir,test,constraints) for test in os.listdir(log_dir))
    #Parallel(n_jobs=4)(delayed(generate_constraints_for_every_test)(log_dir,test,constraints) for test in os.listdir(log_dir))
    #print(parallel_results)
    
    constraints.set_reg_weight(LogEntry.map_api_entry)

    constraints.print_system()
    print('===== LP solving =====')
    constraints.lp_solve()
    #print('===== Gurobi solving =====')
    #constrains.gurobi_solve()
