
from parse import LiteLog

class ConstainedSystem():
    def __init__(self):
        self.loc_to_index_ = {}
        self.rconstrains = []
        self.aconstrains = []
        self.index = 0
                    
    def load_require_constrain(self, rlist):
        print("add require constrains")
        self.rconstrains.append([self.get_id(entry) for entry in rlist])

    def load_acquire_constrain(self, alist):
        print("add acquire constrains")
        self.aconstrains.append([self.get_id(entry) for entry in alist])

    def get_id(self, entry):
        if (entry.location_ not in self.loc_to_index_):
            self.loc_to_index_[entry.location_] = self.index
            self.index = self.index +1
        return self.loc_to_index_[entry.location_]

    def print_system(self):
        print("Variable Defination")
        for loc in self.loc_to_index_ :
            print("   ",loc, " ",self.loc_to_index_[loc])
        print()
        print("Constrains : ")
        for c in self.rconstrains:
            s = ""
            for v in c:
                s = s + "R"+str(v) + " + "
            s = s + " 0 > 1"
            print (s)

        for c in self.rconstrains:
            s = ""
            for v in c:
                s = s + "A"+str(v) + " + "
            s = s + " 0 > 1"
            print (s)
        return

