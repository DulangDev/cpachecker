./scripts/cpa.sh -timelimit 10s -preprocess -bmc-interpolation -setprop cpa.loopbound.maxLoopIterationsUpperBound=3 -spec sv-comp-reachability $1
#./scripts/cpa.sh -timelimit 10s -preprocess -bmc-interpolation -setprop cpa.loopbound.maxLoopIterationsUpperBound=3 -spec sv-comp-reachability example/hand-crafted/even_LBE.c
firefox output/Report.html &
