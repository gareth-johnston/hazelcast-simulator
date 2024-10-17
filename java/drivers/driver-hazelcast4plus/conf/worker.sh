#!/bin/bash

#
# Script to start up a Simulator Worker.
#
# To customize the behavior of the Worker, including Java configuration, copy this file into the 'work dir' of Simulator.
# See the end of this file for examples for different profilers.
#

# automatic exit on script failure
set -e
# printing the command being executed (useful for debugging)
#set -x

# redirecting output/error to the right log files
exec > worker.out
exec 2> worker.err

# Read the parameters file and add it to the environment
while IFS='=' read -r key value; do
    export "$key"="$value"
done < "parameters"


if [ "${WORKER_TYPE}" = "member" ]; then
  # Check if cp-data is mounted
  if mount | grep /cp-data > /dev/null; then
      echo "Clearing contents of /cp-data directory..."
      sudo rm -rf /cp-data/*
  else
      echo "Setting up /cp-data for the first time..."

      # Unmount /dev/nvme1n1 if it's already mounted elsewhere
      if mount | grep /dev/nvme1n1 > /dev/null; then
          echo "Unmounting /dev/nvme1n1..."
          sudo umount /dev/nvme1n1
      fi

      # Create XFS filesystem on /dev/nvme1n1 if it's not already formatted
      sudo mkfs.xfs -f /dev/nvme1n1 || { echo "Failed to format /dev/nvme1n1"; exit 1; }

      # Create /cp-data directory if it doesn't exist
      sudo mkdir -p /cp-data

      # Change the ownership of cp-data to ec2-user
      sudo chown ec2-user:ec2-user /cp-data

      # Change the permissions of cp-data to allow read/write by ec2-user
      sudo chmod 755 /cp-data

      # Mount the filesystem
      sudo mount /dev/nvme1n1 /cp-data

      # Change the ownership of the mounted filesystem to ec2-user
      sudo chown ec2-user:ec2-user /cp-data
  fi
fi

# If you want to be sure that you have the right governor installed; uncomment
# the following 3 lines. They will force the right governor to be used.
#old_governor=$(sudo cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor)
#echo old_governor: $old_governor
#sudo cpupower frequency-set -g performance

# If you have a specific java version you want to use, uncomment the following lines
# and configure the path to the version to use.
#JAVA_HOME=~/java/jdk1.8.0_121
#PATH=$JAVA_HOME/bin:$PATH

JVM_ARGS="-Dlog4j2.configurationFile=log4j.xml"

if [ "${WORKER_TYPE}" = "member" ]; then
    JVM_OPTIONS=$member_args
else
    JVM_OPTIONS=$client_args
fi

# Include the member/client-worker jvm options
JVM_ARGS="$JVM_OPTIONS $JVM_ARGS"

MAIN=com.hazelcast.simulator.worker.Worker

java -classpath "$CLASSPATH" ${JVM_ARGS} ${MAIN}

#########################################################################
# Yourkit
#########################################################################
#
# When YourKit is enabled, a snapshot is created an put in the worker home directory.
# So when the artifacts are downloaded, the snapshots are included and can be loaded with your YourKit GUI.
#
# To upload the libyjpagent, create a 'upload' directory in the working directory and place the libypagent.so there.
# Then it will be automatically uploaded to all workers.
#
# For more information about the YourKit setting, see:
#   http://www.yourkit.com/docs/java/help/agent.jsp
#   http://www.yourkit.com/docs/java/help/startup_options.jsp
#
# java -agentpath:$(pwd)/libyjpagent.so=dir=$(pwd),sampling -classpath $CLASSPATH $JVM_ARGS $MAIN
#

#########################################################################
# HProf
#########################################################################
#
# By default a 'java.hprof.txt' is created in the worker directory.
# The file will be downloaded by the Coordinator after the test has run.
#
# For configuration options see:
#   http://docs.oracle.com/javase/7/docs/technotes/samples/hprof.html
#
# java -agentlib:hprof=cpu=samples,depth=10 -classpath $CLASSPATH $JVM_ARGS $MAIN
#

#########################################################################
# VTune
#########################################################################
#
# It requires Intel VTune to be installed on the system.
#
# The settings is the full commandline for the amplxe-cl excluding the actual arguments for the java program to start.
# These will be provided by the Simulator Agent.
#
# Once the test run completes, all the artifacts will be downloaded by the Coordinator.
#
# To see within the JVM, make sure that you locally have the same Java version (under the same path) as the simulator.
# Else VTune will not be able to see within the JVM.
#
# Reference to amplxe-cl commandline options:
# https://software.intel.com/sites/products/documentation/doclib/iss/2013/amplifier/lin/ug_docs/GUID-09766DB6-3FA8-445B-8E70-5BC9A1BE7C55.htm#GUID-09766DB6-3FA8-445B-8E70-5BC9A1BE7C55
#
# /opt/intel/vtune_amplifier_xe/bin64/amplxe-cl -collect hotspots java -classpath $CLASSPATH $JVM_ARGS $MAIN
#

#########################################################################
# NUMA Control
#########################################################################
#
# NUMA Control. It allows to start member with a specific numactl settings.
# numactl binary has to be available on $PATH
#
# Example: NUMA_CONTROL=numactl -m 0 -N 0
# It will bind members to node 0.
# numactl -m 0 -N 0 java -classpath $CLASSPATH $JVM_ARGS $MAIN
#

#########################################################################
# OpenOnload
#########################################################################
#
# The network stack for Solarflare network adapters (new lab).
#
# onload --profile=latency java -classpath $CLASSPATH $JVM_ARGS $MAIN
#
