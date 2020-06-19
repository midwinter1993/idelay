#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import argparse
import os
from constant_pool import ConstantPool


if __name__ == "__main__":
    dirparser = argparse.ArgumentParser()
    dirparser.add_argument('--dir', help='the log directory')
    args = dirparser.parse_args()

    log_dir = args.dir

    cp = ConstantPool(os.path.join(log_dir, 'map.cp'))
    print(cp.get_str(16))