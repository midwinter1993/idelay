from typing import List, Dict, Set, Tuple
from litelog import LogEntry
import sys
from lp_staff import LpBuilder, Variable, VariableList
from flipy import LpVariable
from constant_pool import ConstantPool
import flipy


# LocationId = int

class SyncVariable(Variable):
    variable_pool: Dict[str, 'SyncVariable'] = {}

    def __init__(self, uid: int, op_type: str, op_target: str):
        super().__init__(uid)
        self.op_type_ = op_type
        self.op_target_ = op_target

        #
        # We amplify the probability as integer values in [0, 100]
        #
        self.lp_rel_var_ = LpBuilder.var(self.as_str_rel(), up_bound=100)
        self.lp_acq_var_ = LpBuilder.var(self.as_str_acq(), up_bound=100)

        #
        # Used by heuristic
        #
        self.is_rel_ = False
        self.is_acq_ = False

    def is_read(self) -> bool:
        return self.op_type_ == 'R'

    def is_write(self) -> bool:
        return self.op_type_ == 'W'

    def is_enter(self) -> bool:
        return self.op_type_ == 'Enter'

    def is_exit(self) -> bool:
        return self.op_type_ == 'Exit'

    def is_monitor_enter(self) -> bool:
        return self.op_target_ == 'Monitor' and self.is_enter()

    def is_monitor_exit(self) -> bool:
        return self.op_target_ == 'Monitor' and self.is_exit()

    def as_str_rel(self) -> str:
        return f'R{self.uid_}'

    def as_str_acq(self) -> str:
        return f'A{self.uid_}'

    def as_lp_rel(self) -> LpVariable:
        return self.lp_rel_var_

    def as_lp_acq(self) -> LpVariable:
        return self.lp_acq_var_

    def is_marked_acq(self) -> bool:
        return self.is_acq_

    def is_marked_rel(self) -> bool:
        return self.is_rel_

    def mark_as_acq(self):
        assert not self.is_rel_
        self.is_acq_ = True

    def mark_as_rel(self):
        assert not self.is_acq_
        self.is_rel_ = True

    @classmethod
    def get_variable(cls, op_type: str, op_target: str) -> 'SyncVariable':
        key = f'{op_type}:{op_target}'
        if key not in cls.variable_pool:
            cls.variable_pool[key] = SyncVariable(len(cls.variable_pool), op_type, op_target)
        return cls.variable_pool[key]

    @classmethod
    def release_var(cls, log_entry: LogEntry) -> 'SyncVariable':
        return cls.get_variable(log_entry.op_type_, log_entry.operand_)

    @classmethod
    def acquire_var(cls, log_entry: LogEntry) -> 'SyncVariable':
        return cls.get_variable(log_entry.op_type_, log_entry.operand_)


