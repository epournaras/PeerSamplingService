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

protopeer_conf=conf/protopeer.conf
echo "protopeer_conf : $protopeer_conf"

# java class path
classpath="$netbeans_build_path:../Protopeer/$eclipse_build_path:../jeromq/$eclipse_build_path:dist/lib/*"
echo "classpath : $classpath"

main="experiments.live.PeerSamplingServiceExperiment"
echo "main : $main"

screenname="peersampling"
echo "screenname : $screenname"

logdir=log/$screenname
echo "logdir : $logdir"

# profiling
profile=0
echo "profile : $profile"

profiledir=profile/$screenname
echo "profiledir : $profiledir"

profilearg=""	# by default no profiling

# files + folders
if [ ! -e $protopeer_conf ]; then red "could not find protopeer_conf $protopeer_conf"; exit 1; fi
mkdir -p $logdir

mkdir -p $profiledir


# start

# get bootstrap port
baseport=`cat $protopeer_conf | grep peerZeroPort | cut -f2 -d'='`
echo "baseport : $baseport"


# verify java class path
verify_class_path "$classpath"

# verify if nodes screen already exists
if screen -ls | grep "$screenname" ; then red "a screen session with name $screenname already found"; exit 1; fi

# launch bootstrap server
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
screen -d -m -S $screenname -t "server" bash $cmdfile
echo "ok"

if [ $npeers == 0 ]; then
	screen -dr $screenname
	exit 0
		
else

	# wait for screen to be fully initialised before launching the windows	
	echo -n "waiting 1 seconds before launching peers..."
	sleep 1
	echo "ok"
fi

			
# launch peers, starting at peer 1
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
	echo java -Xmx$javamem $profilearg -cp "$classpath" "$main" $peer 0 | tee $logfile > $cmdfile
	
	# launch in the existing screen session (recall that bootstrap server was launched first)
	screen -S $screenname -X screen -t "peer.""$peer" bash $cmdfile
	
		
	echo "ok"
	

done

# login
screen -dr $screenname

echo
echo "launched"
	

