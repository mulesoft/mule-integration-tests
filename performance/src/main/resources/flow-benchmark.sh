#!/bin/sh

##############################################
# Null Processor
##############################################
echo "org.mule.test.FlowNullProcessorBenchmark (Null Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowNullProcessorBenchmark -t 32

##############################################
# CPU Light Processor
##############################################
echo "org.mule.test.FlowCPULightProcessorBenchmark (CPU Light Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULightProcessorBenchmark -t 32

##############################################
# CPU Light 2 Processor
##############################################
echo "org.mule.test.FlowCPULight2ProcessorBenchmark (CPU Light 2 Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULight2ProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULight2ProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULight2ProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULight2ProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULight2ProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowCPULight2ProcessorBenchmark -t 32

##############################################
# Blocking Processor
##############################################
echo "org.mule.test.FlowBlockingProcessorBenchmark (Blocking Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlockingProcessorBenchmark -t 32

##############################################
# Blocking 2 Processor
##############################################
echo "org.mule.test.FlowBlocking2ProcessorBenchmark (Blocking 2 Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlocking2ProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlocking2ProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlocking2ProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlocking2ProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlocking2ProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowBlocking2ProcessorBenchmark -t 32

##############################################
# IO Small Processor
##############################################
echo "org.mule.test.FlowIOSmallProcessorBenchmark (IO Small Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOSmallProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOSmallProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOSmallProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOSmallProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOSmallProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOSmallProcessorBenchmark -t 32

##############################################
# IO Medium Processor
##############################################
echo "org.mule.test.FlowIOMediumProcessorBenchmark (IO Medium Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOMediumProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOMediumProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOMediumProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOMediumProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOMediumProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOMediumProcessorBenchmark -t 32


##############################################
# IO Large Processor
##############################################
echo "org.mule.test.FlowIOLargeProcessorBenchmark (IO Large Processor)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOLargeProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOLargeProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOLargeProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOLargeProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOLargeProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowIOLargeProcessorBenchmark -t 32


##############################################
# Mix Light
##############################################
echo "org.mule.test.FlowLightMixProcessorBenchmark (Mix Light)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowLightMixProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowLightMixProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowLightMixProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowLightMixProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowLightMixProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowLightMixProcessorBenchmark -t 32


##############################################
# Mix Heavy
##############################################
echo "org.mule.test.FlowHeavyMixProcessorBenchmark (Mix Heavy)"

java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowHeavyMixProcessorBenchmark -t 1
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowHeavyMixProcessorBenchmark -t 2
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowHeavyMixProcessorBenchmark -t 4
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowHeavyMixProcessorBenchmark -t 8
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowHeavyMixProcessorBenchmark -t 16
java -jar mule-runtime-integration-benchmarks-4.1.0-SNAPSHOT.jar org.mule.test.FlowHeavyMixProcessorBenchmark -t 32




