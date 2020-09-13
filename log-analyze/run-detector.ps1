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
    $output = 'detects/log-' + $app + '-' + $i.ToString() + '.txt'
    $time = Measure-Command{ python race_detector.py --batch $logdir > $output}
    write-Host $time.TotalSeconds
    $i = $i + 1
}
