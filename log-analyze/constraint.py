from typing import List, Dict, Set, Tuple
from litelog import LogEntry
# from pulp import LpVariable, lpSum, LpMinimize
# from pulp import LpProblem, LpConstraint, LpConstraintGE, LpStatus
from flipy import LpVariable
import flipy

LocationId = int

class LpBuilder:
    cons_counter = 0

    @classmethod
    def var(cls, name: str, up_bound: int):
        return flipy.LpVariable(name, var_type=flipy.VarType.Integer,
                                low_bound=0, up_bound=up_bound)

    @classmethod
    def sum_expr(cls, lp_var_list: List[LpVariable]):
        return flipy.LpExpression(expression={v: 1 for v in lp_var_list})

    @classmethod
    def const_expr(cls, value: int):
        return flipy.LpExpression(constant=value)

    @classmethod
    def problem(cls, name: str):
        return flipy.LpProblem(name)

    @classmethod
    def _cons_id(cls):
        cls.cons_counter += 1
        return cls.cons_counter

    @classmethod
    def constraint_sum_geq(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr(lp_var_list)
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'geq', rhs, name=f'_C{cls._cons_id()}')

    @classmethod
    def constraint_sum_leq(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr(lp_var_list)
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'leq', rhs, name=f'_C{cls._cons_id()}')

    @classmethod
    def constraint_sum_eq(cls, lp_var_list: List[LpVariable], value: int):
        lhs = cls.sum_expr(lp_var_list)
        rhs = cls.const_expr(value)

        return flipy.LpConstraint(lhs, 'eq', rhs, name=f'_C{cls._cons_id()}')


class Variable:
    variable_pool: Dict[str, 'Variable'] = {}

    def __init__(self, loc: str, uid: int, description: str):
        # self.type_ = ty
        self.loc_ = loc
        self.uid_ = uid
        self.description_ = description

        #
        # We amplify the probability as integer values in [0, 100]
        #
        self.lp_rel_var_ = LpBuilder.var(self.as_str_rel(), up_bound=100)
        self.lp_acq_var_ = LpBuilder.var(self.as_str_acq(), up_bound=100)

    def __str__(self):
        return str(self.uid_)

    def __repr__(self):
        return str(self)

    def __eq__(self, other):
        if isinstance(other, Variable):
            return self.uid_ == other.uid_
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __lt__(self, other):
        return self.uid_ < other.uid_

    def __hash__(self):
        return hash(self.__repr__())

    def as_str_rel(self) -> str:
        return f'R{self.uid_}'

    def as_str_acq(self) -> str:
        return f'A{self.uid_}'

    def as_lp_rel(self) -> LpVariable:
        return self.lp_rel_var_

    def as_lp_acq(self) -> LpVariable:
        return self.lp_acq_var_

    @classmethod
    def get_variable(cls, loc: str, description: str) -> 'Variable':
        if loc not in cls.variable_pool:
            cls.variable_pool[loc] = Variable(loc, len(cls.variable_pool), description)
        return cls.variable_pool[loc]

    @classmethod
    def release_var(cls, log_entry: LogEntry) -> 'Variable':
        return cls.get_variable(log_entry.location_, log_entry.op_type_ + " " + log_entry.operand_)

    @classmethod
    def acquire_var(cls, log_entry: LogEntry) -> 'Variable':
        return cls.get_variable(log_entry.location_, log_entry.op_type_ + " " + log_entry.operand_)


class VariableList:
    def __init__(self, var_list: List[Variable]):
        self.var_list_ = sorted(set(var_list))

    def key(self) -> str:
        return '-'.join([str(var.uid_) for var in self.var_list_])

    def __hash__(self):
        return hash(self.key())

    def __eq__(self, other):
        if isinstance(other, VariableList):
            return hash(self) == hash(other)
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __iter__(self):
        return iter(self.var_list_)


class ConstaintSystem:
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
        for loc, var in Variable.variable_pool.items() :
            print(f'   {loc}  {var.uid_}, {var.description_}')

        print("\nConstrains : ")

        for constraint in self.rel_constraints_:
            var_list = [var.as_str_rel() for var in constraint]
            s = f'1 <= {" + ".join(var_list)}'
            print (s)

        for constraint in self.acq_constraints_:
            var_list = [var.as_str_acq() for var in constraint]
            s = f'1 <= {" + ".join(var_list)}'
            print (s)

    def _lp_encode_rel(self):
        for constraint in self.rel_constraints_:
            lp_var_list = [var.as_lp_rel() for var in constraint]

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
            lp_var_list = [var.as_lp_acq() for var in constraint]

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
        for var in Variable.variable_pool.values():
            self.prob_.add_constraint(LpBuilder.constraint_sum_leq([var.as_lp_acq(), var.as_lp_rel()], 100))

    def _lp_encode_all_vars_heuristic(self):
        for var in Variable.variable_pool.values():
            if 'Monitor.Enter' in var.description_:
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_acq()], 100))
            elif 'Monitor.Exit' in var.description_:
                self.prob_.add_constraint(LpBuilder.constraint_sum_eq([var.as_lp_rel()], 100))

    def _lp_encode_object_func(self):
        #
        # Object function: min(penalty + all variables)
        #
        obj_func_vars = self.penalty_vars_

        for var in Variable.variable_pool.values():
            obj_func_vars.append(var.as_lp_acq())
            obj_func_vars.append(var.as_lp_rel())

        obj = flipy.LpObjective(expression={v: 1 for v in obj_func_vars}, sense=flipy.Minimize)

        self.prob_.set_objective(obj)

    def lp_solve(self):
        self.prob_ = LpBuilder.problem("HB_Infer")
        self.penalty_vars_ = []

        self._lp_encode_rel()
        self._lp_encode_acq()
        self._lp_encode_all_vars()
        self._lp_encode_all_vars_heuristic()
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
        for name, var in Variable.variable_pool.items():
            if (var.as_lp_rel().evaluate() >= 95):
                print(f'{var.description_} @ {name} => {var.as_str_rel()}')

        print("Acquiring sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_lp_acq().evaluate() >= 95):
                print(f'{var.description_} @ {name} => {var.as_str_acq()}')

        self.prob_.write_lp(open('./problem.lp', 'w'))

