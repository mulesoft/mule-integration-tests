#!/bin/sh

##############################################
# Null Processor
##############################################
echo "org.mule.test.FlowNullProcessorBenchmark (Null Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 64
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 128

##############################################
# CPU Light Processor
##############################################
echo "org.mule.test.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 64

##############################################
# CPU Intensive Processor
##############################################
echo "org.mule.test.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 64

##############################################
# Blocking Processor
##############################################
echo "org.mule.test.FlowNullProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 64

##############################################
# Mixed Processors A
##############################################
echo "org.mule.test.FlowMixedAProcessorBenchmark (Mix)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 64

##############################################
# Mixed Processors A
##############################################
echo "org.mule.test.FlowMixedAProcessorBenchmark (Mix)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 64


##############################################
# Mixed Processors A
##############################################
echo "org.mule.test.FlowMixedAProcessorBenchmark (Mix A)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedAProcessorBenchmark -t 64

##############################################
# Mixed Processors B
##############################################
echo "org.mule.test.FlowMixedBProcessorBenchmark (Mix B)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedBProcessorBenchmark -t 64


##############################################
# Mixed Processors C
##############################################
echo "org.mule.test.FlowMixedCProcessorBenchmark (Mix C)"

java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 1
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 2
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 4
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 8
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 16
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 32
java -jar mule-runtime-benchmarks-4.0.0-SNAPSHOT.jar org.mule.test.FlowMixedCProcessorBenchmark -t 64
