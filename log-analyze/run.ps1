$app = $args[0]

$looplimit = 1
$i = 1
Remove-Item 'E:\Sherlock\idelay\log-analyze\temp\*.*'

while ($i -le $looplimit){
    #$logdir = 'E:\TorchLite\Results-delay\' + $app + '-' + $i.ToString()
    $logdir = 'E:\TorchLite\Results\' + $app + '-' + $i.ToString()
    #$logdir = 'E:\TorchLite\Results\' + $app
    $output = 'results/log-' + $app + '-' + $i.ToString() + '.txt'
    #$time = Measure-Command{ python log_analyzer.py --batch $logdir -refine > $output}
    #Write-Host $time.TotalSeconds
    python log_analyzer.py --batch $logdir -refine 
    $i = $i + 1
}

<#
$logdir1 = 'E:\TorchLite\Results\' + $app + '-1'
$logdir2 = 'E:\TorchLite\Results\' + $app + '-2'
$logdir3 = 'E:\TorchLite\Results\' + $app + '-3'

$output1 = 'results/log-' + $app +'.txt'
$output2 = 'results/log-' + $app +'-2.txt'
$output3 = 'results/log-' + $app +'-3.txt'

Remove-Item 'E:\Sherlock\idelay\log-analyze\temp\*.*'

$time1 = Measure-Command{ python log_analyzer.py --batch $logdir1 -refine > $output1}
#python log_analyzer.py --batch $logdir
Write-Host $time1.TotalSeconds

$time2 = Measure-Command{ python log_analyzer.py --batch $logdir2 -refine > $output2}
#python log_analyzer.py --batch $logdir -refine
Write-Host $time2.TotalSeconds

$time3 = Measure-Command{ python log_analyzer.py --batch $logdir3 -refine > $output3}
#python log_analyzer.py --batch $logdir -refine
Write-Host $time3.TotalSeconds
#>