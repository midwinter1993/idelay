#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
from litelog import LiteLog, LogPool
from sync_constraint import SyncVariable, SyncConstraintSystem
from typing import Dict
from hb_constraint import HbVariable, HbConstraintSystem




if __name__ == "__main__":

    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--dir', help='the log directory')
    args = dirparser.parse_args()

    print(args.dir)
    log_dir = args.dir

    log_pool = LogPool(log_dir)

    #
    # Search the nearmiss
    # TODO: paralleled
    #
    constraints = SyncConstraintSystem()
    constraints.near_miss_encode(log_pool)

    # constraints = HbConstraintSystem()
    # near_miss_hb_encode(constraints, thread_log, obj_id_log)

    constraints.print_system()
    print('===== LP solving =====')
    constraints.lp_solve()
    #print('===== Gurobi solving =====')
    #constrains.gurobi_solve()
