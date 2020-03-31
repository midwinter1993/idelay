from typing import List, Dict, Set, Tuple
from litelog import LogEntry
from pulp import LpVariable, lpSum, LpMinimize
from pulp import LpProblem, LpConstraint, LpConstraintGE, LpStatus


LocationId = int


class Variable:
    variable_pool: Dict[str, 'Variable'] = {}

    def __init__(self, uid: int, description: str):
        # self.type_ = ty
        self.uid_ = uid
        self.description_ = description

        #
        # We amplify the probability as integer values in [0, 100]
        #
        self.pulp_rel_var_ = LpVariable(self.as_str_rel(), 0, 100)
        self.pulp_acq_var_ = LpVariable(self.as_str_acq(), 0, 100)

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

    def as_pulp_rel(self) -> LpVariable:
        return self.pulp_rel_var_

    def as_pulp_acq(self) -> LpVariable:
        return self.pulp_acq_var_

    @classmethod
    def get_variable(cls, loc: str, description: str) -> 'Variable':
        if loc not in cls.variable_pool:
            cls.variable_pool[loc] = Variable(len(cls.variable_pool), description)
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
        for loc, loc_id in Variable.variable_pool.items() :
            print(f'   {loc}  {loc_id}')

        print("\nConstrains : ")

        for constraint in self.rel_constraints_:
            var_list = [var.as_str_rel() for var in constraint]
            s = f'1 <= {" + ".join(var_list)} <= 2'
            print (s)

        for constraint in self.acq_constraints_:
            var_list = [var.as_str_acq() for var in constraint]
            s = f'1 <= {" + ".join(var_list)} <= 2'
            print (s)

    def pulp_solve(self):
        prob = LpProblem("HB_Infer", LpMinimize)

        penalty_vars = []

        for constraint in self.rel_constraints_:
            lp_var_list = [var.as_pulp_rel() for var in constraint]

            #
            # There is only one release operation
            #
            penalty = LpVariable(f'Penalty{len(penalty_vars)}', 0, 50)
            penalty_vars.append(penalty)

            prob += lpSum(lp_var_list) + penalty >= 100
            prob += lpSum(lp_var_list) <= 200

        for constraint in self.acq_constraints_:
            lp_var_list = [var.as_pulp_acq() for var in constraint]

            #
            # There is only one acquire operation
            #
            penalty = LpVariable(f'Penalty{len(penalty_vars)}', 0, 50)
            penalty_vars.append(penalty)

            prob += lpSum(lp_var_list) + penalty >= 100
            prob += lpSum(lp_var_list) <= 200

        #
        # Object function: min(penalty + all variables)
        #
        obj_func_vars = penalty_vars

        for var in Variable.variable_pool.values():
            #
            # For each variable/location, P_rel + P_acq < 100
            #
            prob += var.as_pulp_acq() + var.as_pulp_rel() <= 100

            obj_func_vars.append(var.as_pulp_acq())
            obj_func_vars.append(var.as_pulp_rel())

        #
        # Object function here
        #
        prob += lpSum(obj_func_vars)

        status = prob.solve()
        print(LpStatus[status])

        for name, var in Variable.variable_pool.items():
            if (var.as_pulp_acq().varValue >= 95 or var.as_pulp_rel().varValue >= 95):
                print(name, (f'{var.as_str_acq()}: {var.as_pulp_acq().varValue}',
                             f'{var.as_str_rel()}: {var.as_pulp_rel().varValue}'))
                print(var.description_)

        # for penalty in penalty_vars:
            # print(penalty, penalty.varValue)

        print()
        print("Releasing sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_pulp_rel().varValue >= 95):
                print(f'{var.description_} @ {name} => {var.as_str_rel()}')

        print("Acquiring sites :")
        for name, var in Variable.variable_pool.items():
            if (var.as_pulp_acq().varValue >= 95):
                print(f'{var.description_} @ {name} => {var.as_str_acq()}')

