## Usage of SherLock
Dynamic SherLock has three steps to apply:
- Instrumentation: instrument the binaries.
- Log transformation: Split the generated log from runtime for next step.
- Constraints build and solve: linear solver for the constraint system.

# Instrumentation
> TorchLite-master-0410\TorchLite\bin\Debug\TorchLite.exe [Binary Dicrectory]

All the binaries under this directory are instrumented. When run with the input, we can see a Runtime.log
PS: There is a hard-coded path "E:\TSVD\Benchmarks\config\rel_vars.lp" for predefined delay. Change it if possible.

# Log Transformation
> Analyzer\bin\Debug\Analyzer.exe [log Directory]

Analyzer.exe searches all the Runtime.log under specified directory and splits it based on the thread-id. 

# Constraints Build and Solve
> python log-analyze\log_analyzer.py --batch [log Directory] -refine

Log directory contains all the splitted log files for constraints system. The structure under log directory is 

log directory
	|

	|--test1

	|    |
	
	|    |--*.litelog
		
	|--test2
	
	     |
	     
	     |--*.litelog

the option refine means it will load the previous solving results and store current results to a checkpoint directory. The checkpoint directory is hardly coded as "E:/Sherlock/idelay/log-analyze/temp". Change it if necessary.
Under the checkpoint directory, the rel_vars.lp is the predefined delay file.

## Benchmarks
ApplicationInsights https://github.com/microsoft/ApplicationInsights-dotnet

DataTimeExtention https://github.com/joaomatossilva/DateTimeExtensions

FluentAssertion https://github.com/fluentassertions/fluentassertions

K8s-Client https://github.com/kubernetes-client/csharp

Radical https://github.com/RadicalFx/Radical

RestSharp https://github.com/restsharp/RestSharp

Stastd https://github.com/lukevenediger/statsd.net

Linq.Dynamic https://github.com/zzzprojects/System.Linq.Dynamic

Let me know if I need to upload all the suit for the testing on my side as it is too large.


## Static SherLock
Can we shift the approach to pure static? 
- variable. Yes, the definition in dynamic sherlock is still static.
- Type constraints. Yes.
- Pair constraints. Yes.
- Rareness constraints. Yes. It is doable to judge popular or rare operation statically.
- Acquire time varies. No.
- Protection constraints. Not easy. We need to decide if two operations are in different thread, accessing same object, somehow close with each other. Then we also need to guess which is the early operation and later operation for generating windows.
