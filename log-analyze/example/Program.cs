using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;

namespace TestApp
{
    class Program
    {

        static List<string> list = new List<string>();

        static void Main(string[] args)
        {
            /*
            Stopwatch sw = new Stopwatch();
            sw.Start();
            TestClass tc = new TestClass();
            tc.UseFields();
            tc.UseFieldsInAnotherClass(new AnotherClass());
            Test();
            sw.Stop();
            Console.WriteLine("Ellapsed time (ms): " + sw.ElapsedMilliseconds);
            */
            ConstraintsTests ct = new ConstrainsTests();
            ct.RunTests();
        }

    }
}
