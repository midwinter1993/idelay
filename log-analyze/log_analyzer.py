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

    log_dir = args.dir

    log_pool = LogPool(log_dir)

    #
    # Search the nearmiss
    # TODO: paralleled
    #
    constraints = SyncConstraintSystem()

    print(f'{Color.GREEN}>>> Encode...{Color.END}')
    # for obj, log in log_pool.get_obj_log_dict().items():
        # print(obj, len(log))
    # import sys
    # sys.exit(0)

    constraints.lp_encode(log_pool)

    # constraints = HbConstraintSystem()
    # near_miss_hb_encode(constraints, thread_log, obj_id_log)

    print(f'{Color.GREEN}>>> Solve LP...{Color.END}')
    constraints.lp_solve()

    print(f'{Color.GREEN}>>> Load Constant Pool...{Color.END}')
    cp = ConstantPool(os.path.join(log_dir, 'map.cp'))
    # cp.dump()

    print(f'{Color.GREEN}>>> Print Results...{Color.END}')
    constraints.print_result(cp)

    print(f'{Color.GREEN}>>> Save Info...{Color.END}')
    constraints.save_info(cp)

    #print('===== Gurobi solving =====')
    #constrains.gurobi_solve()
