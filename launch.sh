#!/bin/bash

# launch N processses using LiveRun
# eag - 2017-02-06

# arguments
npeers=$1

# includes
. colors.sh
. verify.class.path.sh

# flags
set -e
set -u

# verify arguments
if [ -z $npeers ]; then red "missing argument 1 : npeers"; exit 1; fi
	
# constants
javamem=128M
echo "javamem : $javamem"

baseport=5555
echo "baseport : $baseport"

# java class path
classpath="$netbeans_build_path:../Protopeer/$eclipse_build_path:../jeromq/$eclipse_build_path:dist/lib/*"
echo "classpath : $classpath"

main="experiments.live.PeerSamplingServiceExperiment"
echo "main : $main"

screenstem="peersampling"
echo "screenstem : $screenstem"

serverscreenname="$screenstem.server"
echo "serverscreenname : $serverscreenname"

screenname="$screenstem.nodes"
echo "screenname : $screenname"

logdir=log/$screenstem
echo "logdir : $logdir"

# profiling
profile=0
echo "profile : $profile"

profiledir=profile/$screenstem
echo "profiledir : $profiledir"

profilearg=""	# by default no profiling

# files + folders
mkdir -p $logdir

mkdir -p $profiledir


# start

# verify java class path
verify_class_path "$classpath"

# verify if nodes screen already exists
if screen -ls | grep "$screenname" ; then red "a screen session with name $screenname already found"; exit 1; fi

# launch bootstrap server
if screen -ls | grep "$serverscreenname" ; then 
	echo "bootstrap server $serverscreenname already found"
else
	logfile=$logdir/server.log
	rm -f $logfile
	
	# create a command file to launch each screen, necessary so that screen output can be tee'd to logfile
	# absolutely critical that each screen tab has it's own command file, cannot use the same one
	# for all tabs because of concurrency
	cmdfile=/tmp/$0.server.sh
	echo "cmdfile : $cmdfile"
	
	profileoutput=$profiledir/server.out
	echo "profileoutput : $profileoutput"
	
	if [ $profile == 1 ]; then
		profilearg="-Xrunhprof:interval=60000,depth=20,heap=sites,format=a,file=$profileoutput"
		echo "profilearg : $profilearg"
	fi

	echo java -Xmx$javamem $profilearg -cp "$classpath" "$main" 0 $baseport | tee $logfile > $cmdfile
	echo -n "launching server on port $baseport..."
	screen -d -m -S $serverscreenname -t "server" bash $cmdfile
	echo "ok"
	
	if [ $npeers == 0 ]; then
		screen -dr $serverscreenname
		exit 0
			
	else

		# wait for screen to be fully initialised before launching the windows	
		echo -n "waiting 1 seconds before launching peers..."
		sleep 1
		echo "ok"
	fi
fi
			
# launch peers
for peer in `seq 1 1 $npeers`; do
	
	port=$(($baseport + $peer))
	title="$port"
	
	logfile=$logdir/peer.$peer.log
	rm -f $logfile
	
	cmdfile=/tmp/$0.$peer.sh
	echo "cmdfile : $cmdfile"
	
	profileoutput=$profiledir/$peer.out
	echo "profileoutput : $profileoutput"
	
	if [ $profile == 1 ]; then
		profilearg="-Xrunhprof:interval=60000,depth=20,heap=sites,format=a,file=$profileoutput"
		echo "profilearg : $profilearg"
	fi

	echo -n "launching peer $peer on port $port..."
	echo java -Xmx$javamem $profilearg -cp "$classpath" "$main" $peer $port | tee $logfile > $cmdfile
	if [ $peer == 1 ]; then
		screen -d -m -S $screenname -t "$title" bash $cmdfile
	else
		screen -S $screenname -X screen -t "$title" bash $cmdfile
	fi
		
	echo "ok"
	

done

# login
screen -dr $screenname

echo
echo "launched"
	

