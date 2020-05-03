from typing import List, Dict, Set, Tuple
from litelog import LogEntry, LogPool
from lp_staff import LpBuilder, Variable, VariableList
from flipy import LpVariable
from constant_pool import ConstantPool
from color import Color
import sys
import flipy


# LocationId = int

class SyncVariable(Variable):
    '''
    The SyncVariable represents the probability for each operation.

    The operation may be `Call a method`, `Read/Write a field`
    at the source code level.

    It contains a list that represents which LogEntries are instances
    of these operations.
    '''
    variable_pool: Dict[str, 'SyncVariable'] = {}

    def __init__(self, uid: int):
        super().__init__(uid)
        self.log_entry_list_: List[LogEntry] = []

        #
        # We amplify the probability as integer values in [0, 100]
        #
        self.lp_rel_var_ = LpBuilder.var(self.as_str_rel(), up_bound=100)
        self.lp_acq_var_ = LpBuilder.var(self.as_str_acq(), up_bound=100)

        #
        # When the probability > threshold_, acq/rel is True
        #
        self.threshold_ = 95

        #
        # Used by heuristic
        #
        self.is_rel_ = False
        self.is_acq_ = False

    def get_operand(self) -> int:
        return self.log_entry_list_[0].get_operand()

    def get_op_type(self) -> str:
        return self.log_entry_list_[0].get_op_type()

    def get_threshold(self) -> int:
        return self.threshold_

    def is_read(self) -> bool:
        return self.log_entry_list_[0].is_read()

    def is_write(self) -> bool:
        return self.log_entry_list_[0].is_write()

    def is_enter(self) -> bool:
        return self.log_entry_list_[0].is_enter()

    def is_exit(self) -> bool:
        return self.log_entry_list_[0].is_exit()

    def is_monitor_enter(self) -> bool:
        return self.log_entry_list_[0].is_monitor_enter()

    def is_monitor_exit(self) -> bool:
        return self.log_entry_list_[0].is_monitor_exit()

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

    def complete_str(self, cp: ConstantPool) -> str:
        return f'{self.get_op_type()} {cp.get_str(self.get_operand())}'

    @classmethod
    def get_variable(cls, log_entry: LogEntry) -> 'SyncVariable':
        key = log_entry.get_operation_str()

        if key not in cls.variable_pool:
            cls.variable_pool[key] = SyncVariable(len(cls.variable_pool))

        var = cls.variable_pool[key]
        var.log_entry_list_.append(log_entry)
        return var

    @classmethod
    def release_var(cls, log_entry: LogEntry) -> 'SyncVariable':
        return cls.get_variable(log_entry)

    @classmethod
    def acquire_var(cls, log_entry: LogEntry) -> 'SyncVariable':
        return cls.get_variable(log_entry)


