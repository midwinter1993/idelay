using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace TestApp
{
    class ConstraintsTests
    {
        List<int> mylist;
      
        public ConstrainsTests()
        {
            this.mylist = new List<int>();
        }

        public void AddWithLock()
        {
            lock (this.mylist)
            {
                mylist.Add(3);
            }
        }
        public void AddWithoutLock()
        {
            mylist.Add(3);
        }

        public void ChangeWithLock()
        {
            lock (this.mylist)
            {
                if (this.mylist != null)
                    this.mylist = new List<int>();
            }
        }

        public void ChangeWithoutLock()
        {
            if (this.mylist != null)
                this.mylist = new List<int>();
        }

        public void RunTests()
        {
            RunWithTwothreads(AddWithLock);
            // RunWithTwothreads(AddWithoutLock);
            RunWithTwothreads(ChangeWithLock);
            // RunWithTwothreads(ChangeWithoutLock);
        }
       
        public delegate void Del();
        public void RunWithTwothreads(Del f){
            Stopwatch sw = new Stopwatch();
            sw.Start();
            Thread t1 = new Thread(new ThreadStart(f));
            Thread t2 = new Thread(new ThreadStart(f));
            t1.Start();
            t2.Start();
            t1.Join();
            t2.Join();
            sw.Stop();
            Console.WriteLine("Ellapsed time (ms): " + sw.ElapsedMilliseconds);
        }
    }
}
