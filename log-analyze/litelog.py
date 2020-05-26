#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from typing import List, Dict
import bisect
import re 

import numpy as np
import time

from APISpecification import APISpecification


class LogEntry():
    map_api_entry: Dict[str, Dict[str,int]]  = {}
    map_api_timegap: Dict[str, List[int]] = {}
    objid_to_int: Dict[str, int] = {}
    @staticmethod
    def parse(line: str) -> 'LogEntry':
        tup = line.strip().split('|')
        return LogEntry(tup)
    @classmethod
    def get_objid(cls, objid: str):
        if objid not in cls.objid_to_int:
            cls.objid_to_int[objid] = len(cls.objid_to_int)
        return cls.objid_to_int[objid]

    def __init__(self, tup):
        if len(tup) != 6:
            for t in tup:
                print(t)
        assert len(tup) == 6

        #start = time.time()
        self.tsc_ = int(tup[0].strip())
        objid = tup[1].strip()
        #self.object_id_ = tup[1].strip()
        self.object_id_ = LogEntry.get_objid(objid)
        self.op_type_ = tup[2].strip()
        self.operand_ = self.shrink_name(tup[3].strip())
        self.is_write_ = self.op_type_ == "Write" or (self.op_type_ == "Call" and APISpecification.Is_Write_API(self.operand_) )
        self.is_read_  = self.op_type_ == "Read"  or (self.op_type_ == "Call" and APISpecification.Is_Read_API( self.operand_) )
        
        #if self.is_write_ and self.op_type_ == "Call":
        #    print("Setting an api to write obj ", self.object_id_, " ts ", self.tsc_)

        #end = time.time()
        #print("assign field time",end - start)

        start = time.time()
        #if '`1' in self.operand_ :
        #    self.operand_ = self.operand_.replace('`1','')
        #if '`2' in self.operand_ :
        #    self.operand_ = self.operand_.replace('`2','')
        #start = time.time()
        self.location_ = tup[4].strip()
        self.time_gap_ = int(tup[5].strip())
        self.thread_id_ = -1  # Fixed after log is loaded
        
        self.in_window_ = False
        
        self.description_ = self.op_type_ + "|" + self.operand_ 
        #end = time.time()
        #print("concatanate string time",end - start)

        #start = time.time()
        if self.description_ not in LogEntry.map_api_entry:
            LogEntry.map_api_entry[self.description_] = {}
        if self.location_ not in LogEntry.map_api_entry[self.description_]:
            LogEntry.map_api_entry[self.description_][self.location_] =1
        else:
            LogEntry.map_api_entry[self.description_][self.location_] +=1
        
        if self.description_ not in LogEntry.map_api_timegap:
            LogEntry.map_api_timegap[self.description_] = []
        LogEntry.map_api_timegap[self.description_].append(self.time_gap_)
        #end = time.time()
        #print("looking dict time",end - start)

    def shrink_name(self, s: str):
        t = s.replace('`1','')
        t = t.replace('`2','')
        t = re.sub('<.*?>','',t)
        return t;
    def __str__(self):
        s = (f'Tsc: {self.tsc_}',
             f'ThreadID: {self.thread_id_}',
             f'Object ID: {self.object_id_}',
             f'Op type: {self.op_type_}',
             f'Operand: {self.operand_}',
             f'Location: {self.location_}',
             )
        return '\n'.join(s)

    def is_write(self) -> bool:
        return self.is_write_ 

    def is_read(self) -> bool:
        return self.is_read_ 

    def is_candidate(self) -> bool:
        #if self.op_type_.lower() != 'call':
        #    return False

        if '::.ctor' in self.operand_ and 'Call' in self.op_type_ :
            return False

        #if 'k__BackingField' in self.operand_:
        #    return False
        
        if '::get_' in self.operand_ and 'Call' in self.op_type_:
            return False

        if '::set_' in self.operand_ and 'Call' in self.op_type_:
            return False 


        #if '::get_' in self.operand_ and 'Call' in self.op_type_ and 'Begin' in self.operand_:
        #    return False     
        #if '::get_' in self.operand_ and 'Call' in self.op_type_ and 'End' in self.operand_:
        #    return False
        #if '::set_' in self.operand_ and 'Call' in self.op_type_ and 'Begin' in self.operand_:
        #    return False
        #if '::set_' in self.operand_ and 'Call' in self.op_type_ and 'End' in self.operand_:
        #    return False
        
        return True

    def is_conflict(self, another: 'LogEntry') -> bool:
        if ((self.thread_id_ != another.thread_id_) and
            (self.is_write() or another.is_write())):
            return True

        return False

    #
    # A trick to exploit the binary search
    # because bisect does not support customized comparison directly
    #
    class TscCompare:
        def __init__(self, tsc):
            self.tsc_ = tsc

        def __lt__(self, other: 'LogEntry'):
            return self.tsc_ < other.tsc_

    def __lt__(self, other: 'TscCompare'):
        return self.tsc_ < other.tsc_


class LiteLog:

    @staticmethod
    def load_log(logpath: str) -> 'LiteLog':
        
        #start = time.time()

        log = LiteLog() 
        my_list = []
        with open(logpath) as fd:
            for line in fd:
                entry = LogEntry.parse(line)
                my_list.append(entry)
        
        #my_list.sort(key = lambda x: x.tsc_)

        log.log_list_ = np.array(my_list)
        #end = time.time()
        #ave = (end-start)/len(log.log_list_)
        #print(logpath, " loading time ", end-start, " ave time ", ave)
        return log

    #def __init__(self):
    #   self.log_list_ = np.array()

    def __iter__(self):
        return iter(self.log_list_)

    def __getitem__(self, index: int):
        return self.log_list_[index]

    def __len__(self):
        return len(self.log_list_)

    def append(self, log_entry: LogEntry):
        self.log_list_.append(log_entry)

    def range_by(self, start_tsc: int, end_tsc: int, left_one_more, right_one_less) -> 'LiteLog':
        
        '''
        Find log entries whose tsc: start_tsc < tsc < end_tsc
        When left_one_more is True, add one more log whose tsc may be less then start_tsc
        '''
        #start = time.time()
        left_key = LogEntry.TscCompare(start_tsc)
        right_key = LogEntry.TscCompare(end_tsc)

        left_index = bisect.bisect_right(self.log_list_, left_key)
        right_index = bisect.bisect_left(self.log_list_, right_key)


        if left_one_more:
            if left_index > 0:
                left_index -= 1
        
        if right_one_less:
            if right_index > left_index:
                right_index -= 1

        log = LiteLog()
        log.log_list_ =  self.log_list_[left_index: right_index]

        #end = time.time()
        #print("One time range search time",end - start)
        return log


if __name__ == "__main__":
    log = LiteLog.load_log('outputs/outputs/1.litelog')
    for x in log:
        print(x)
        print()

    print("===============")

    for x in log.range_by(637207729813661304, 637207729814207440):
        print(x)
        print()
