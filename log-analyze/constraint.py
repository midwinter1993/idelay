from typing import List, Dict, Set
from log_entry import LogEntry


LocationId = int


class Variable:
    location_id_mapping: Dict[str, LocationId] = {}

    def __init__(self, ty: str, uid: int):
        self.type_ = ty
        self.uid_ = uid

    def __str__(self):
        return f'{self.type_}{self.uid_}'

    def __repr__(self):
        return str(self)

    def __eq__(self, other):
        if isinstance(other, Variable):
            return self.type_ == other.type_ and self.uid_ == other.uid_
        else:
            return False

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.__repr__())


    @classmethod
    def get_location_id(cls, loc: str) -> LocationId:
        if loc not in cls.location_id_mapping:
            cls.location_id_mapping[loc] = len(cls.location_id_mapping)

        return cls.location_id_mapping[loc]

    @classmethod
    def release_var(cls, log_entry: LogEntry) -> 'Variable':
        uid = cls.get_location_id(log_entry.location_)
        return Variable('R', uid)

    @classmethod
    def acquire_var(cls, log_entry: LogEntry) -> 'Variable':
        uid = cls.get_location_id(log_entry.location_)
        return Variable('A', uid)


class ConstaintSystem:
    def __init__(self):
        self.rel_constraints_: List[List[Variable]] = []
        self.acq_constraints_: List[List[Variable]] = []

    def add_release_constraint(self, var_set: Set[Variable]):
        if len(var_set):
            self.rel_constraints_.append(var_set)

    def add_acquire_constraint(self, var_set: Set[Variable]):
        if len(var_set):
            self.acq_constraints_.append(var_set)

    def print_system(self):
        print("Variable Definition")
        for loc, loc_id in Variable.location_id_mapping.items() :
            print(f'   {loc}  {loc_id}')

        print("\nConstrains : ")

        for constraint in self.rel_constraints_:
            var_list = [str(var) for var in constraint]
            s = f'{" + ".join(var_list)} + 0 > 1'
            print (s)

        for constraint in self.acq_constraints_:
            var_list = [str(var) for var in constraint]
            s = f'{" + ".join(var_list)} + 0 > 1'
            print (s)