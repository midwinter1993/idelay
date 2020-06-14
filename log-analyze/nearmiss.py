
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
        obj_id_log[obj_id].sort(key=lambda log_entry: log_entry.start_tsc_)

    return obj_id_log, obj_id_threadlist

def close_enough(x, y):
    DISTANCE = 10000000

    if x > y:
        x, y = y, x

    return y < x + DISTANCE

def near_miss_encode(cs, thread_log, obj_id_log, obj_id_threadlist):

    klen = 5
    near_miss_dict = {}

    #if LogEntry.objid_to_int['null'] in obj_id_log:
        #print("delete null key when filtering nearmiss")
        #del obj_id_log[LogEntry.objid_to_int['null']]

    progress = 0
    for objid in obj_id_log:

        progress = progress + 1
        log = obj_id_log[objid]

        if len(log) < 2 or LogEntry.int_to_objid[objid] == 'null':
            continue
        #print('processing ', progress,'/', len(obj_id_log),' objid ', LogEntry.int_to_objid[objid])

        ex_entry = log[0]
        if len(obj_id_threadlist[ex_entry.object_id_]) < 2:
            continue
        #print('processing ', progress,'/', len(obj_id_log), " thread size ", len(obj_id_threadlist[ex_entry.object_id_]))

        for idx, end_log_entry in enumerate(log):

            for j in range(idx - 1, max(idx - klen-1,-1), -1):
            #for j in range(idx - 1, -1, -1):
                start_log_entry = log[j]

                start_tsc, end_tsc = start_log_entry.finish_tsc_, end_log_entry.start_tsc_

                if not close_enough(start_tsc, end_tsc):
                    break

                if not start_log_entry.is_conflict(end_log_entry):
                    continue

                # very important here. dramatically reduce the overhead.
                sig = start_log_entry.description_ + "!" + end_log_entry.description_

                if sig in near_miss_dict and near_miss_dict[sig] > 10:
                    continue

                if sig not in near_miss_dict:
                    near_miss_dict[sig] = 0

                near_miss_dict[sig] += 1

                #
                # We just encode constraints for call operations now
                #
                rel_log_original_list = [
                #rel_log_list = [
                    log_entry
                    for log_entry in thread_log[start_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc, ltsc = True)
                    if log_entry.is_candidate()
                ]

                acq_log_list = [
                    log_entry
                    for log_entry in thread_log[end_log_entry.thread_id_].
                    range_by(start_tsc, end_tsc, ltsc = False)
                    if log_entry.is_candidate()
                ]

                acq_sleep_log_list = [log_entry for log_entry in acq_log_list if log_entry.is_sleep_]

                rel_log_list = rel_log_original_list
                #sleep refine algorithm here


                domi_sleep_entry = None
                index = -1
                confirmed = False
                #'''
                if  len(acq_sleep_log_list) == 0:

                    for i in range(len(rel_log_original_list)-1):
                        log_entry = rel_log_original_list[i]
                        if log_entry.is_sleep_ and log_entry.finish_tsc_ < end_tsc:
                            index = i
                            domi_sleep_entry = log_entry

                    if index > -1:
                        #print("Refine a original constraint")
                        rel_log_list = rel_log_original_list[index+1:]
                        print("Refine a constraint by sleep before " + rel_log_list[0].description_)
                        rel_nonsleep_log_list = [i for i in rel_log_list if not i.is_sleep_]
                        #if len(rel_nonsleep_log_list) == 1:
                        #    print("Confirm releasing", rel_nonsleep_log_list[0].description_)
                        start_tsc = domi_sleep_entry.finish_tsc_
                        acq_log_list = [
                            log_entry
                            for log_entry in thread_log[end_log_entry.thread_id_].
                            range_by(start_tsc, end_tsc, ltsc = False)
                            if log_entry.is_candidate()
                        ]

                        if len(rel_log_list) == 2 and not rel_log_list[0].is_sleep_ and rel_log_list[1].is_sleep_ and rel_log_list[1].finish_tsc_> end_tsc:
                            confirmed = True
                            Variable.release_var(rel_nonsleep_log_list[0]).set_confirmation()
                            print("Confirm releasing", rel_nonsleep_log_list[0].description_)
                            if len(acq_log_list) > 0:
                                Variable.acquire_var(acq_log_list[0]).set_confirmation()
                            #Variable.variable_pool[acq_log_list[0].description_].set_confirmation()
                    # end of sleep refine algorithm
                    #'''
                cs.add_constraint(rel_log_list, acq_log_list, objid)


                #for debugging
                #'''
                #if index > -1 and len(acq_sleep_log_list) == 0:
                if confirmed:
                    print()
                    print("Find a nearmiss : ")

                    print("Start op : ",start_log_entry.op_type_,"|",start_log_entry.operand_,"|",start_log_entry.location_, "|", start_log_entry.start_tsc_)
                    print("Releasing window : ")
                    s = '1 <= '
                    for log_entry in rel_log_list:
                        i = Variable.release_var(log_entry)
                        print(i.description_," ",i.loc_)
                        s += 'R' + str(i.uid_) + ' + '
                    print(s)

                    print("End   op : ",end_log_entry.op_type_, "|",end_log_entry.operand_,"|", end_log_entry.location_, "|", end_log_entry.start_tsc_)
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
