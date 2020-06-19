# from pulp import LpVariable, lpSum, LpMinimize
# from pulp import LpProblem, LpConstraint, LpConstraintGE, LpStatus
from typing import Dict, List
from flipy import LpVariable
import flipy


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
    def __init__(self, uid: int):
        self.uid_ = uid

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

