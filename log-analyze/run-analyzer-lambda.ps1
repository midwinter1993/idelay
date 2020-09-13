$app = $args[0]

$larray = '0.1','0.2'
$larray = '0.1','0.2','0.4','0.6','0.8','1','5','10','50','100'

$i = 1


#Write-Host $larray

Remove-Item 'E:\Sherlock\idelay\log-analyze\temp\*.*'

foreach($i in $larray){
    Remove-Item 'E:\Sherlock\idelay\log-analyze\temp\*.*'
    $logdir = 'E:\TorchLite\Results-cov\' + $app + '-1' 
    $output = 'results/log-' + $app + '-' + $i.ToString() + '.1txt'
    Write-Host python log_analyzer.py --lambda $i --batch $logdir -refine > $output
    $time = Measure-Command{ python log_analyzer.py --balance $i --batch $logdir -refine > $output}
    Write-Host $time.TotalSeconds

    $logdir = 'E:\TorchLite\Results-cov\' + $app + '-2' 
    $output = 'results/log-' + $app + '-' + $i.ToString() + '.2txt'
    Write-Host python log_analyzer.py --lambda $i --batch $logdir -refine > $output
    $time = Measure-Command{ python log_analyzer.py --balance $i --batch $logdir -refine > $output}
    Write-Host $time.TotalSeconds


    $logdir = 'E:\TorchLite\Results-cov\' + $app + '-3' 
    $output = 'results/log-' + $app + '-' + $i.ToString() + '.3txt'
    Write-Host python log_analyzer.py --lambda $i --batch $logdir -refine > $output
    $time = Measure-Command{ python log_analyzer.py --balance $i --batch $logdir -refine > $output}
    Write-Host $time.TotalSeconds


    $i = $i + 1
}
