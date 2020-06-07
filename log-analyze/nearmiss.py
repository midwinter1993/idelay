
import os
from litelog import LiteLog, LogEntry
from constraint import Variable, ConstaintSystem
from collections import defaultdict
from typing import Dict
from APISpecification import APISpecification

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

    progress = 0
    for log in obj_id_log.values():
        progress += 1

        if len(log) < 2:
            continue
        ex_entry = log[0]
        if len(obj_id_threadlist[ex_entry.object_id_]) < 2:
            continue

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

                #
                # We just encode constraints for call operations now
                #

                rel_log_list = [
                    log_entry
                    for log_entry in thread_log[start_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc, left_one_more = False, right_one_less = True)
                    if log_entry.is_candidate()
                ]

                #
                # For acquiring sites, implementing window + 1
                # Add a log whose tsc < start_tsc
                #

                acq_log_list = [
                    log_entry
                    for log_entry in thread_log[end_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc, left_one_more=True, right_one_less = False)
                    if log_entry.is_candidate()
                ]

                cs.add_constraint(rel_log_list, acq_log_list)


                #for debugging
                '''
                #if "Call" not in start_log_entry.op_type_:
                #    continue
                #if 'Finalize-Begin' in acq_var_list[0].description_:
                #    continue

                print()
                print("Find a nearmiss : ")

                print("Start op : ",start_log_entry.op_type_,"|",start_log_entry.operand_,"|",start_log_entry.location_, "|", start_log_entry.tsc_)
                print("Releasing window : ")
                s = '1 <= '
                for log_entry in rel_log_list:
                    i = Variable.release_var(log_entry)
                    print(i.description_," ",i.loc_)
                    s += 'R' + str(i.uid_) + ' + '
                print(s)

                print("End   op : ",end_log_entry.op_type_, "|",end_log_entry.operand_,"|", end_log_entry.location_, "|", end_log_entry.tsc_)
                print("Acquiring window : ")
                s = '1 <= '
                for log_entry in acq_log_list:
                    i = Variable.acquire_var(log_entry)
                    print(i.description_," ",i.loc_)
                    s += 'A' + str(i.uid_) + ' + '
                print(s)
                print()
                #'''
        #print("near-miss count ", nm_cnt)
    return cs
