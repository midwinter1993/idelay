from typing import Dict
import pprint


class ConstantPool:
    @staticmethod
    def _load_int(fd):
        byte_array = fd.read(4)
        if byte_array:
            return int.from_bytes(byte_array, byteorder='big', signed=False)
        else:
            None

    @staticmethod
    def _load_string(fd):
        l = ConstantPool._load_int(fd)
        if l:
            byte_array = fd.read(l)
            return byte_array.decode('utf-8')
        else:
            return None

    @staticmethod
    def _load_constant_pool(file_path: str) -> Dict[int, str]:
        constant_pool = {}
        with open(file_path, 'rb') as fd:
            while True:
                s = ConstantPool._load_string(fd)
                if not s:
                    break
                s_uid = ConstantPool._load_int(fd)
                constant_pool[s_uid] = s

        return constant_pool

    def __init__(self, file_path: str):
        self.pool_dict_: Dict[int, str] = ConstantPool._load_constant_pool(file_path)
        self.pool_dict_[-1] = 'Mark.Thread'

    def get_str(self, str_uid: int) -> str:
        return self.pool_dict_[str_uid]

    def dump(self):
        pprint.pprint(self.pool_dict_)