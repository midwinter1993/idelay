#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from typing import List, Dict, Iterator
from collections import defaultdict
import bisect
import re
import os


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

        self.tsc_: int = int(tup[0].strip())
        self.object_id_: int = int(tup[1].strip())
        self.op_type_: str = tup[2].strip()

        self.operand_: int = int(tup[3].strip())
        # if '`1' in self.operand_ :
        # self.operand_ = self.operand_.replace('`1','')
        # if '`2' in self.operand_ :
        # self.operand_ = self.operand_.replace('`2','')
        # self.operand_  = re.sub('<.*?>','',self.operand_)

        self.location_ = tup[4].strip()
        self.thread_: int = -1  # Fixed after log is loaded

    def __str__(self):
        s = (f'[ Tsc ]: {self.tsc_}',
             f'[ ThreadID ]: {self.thread_}',
             f'[ Object ID ]: {self.object_id_}',
             f'[ Op type ]: {self.op_type_}',
             f'[ Operand]: {self.operand_}',
             f'[ Location ]: {self.location_}',
             )
        return '\n'.join(s)

    def get_operation_str(self) -> str:
        return f'{self.op_type_}:{self.operand_}'

    def get_operand(self) -> int:
        return self.operand_

    def get_op_type(self) -> str:
        return self.op_type_

    def is_write(self) -> bool:
        return self.op_type_ == 'W'

    def is_read(self) -> bool:
        return self.op_type_ == 'R'

    def is_enter(self) -> bool:
        return self.op_type_ == 'Enter'

    def is_exit(self) -> bool:
        return self.op_type_ == 'Exit'

    def is_monitor_enter(self) -> bool:
        return False
        # return self.operand_ == 'Monitor' and self.is_enter()

    def is_monitor_exit(self) -> bool:
        return False
        # return self.operand_ == 'Monitor' and self.is_exit()

    def is_conflict(self, another: 'LogEntry') -> bool:
        # There must be a write operation
        if not (self.is_write() or another.is_write()):
            return False

        # From different threads
        if self.thread_ == another.thread_:
            return False

        # Access the same object
        if self.object_id_ != another.object_id_:
            return False

        # Access the same field
        if self.operand_ != another.operand_:
            return False

        return True

    # def operand_class_name(self):
        # return self.operand_.split('::')[0]

    # def operand_method_name(self):
        # return self.operand_.split('::')[1]

    def is_close(self, another: 'LogEntry') -> bool:
        DISTANCE = 10000000

        x, y = self.tsc_, another.tsc_

        if x > y:
            x, y = y, x

        return y < x + DISTANCE

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

        if left_one_more:
            if left_index > 0:
                left_index -= 1

        log = LiteLog()
        log.log_list_ = self.log_list_[left_index: right_index]
        return log

    def sort(self):
        self.log_list_.sort(key=lambda log_entry: log_entry.tsc_)


class LogPool:
    def __init__(self, log_dir: str):
        self._load(log_dir)
        self._organize_by_obj()

    def _load(self, log_dir: str):
        log_files = [f for f in os.listdir(log_dir) if f.endswith(".litelog")]
        print(f'  |_ Found log files size : {len(log_files)}')

        #
        # Load the lite log by thread ID
        # TODO: paralleled
        #
        self.thread_log_dict_: Dict[int, LiteLog] = {
            int(os.path.splitext(log_name)[0]): LiteLog.load_log(
                os.path.join(log_dir, log_name))
            for log_name in log_files
        }

        #
        # Patch thread id for each log entry
        #
        for thread, log in self.thread_log_dict_.items():
            for log_entry in log:
                log_entry.thread_ = thread
            print(f'  |_ Thread {thread:2}; log size: {len(log)}')

    def _organize_by_obj(self):
        self.obj_log_dict_: Dict[int, LiteLog] = defaultdict(LiteLog)

        for log in self.thread_log_dict_.values():
            for log_entry in log:
                if log_entry.is_read() or log_entry.is_write():
                    self.obj_log_dict_[log_entry.object_id_].append(log_entry)

        for obj in self.obj_log_dict_:
            self.obj_log_dict_[obj].sort()

    def get_thread_log_dict(self) -> Dict[int, LiteLog]:
        return self.thread_log_dict_

    def get_obj_log_dict(self) -> Dict[int, LiteLog]:
        return self.obj_log_dict_

    def ordered_entries(self) -> Iterator[LogEntry]:
        thread_idx_dict = {thread: 0 for thread in self.thread_log_dict_}

        while thread_idx_dict:
            thread, idx = min(
                thread_idx_dict.items(),
                key=lambda thd_idx: self.thread_log_dict_[thd_idx[0]][thd_idx[1]].tsc_
            )
            if idx == len(self.thread_log_dict_[thread])-1:
                del thread_idx_dict[thread]
            else:
                thread_idx_dict[thread] = idx + 1

            yield self.thread_log_dict_[thread][idx]
