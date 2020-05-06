from typing import List
import re

class APISpecification():
    read_apis = []
    write_apis = []

    @classmethod
    def Initialize(cls):
        cls.read_apis.append('System\.Collections\.Generic\.Dictionary.*::get_Item')
        cls.read_apis.append('System\.Collections\.Generic\.Dictionary.*::get.*')
        cls.read_apis.append('System\.Collections\.Generic\.Dictionary.*::ContainsKey')
        cls.read_apis.append('System\.Collections\.Generic\.Dictionary.*::ContainsValue')
        cls.read_apis.append('System\.Collections\.Generic\.Dictionary.*::TryGetValue')
        cls.write_apis.append('System\.Collections\.Generic\.Dictionary.*::Add')
        cls.write_apis.append('System\.Collections\.Generic\.Dictionary.*::Remove')
        cls.write_apis.append('System\.Collections\.Generic\.Dictionary.*::Clear')
        cls.write_apis.append('System\.Collections\.Generic\.Dictionary.*::set_Item')
        cls.write_apis.append('System\.Collections\.Generic\.Dictionary.*::set.*')

        cls.read_apis.append('System\.Collections\.Generic\.IDictionary.*::get_Item')
        cls.read_apis.append('System\.Collections\.Generic\.IDictionary.*::ContainsKey')
        cls.read_apis.append('System\.Collections\.Generic\.IDictionary.*::TryGetValue')
        cls.write_apis.append('System\.Collections\.Generic\.IDictionary.*::Add')
        cls.write_apis.append('System\.Collections\.Generic\.IDictionary.*::Remove')
        cls.write_apis.append('System\.Collections\.Generic\.IDictionary.::Clear')
        cls.write_apis.append('System\.Collections\.Generic\.IDictionary.*::set_Item')

        cls.read_apis.append('System\.Collections\.Generic\.List.*::BinarySearch.*?')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::Contains')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::ConvertAll')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::CopyTo')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::Exists')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::Find.*')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::GetRange')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::IndexOf')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::get_Item')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::MemberwiseClone')
        cls.read_apis.append('System\.Collections\.Generic\.List.*::Take')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::Add')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::Clear')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::Remove')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::Reverse')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::Sort')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::ToArray')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::Insert')
        cls.write_apis.append('System\.Collections\.Generic\.List.*::set_Item')

    @classmethod
    def Wild_Match(cls, s: str, l :List[str]):
        for api in l:
            if re.search(api, s, re.I):
                #print(s, "match to", api)
                return True
        return False

    @classmethod
    def Is_Read_API(cls, s :str):
        return cls.Wild_Match(s, cls.read_apis)

    @classmethod
    def Is_Write_API(cls, s :str):
        return cls.Wild_Match(s, cls.write_apis)

        
