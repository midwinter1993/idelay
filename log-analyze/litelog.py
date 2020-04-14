#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from typing import List
import bisect


class LogEntry():
    @staticmethod
    def parse(line: str) -> 'LogEntry':
        tup = line.strip().split('|')
        return LogEntry(tup)

    def __init__(self, tup):
        if len(tup) != 5:
            for t in tup:
                print(t)
        assert len(tup) == 5

        self.tsc_ = int(tup[0].strip())
        self.object_id_ = tup[1].strip()
        self.op_type_ = tup[2].strip()
        self.operand_ = tup[3].strip()
        self.location_ = tup[4].strip()
        self.thread_id_ = -1  # Fixed after log is loaded

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
        return self.op_type_ == "Write"

    def is_call(self) -> bool:
        return self.op_type_.lower() == 'call'

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
        log = LiteLog()
        
        with open(logpath) as fd:
            for line in fd:
                log.log_list_.append(LogEntry.parse(line))

        log.log_list_.sort(key=lambda x: x.tsc_)

        return log

    def __init__(self):
        self.log_list_ = []

    def __iter__(self):
        return iter(self.log_list_)

    def __getitem__(self, index: int):
        return self.log_list_[index]

    def __len__(self):
        return len(self.log_list_)

    def append(self, log_entry: LogEntry):
        self.log_list_.append(log_entry)

    def range_by(self, start_tsc: int, end_tsc: int, left_one_more=False) -> 'LiteLog':
        '''
        Find log entries whose tsc: start_tsc < tsc < end_tsc
        When left_one_more is True, add one more log whose tsc may be less then start_tsc
        '''
        left_key = LogEntry.TscCompare(start_tsc)
        right_key = LogEntry.TscCompare(end_tsc)

        left_index = bisect.bisect_right(self.log_list_, left_key)
        right_index = bisect.bisect_left(self.log_list_, right_key)

        n = len(self.log_list_)
        l_index2 = n
        r_index2 = 0
        for i in range(n):
            if self.log_list_[i].tsc_ >= end_tsc:
                r_index2 = i
                break

        for i in range(n-1, -1, -1):
            if self.log_list_[i].tsc_ <= start_tsc:
                print("log[",i,"].tsc_ = ",self.log_list_[i].tsc_, " <= ", start_tsc)
                l_index2 = i + 1
                break
        
        if l_index2 == n :
            l_index2 = 0

        if left_one_more:
            if left_index > 0:
                left_index -= 1
            if l_index2 > 0 :
                l_index2 -= 1

        if left_index != l_index2 or right_index != r_index2 :
            print()
            print("Conflicting searaching result: ")
            print("Binary search :",left_index ," -> ", right_index)
            print("Naive search :",l_index2 , " -> ", r_index2)
            print()
        else:
            print()
            print("Binary search :",left_index ," -> ", right_index)
            print()

        log = LiteLog()
        log.log_list_ =  self.log_list_[left_index: right_index]
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