class SyncConstraintSystem:
    def __init__(self):
        self.rel_constraints_: Set[VariableList] = set()
        self.acq_constraints_: Set[VariableList] = set()

    def add_release_constraint(self, var_set: List[Variable]):
        if len(var_set):
            self.rel_constraints_.add(VariableList(var_set))

    def add_acquire_constraint(self, var_set: List[Variable]):
        if len(var_set):
            self.acq_constraints_.add(VariableList(var_set))

    def print_system(self):
        print("Variable Definition")
        for op, var in SyncVariable.variable_pool.items() :
            print(f'   {op}  {var.uid_}')

    def _lp_encode_rel(self):
        for constraint in self.rel_constraints_:
            lp_var_list = [var.as_lp_rel() for var in constraint if not var.is_marked_acq()]

            if not lp_var_list:
                continue

            #
            # There is only one release operation
            #
            penalty = LpBuilder.var(f'Penalty{len(self.penalty_vars_)}', up_bound=50)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)

            self.prob_.add_constraint(LpBuilder.constraint_sum_geq(lp_var_list, 100))
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_acq(self):
        for constraint in self.acq_constraints_:
            lp_var_list = [var.as_lp_acq() for var in constraint if not var.is_marked_rel()]

            if not lp_var_list:
                continue

            #
            # There is only one acquire operation
            #
            penalty = LpBuilder.var(f'Penalty{len(self.penalty_vars_)}', up_bound=50)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)
            self.prob_.add_constraint(LpBuilder.constraint_sum_geq(lp_var_list, 100))
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_all_vars(self):
        #
        # For each variable/location, P_rel + P_acq < 100
        #
        for var in SyncVariable.variable_pool.values():
            self.prob_.add_constraint(LpBuilder.constraint_sum_leq([var.as_lp_acq(), var.as_lp_rel()], 100))

    def _lp_encode_all_vars_heuristic(self):
        for var in SyncVariable.variable_pool.values():
            if var.is_monitor_enter():
                var.mark_as_acq()
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_acq()], 100))
            elif var.is_monitor_exit():
                var.mark_as_rel()
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_rel()], 100))
            elif var.is_read():
                var.mark_as_acq()
            elif var.is_write():
                var.mark_as_rel()

    def _lp_encode_object_func(self):
        #
        # Object function: min(penalty + all variables)
        #
        obj_func_vars = self.penalty_vars_

        for var in SyncVariable.variable_pool.values():
            obj_func_vars.append(var.as_lp_acq())
            obj_func_vars.append(var.as_lp_rel())

        obj = flipy.LpObjective(expression={v: 1 for v in obj_func_vars}, sense=flipy.Minimize)

        self.prob_.set_objective(obj)

    def lp_solve(self):
        self.prob_ = LpBuilder.problem("HB_Infer")
        self.penalty_vars_ = []

        #
        # Heuristic must be encoded first
        #
        self._lp_encode_all_vars_heuristic()

        self._lp_encode_rel()
        self._lp_encode_acq()
        self._lp_encode_all_vars()
        self._lp_encode_object_func()

        solver = flipy.CBCSolver()
        status = solver.solve(self.prob_)

        print('Solving Status:', status)

    def print_result(self, cp: ConstantPool):
        print("Releasing sites :")
        for op, var in SyncVariable.variable_pool.items():
            if (var.as_lp_rel().evaluate() >= 95):
                print(f'{op} {cp.get_str(var.op_target_)} => {var.as_str_rel()}')

        print("Acquiring sites :")
        for op, var in SyncVariable.variable_pool.items():
            if (var.as_lp_acq().evaluate() >= 95):
                print(f'{op} {cp.get_str(var.op_target_)} => {var.as_str_acq()}')

        self.prob_.write_lp(open('./problem.lp', 'w'))

    def near_miss_encode(self, log_pool):
        thread_log_dict = log_pool.get_thread_log_dict()
        obj_log_dict = log_pool.get_obj_log_dict()

        for log in obj_log_dict.values():
            for idx, end_log_entry in enumerate(log):
                for j in range(idx - 1, -1, -1):
                    start_log_entry = log[j]

                    if not end_log_entry.is_close(start_log_entry):
                        break
                    if not start_log_entry.is_conflict(end_log_entry):
                        continue

                    start_tsc, end_tsc = start_log_entry.tsc_, end_log_entry.tsc_

                    self._encode_sync_in_window(thread_log_dict[start_log_entry.thread_id_],
                                                thread_log_dict[end_log_entry.thread_id_],
                                                start_tsc,
                                                end_tsc)

    def _encode_sync_in_window(self, thread_log_1, thread_log_2, start_tsc, end_tsc):
        #
        # We just encode constraints for call operations now
        #
        rel_var_list = [
            SyncVariable.release_var(log_entry)
            for log_entry in thread_log_1. range_by(start_tsc, end_tsc)
            if log_entry.is_call()
        ]
        self.add_release_constraint(rel_var_list)

        #
        # For acquiring sites, implementing window + 1
        # Add a log whose tsc < start_tsc
        #
        acq_var_list = [
            SyncVariable.acquire_var(log_entry)
            for log_entry in thread_log_2. range_by(start_tsc, end_tsc, left_one_more=True)
            if log_entry.is_call()
        ]

        self.add_acquire_constraint(acq_var_list)
