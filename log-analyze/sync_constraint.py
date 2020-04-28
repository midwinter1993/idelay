from typing import List, Dict, Set, Tuple
from litelog import LogEntry
import sys
from lp_staff import LpBuilder, Variable, VariableList
from flipy import LpVariable
import flipy


# LocationId = int

class SyncVariable(Variable):
    variable_pool: Dict[str, 'SyncVariable'] = {}

    def __init__(self, uid: int, loc: str, description: str):
        super().__init__(uid)
        # self.type_ = ty
        self.loc_ = loc
        self.description_ = description

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
    def get_variable(cls, loc: str, description: str) -> 'SyncVariable':
        if loc not in cls.variable_pool:
            cls.variable_pool[loc] = SyncVariable(len(cls.variable_pool), loc, description)
        return cls.variable_pool[loc]

    @classmethod
    def release_var(cls, log_entry: LogEntry) -> 'SyncVariable':
        return cls.get_variable(log_entry.location_, log_entry.op_type_ + " " + log_entry.operand_)

    @classmethod
    def acquire_var(cls, log_entry: LogEntry) -> 'SyncVariable':
        return cls.get_variable(log_entry.location_, log_entry.op_type_ + " " + log_entry.operand_)


class ConstraintSystem:
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
        for loc, var in SyncVariable.variable_pool.items() :
            print(f'   {loc}  {var.uid_}, {var.description_}')

        # self.prob_.write_lp(sys.stdout)

        # print("\nConstrains : ")

        # for constraint in self.rel_constraints_:
        #     var_list = [var.as_str_rel() for var in constraint]
        #     s = f'1 <= {" + ".join(var_list)}'
        #     print (s)

        # for constraint in self.acq_constraints_:
        #     var_list = [var.as_str_acq() for var in constraint]
        #     s = f'1 <= {" + ".join(var_list)}'
        #     print (s)

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
            if 'Monitor.Enter' in var.description_:
                var.mark_as_acq()
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_acq()], 100))
            elif 'Monitor.Exit' in var.description_:
                var.mark_as_rel()
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_rel()], 100))

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

        # for name, var in Variable.variable_pool.items():
        #     if var.as_pulp_acq().varValue >= 95 or var.as_pulp_rel().varValue >= 95:
        #         print(name,
        #               f'{var.as_str_acq()}: {var.as_pulp_acq().varValue}',
        #               f'{var.as_str_rel()}: {var.as_pulp_rel().varValue}',
        #               var.description_)

        # for penalty in self.penalty_vars_:
            # print(penalty, penalty.varValue)

        print()
        print("Releasing sites :")
        for name, var in SyncVariable.variable_pool.items():
            if (var.as_lp_rel().evaluate() >= 95):
                print(f'{var.description_} @ {name} => {var.as_str_rel()}')

        print("Acquiring sites :")
        for name, var in SyncVariable.variable_pool.items():
            if (var.as_lp_acq().evaluate() >= 95):
                print(f'{var.description_} @ {name} => {var.as_str_acq()}')

        self.prob_.write_lp(open('./problem.lp', 'w'))