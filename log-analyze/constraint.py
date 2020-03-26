from typing import List, Dict
from log_entry import LogEntry


LocationId = int


class ConstaintSystem():
    def __init__(self):
        self.location_id_: Dict[str, LocationId] = {}
        self.rel_constraints_: List[List[LocationId]] = []
        self.acq_constraints_: List[List[LocationId]] = []

    def add_release_constraint(self, log_list: List[LogEntry]):
        print("add release constrains")
        if len(log_list):
            self.rel_constraints_.append(list(map(self.get_location_id, log_list)))

    def add_acquire_constraint(self, log_list: List[LogEntry]):
        print("add acquire constrains")
        if len(log_list):
            self.acq_constraints_.append(list(map(self.get_location_id, log_list)))

    def get_location_id(self, entry: LogEntry) -> LocationId:
        if entry.location_ not in self.location_id_:
            self.location_id_[entry.location_] = len(self.location_id_)

        return self.location_id_[entry.location_]

    def print_system(self):
        print("Variable Defination")
        for loc, loc_id in self.location_id_.items() :
            print(f'   {loc}  {loc_id}')

        print("\nConstrains : ")

        for constraint in self.rel_constraints_:
            var_list = [f'R{loc_id}' for loc_id in constraint]
            s = f'{" + ".join(var_list)} + 0 > 1'
            print (s)

        for constraint in self.acq_constraints_:
            var_list = [f'A{loc_id}' for loc_id in constraint]
            s = f'{" + ".join(var_list)} + 0 > 1'
            print (s)