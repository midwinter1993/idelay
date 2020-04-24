from typing import List, Dict, Set, Tuple
from litelog import LogEntry
from flipy import LpVariable
from lp_staff import LpBuilder, Variable, VariableList
import flipy
import sys


class HbVariable(Variable):
    variable_pool: Dict[str, 'HbVariable'] = {}

    def __init__(self, uid: int, hb_key: str):
        super().__init__(uid)
        # self.type_ = ty
        self.hb_key_ = hb_key

        self.lp_hb_var_ = LpBuilder.var(self.as_str_hb(), up_bound=100)

    def as_str_hb(self) -> str:
        return f'HB{self.uid_}'

    def as_lp_hb(self) -> LpVariable:
        return self.lp_hb_var_

    @classmethod
    def hb_var(cls, before_log_entry: LogEntry, after_log_entry: LogEntry) -> 'HbVariable':

        before_operand = before_log_entry.operand_
        after_operand = after_log_entry.operand_

        hb_key = f'{before_operand} -> {after_operand}'

        if hb_key not in cls.variable_pool:
            cls.variable_pool[hb_key] = HbVariable(len(cls.variable_pool), hb_key )

        return cls.variable_pool[hb_key]


class HbConstraintSystem:
    def __init__(self):
        self.hb_constraints_: Set[VariableList] = set()

    def add_hb_constraint(self, var_set: List[Variable]):
        if len(var_set):
            self.hb_constraints_.add(VariableList(var_set))

    def print_system(self):
        print("Variable Definition")
        for hb_key, var in HbVariable.variable_pool.items() :
            print(f'   {hb_key}  {var.uid_}')

    def _lp_encode_hb(self):
        for constraint in self.hb_constraints_:
            lp_var_list = [var.as_lp_hb() for var in constraint]

            if not lp_var_list:
                continue

            penalty = LpBuilder.var(f'Penalty{len(self.penalty_vars_)}', up_bound=50)
            self.penalty_vars_.append(penalty)

            lp_var_list.append(penalty)

            self.prob_.add_constraint(LpBuilder.constraint_sum_geq(lp_var_list, 100))
            # self.prob_ += lpSum(lp_var_list) <= 199

    def _lp_encode_object_func(self):
        #
        # Object function: min(penalty + all variables)
        #
        obj_func_vars = self.penalty_vars_

        for var in HbVariable.variable_pool.values():
            obj_func_vars.append(var.as_lp_hb())

        obj = flipy.LpObjective(expression={v: 1 for v in obj_func_vars}, sense=flipy.Minimize)

        self.prob_.set_objective(obj)

    def lp_solve(self):
        self.prob_ = LpBuilder.problem("HB_Infer")
        self.penalty_vars_ = []

        self._lp_encode_hb()
        self._lp_encode_object_func()

        solver = flipy.CBCSolver()
        status = solver.solve(self.prob_)

        print('Solving Status:', status)

        for hb_key, var in HbVariable.variable_pool.items():
            if (var.as_lp_hb().evaluate() >= 95):
                print(f'{hb_key} => {var.as_str_hb()}')

        self.prob_.write_lp(open('./problem.lp', 'w'))