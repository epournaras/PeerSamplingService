#!/bin/bash

# includes
. colors.sh

# constants
eclipse_build_path="bin"
echo "eclipse_build_path : $eclipse_build_path"

netbeans_build_path="build/classes"
echo "netbeans_build_path : $netbeans_build_path"

# functions
function verify_class_path
{
	echo
	class_path=$1 
	echo "verifying class paths:"
	
	for path in `echo $class_path | tr ':' ' '`; do
		
		echo -n "$path..."
		if [ ! -e $path ]; then red "path $path not found"; exit 1; fi
		echo "ok"
		
	done

	green "class path verified"
}


