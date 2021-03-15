powershell

$PROJECT_HOME="C:\Users\nomea\Documents\Projects\jms"
cd $PROJECT_HOME

####################################################################################################
# Install JDK
####################################################################################################

bitsadmin /CREATE /DOWNLOAD download_openjdk_12
bitsadmin /ADDFILE download_openjdk_12 "https://download.java.net/java/GA/jdk12.0.2/e482c34c86bd4bf8b56c0b35558996b9/10/GPL/openjdk-12.0.2_windows-x64_bin.zip" $PROJECT_HOME\openjdk-12.0.2_windows-x64_bin.zip
bitsadmin /RESUME download_openjdk_12
bitsadmin /INFO download_openjdk_12 /VERBOSE
bitsadmin /COMPLETE download_openjdk_12

Expand-Archive -Path "$PROJECT_HOME\openjdk-12.0.2_windows-x64_bin.zip" -DestinationPath ".\"

$env:JAVA_HOME = "$PROJECT_HOME\jdk-12.0.2"
$env:Path = "$env:JAVA_HOME\bin" + ";$env:Path"

####################################################################################################
# Install Scala
# Compatibility Reference -- https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html
####################################################################################################

bitsadmin /CREATE /DOWNLOAD download_scala_2_13_4
bitsadmin /ADDFILE download_scala_2_13_4 "https://downloads.lightbend.com/scala/2.13.4/scala-2.13.4.msi" $PROJECT_HOME\scala-2.13.4.msi
bitsadmin /RESUME download_scala_2_13_4
bitsadmin /INFO download_scala_2_13_4 /VERBOSE
bitsadmin /COMPLETE download_scala_2_13_4

.\scala-2.13.4.msi

$env:SCALA_HOME =  "C:\Program Files (x86)\scala"
$env:Path = "$env:SCALA_HOME\bin" + ";$env:Path"

####################################################################################################
# Reference -- http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html
# Build a Scala skeleton project using the Maven plugin
####################################################################################################

bitsadmin /CREATE /DOWNLOAD download_maven
bitsadmin /ADDFILE download_maven "https://mirrors.ocf.berkeley.edu/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip" $PROJECT_HOME\apache-maven-3.6.3-bin.zip
bitsadmin /RESUME download_maven
bitsadmin /INFO download_maven /VERBOSE
bitsadmin /COMPLETE download_maven

Expand-Archive -Path "$PROJECT_HOME\apache-maven-3.6.3-bin.zip" -DestinationPath ".\"

$env:Path += ";$PROJECT_HOME\apache-maven-3.6.3\bin"
mvn -v

$APP_NAME="jms-connector-app"
mkdir $APP_NAME

mvn archetype:generate "-DarchetypeGroupId=net.alchim31.maven" "-DarchetypeArtifactId=scala-archetype-simple" "-DartifactId=$APP_NAME" "-DgroupId=com.statisticalfx.jms" "-DinteractiveMode=false"

cd $APP_NAME

####################################################################################################
# Change pom.xml
# Edit the Java version in <maven.compiler.source>, using Java 12 in this example
# A massive jar is built with all of the dependencies self-contained including the Scala runtime
####################################################################################################
mvn package

####################################################################################################
# How to initialize environment and run Scala programs
####################################################################################################
powershell
$PROJECT_HOME="C:\Users\nomea\Documents\Projects\jms"
$env:JAVA_HOME = "$PROJECT_HOME\jdk-12.0.2"
$env:Path = "$env:JAVA_HOME\bin" + ";$env:Path"
$env:SCALA_HOME =  "C:\Program Files (x86)\scala"
$env:Path = "$env:SCALA_HOME\bin" + ";$env:Path"
$env:Path += ";$PROJECT_HOME\apache-maven-3.6.3\bin"
$APP_NAME="jms-connector-app"

java -cp target/jms-connector-app-1.0-SNAPSHOT-jar-with-dependencies.jar com.statisticalfx.jms.OneMessagePerFileInDirectoryApp
java -cp target/jms-connector-app-1.0-SNAPSHOT-jar-with-dependencies.jar com.statisticalfx.jms.ReadQueueWriteToS3App

