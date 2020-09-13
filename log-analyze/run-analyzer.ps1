$app = $args[0]

$looplimit = 1
$i = 1

if ($args.Count -gt 1){
    $Looplimit = [int]$args[1]
}
Write-Host $Looplimit

Remove-Item 'E:\Sherlock\idelay\log-analyze\temp\*.*'

while ($i -le $looplimit){
    $logdir = 'E:\TorchLite\Results-cov\' + $app + '-' + $i.ToString()
    #$logdir = 'E:\TorchLite\Results-persentailduration\' + $app + '-' + $i.ToString()
    #$logdir = 'E:\TorchLite\Results\' + $app
    $output = 'results/log-' + $app + '-' + $i.ToString() + '.txt'
    $time = Measure-Command{ python log_analyzer.py --batch $logdir -refine > $output}
    #$time = Measure-Command{ python log_analyzer.py --batch $logdir > $output}
    write-Host $time.TotalSeconds
    #python log_analyzer.py --batch $logdir -refine 
    $i = $i + 1
}
