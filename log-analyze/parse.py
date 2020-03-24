#!/usr/bin/env python3
# -*- coding: utf-8 -*-


from typing import List


class LiteLog():
    @staticmethod
    def parse(line: str):
        tup = line.strip().split(',')
        return LiteLog(tup)

    def __init__(self, tup):
        if len(tup) != 6:
            # print(len(tup))
            for t in tup:
                print(t)
        assert len(tup) == 6 # Contain thread id
        self.tsc_ = int(tup[0].strip())
        self.thread_id_ = tup[1].strip()
        self.op_type_ = tup[2].strip()
        self.operand_ = tup[3].strip()
        self.obj_id_ = tup[4].strip()
        self.location_  = tup[5].strip()


def load_log(logpath: str) -> List[LiteLog]:
    log_list = []
    with open(logpath) as fd:
        for line in fd:
            log_list.append(LiteLog.parse(line))


if __name__ == "__main__":
    load_log('./debug.litelog')