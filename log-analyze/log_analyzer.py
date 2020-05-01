#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
from litelog import LiteLog, LogPool
from sync_constraint import SyncVariable, SyncConstraintSystem
from typing import Dict
from hb_constraint import HbVariable, HbConstraintSystem
from constant_pool import ConstantPool
from color import Color


if __name__ == "__main__":

    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--dir', help='the log directory')
    args = dirparser.parse_args()

    print(args.dir)
    log_dir = args.dir

    print(f'{Color.GREEN}>>> Log Loading...{Color.END}')
    log_pool = LogPool(log_dir)

    #
    # Search the nearmiss
    # TODO: paralleled
    #
    constraints = SyncConstraintSystem()

    print(f'{Color.GREEN}>>> Near-miss Encoding...{Color.END}')
    constraints.near_miss_encode(log_pool)

    # constraints = HbConstraintSystem()
    # near_miss_hb_encode(constraints, thread_log, obj_id_log)

    constraints.print_system()
    print(f'{Color.GREEN}>>> LP Solving...{Color.END}')
    constraints.lp_solve()

    print(f'{Color.GREEN}>>> Constant Pool Loading...{Color.END}')
    cp = ConstantPool(os.path.join(log_dir, 'map.cp'))
    # cp.dump()
    constraints.print_result(cp)

    #print('===== Gurobi solving =====')
    #constrains.gurobi_solve()