class SyncConstraintSystem:
    def __init__(self):
        self.rel_constraints_: Set[VariableList] = set()
        self.acq_constraints_: Set[VariableList] = set()

    def _lp_encode_rel(self):
        for constraint in self.rel_constraints_:
            lp_var_list = [var.as_lp_rel()
                           for var in constraint if not var.is_marked_acq()]

            if not lp_var_list:
                continue

            #
            # There is only one release operation
            #
            penalty = LpBuilder.var(
                f'Penalty{len(self.penalty_vars_)}', up_bound=50)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)

            self.prob_.add_constraint(
                LpBuilder.constraint_sum_geq(lp_var_list, 100))
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_acq(self):
        for constraint in self.acq_constraints_:
            lp_var_list = [var.as_lp_acq()
                           for var in constraint if not var.is_marked_rel()]

            if not lp_var_list:
                continue

            #
            # There is only one acquire operation
            #
            penalty = LpBuilder.var(
                f'Penalty{len(self.penalty_vars_)}', up_bound=50)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)
            self.prob_.add_constraint(
                LpBuilder.constraint_sum_geq(lp_var_list, 100))
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_all_vars(self):
        #
        # For each variable/location, P_rel + P_acq < 100
        #
        for var in SyncVariable.variable_pool.values():
            self.prob_.add_constraint(LpBuilder.constraint_sum_leq(
                [var.as_lp_acq(), var.as_lp_rel()], 100))

    def _lp_encode_all_vars_heuristic(self):
        for var in SyncVariable.variable_pool.values():
            if var.is_monitor_enter():
                var.mark_as_acq()
                self.prob_.add_constraint(
                    LpBuilder.constraint_sum_eq([var.as_lp_acq()], 100))
            elif var.is_monitor_exit():
                var.mark_as_rel()
                self.prob_.add_constraint(
                    LpBuilder.constraint_sum_eq([var.as_lp_rel()], 100))
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

        obj = flipy.LpObjective(
            expression={v: 1 for v in obj_func_vars}, sense=flipy.Minimize)

        self.prob_.set_objective(obj)

    def lp_encode(self, log_pool: LogPool):
        self.prob_ = LpBuilder.problem("Sync_Infer")
        self.penalty_vars_: List[LpVariable] = []

        # self._near_miss_encode(log_pool)
        self._data_race_encode(log_pool)

        #
        # Heuristic must be encoded first
        #
        self._lp_encode_all_vars_heuristic()

        self._lp_encode_rel()
        self._lp_encode_acq()
        self._lp_encode_all_vars()
        self._lp_encode_object_func()

    def lp_solve(self):
        solver = flipy.CBCSolver()
        status = solver.solve(self.prob_)

        print('  |_ Solving Status:', status)

    def print_result(self, cp: ConstantPool):
        print(f'{Color.BLUE}--- Release Operation ---{Color.END}')
        for op, var in SyncVariable.variable_pool.items():
            if (var.as_lp_rel().evaluate() >= var.get_threshold()):
                print(f'{var.complete_str(cp)} => {var.as_str_rel()}')

        print(f'{Color.BLUE}--- Acquire Operations ---{Color.END}')
        for op, var in SyncVariable.variable_pool.items():
            if (var.as_lp_acq().evaluate() >= var.get_threshold()):
                print(f'{var.complete_str(cp)} => {var.as_str_acq()}')

    def save_info(self, cp: ConstantPool):
        print(f'  |_ ./syncvar.def')
        print(f'  |_ ./problem.lp')

        with open('./syncvar.def', 'w') as fd:
            fd.write('Sync Variable Definition\n===\n')

            for _, var in SyncVariable.variable_pool.items():
                fd.write(f'{var.complete_str(cp)} => {var.uid_}\n')

        self.prob_.write_lp(open('./problem.lp', 'w'))

    def _data_race_encode(self, log_pool: LogPool):
        print(f'  |_ By Data-race')
        nr_window = 0

        thread_log_dict = log_pool.get_thread_log_dict()
        obj_last_entry_dict: Dict[int, LogEntry] = {}

        for log_entry in log_pool.ordered_entries():
            if not (log_entry.is_read() or log_entry.is_write()):
                continue

            obj = log_entry.object_id_
            last_entry = obj_last_entry_dict.get(obj, None)
            if not last_entry:
                obj_last_entry_dict[obj] = log_entry
                continue

            if last_entry.is_close(log_entry) and last_entry.is_conflict(log_entry):
                start_tsc, end_tsc = last_entry.tsc_, log_entry.tsc_

                nr_window += 1
                self._encode_sync_in_window(thread_log_dict[last_entry.thread_],
                                            thread_log_dict[log_entry.thread_],
                                            start_tsc,
                                            end_tsc)

            obj_last_entry_dict[obj] = log_entry
        print(f'  |_ #Window: {nr_window}')

    def _near_miss_encode(self, log_pool: LogPool):
        print(f'  |_ By Near-miss')
        nr_window = 0

        thread_log_dict = log_pool.get_thread_log_dict()
        obj_log_dict = log_pool.get_obj_log_dict()

        for obj, log in obj_log_dict.items():
            for idx, end_log_entry in enumerate(log):
                for j in range(idx - 1, -1, -1):
                    start_log_entry = log[j]

                    if not end_log_entry.is_close(start_log_entry):
                        break
                    if not start_log_entry.is_conflict(end_log_entry):
                        continue

                    start_tsc, end_tsc = start_log_entry.tsc_, end_log_entry.tsc_

                    nr_window += 1
                    self._encode_sync_in_window(thread_log_dict[start_log_entry.thread_],
                                                thread_log_dict[end_log_entry.thread_],
                                                start_tsc,
                                                end_tsc)
        print(f'  |_ #Window: {nr_window}')

    def _add_acq_constraint(self, var_set: List[Variable]):
        if len(var_set):
            self.acq_constraints_.add(VariableList(var_set))

    def _add_rel_constraint(self, var_set: List[Variable]):
        if len(var_set):
            self.rel_constraints_.add(VariableList(var_set))

    def _encode_sync_in_window(self, thread_log_1, thread_log_2, start_tsc, end_tsc):
        #
        # We just encode constraints for call operations now
        #
        rel_var_list = [
            SyncVariable.release_var(log_entry)
            for log_entry in thread_log_1. range_by(start_tsc, end_tsc)
            if log_entry.is_exit()
        ]
        self._add_rel_constraint(rel_var_list)

        #
        # For acquiring sites, implementing window + 1
        # Add a log whose tsc < start_tsc
        #
        acq_var_list = [
            SyncVariable.acquire_var(log_entry)
            for log_entry in thread_log_2. range_by(start_tsc, end_tsc, left_one_more=True)
            if log_entry.is_enter()
        ]

        self._add_acq_constraint(acq_var_list)
