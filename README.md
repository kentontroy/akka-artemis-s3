## General Pattern <br>

<img src="./statfx_akka_amq_s3.png" alt="Simple pattern"/> <br>
<ul>
<li>An Akka typed Actor system is used with a Guardian to control the lifecycle of a JMS Consumer</li>
<li>The JMS Consumer reads messages from an ActiveMQ instance running within a Docker container</li>
<li>Configuration options are set to control the session count, batch size, Ack timeout, and polling interval</li>
<li>Akka stream types are used to wrap the JMS message consumed within a transaction envelope</li>
<li>If the JMS message is successfully written to S3 (via multi-part upload), the transaction is committed</li>
</ul>

## Initialize a Powershell Environment and create a Project Home directory <br>

powershell <br>

$PROJECT_HOME="C:\Users\nomea\Documents\Projects\jms" <br>
cd $PROJECT_HOME <br>

## Install JDK<br>

bitsadmin /CREATE /DOWNLOAD download_openjdk_12<br><br>
bitsadmin /ADDFILE download_openjdk_12 "https://download.java.net/java/GA/jdk12.0.2/e482c34c86bd4bf8b56c0b35558996b9/10/GPL/openjdk-12.0.2_windows-x64_bin.zip" $PROJECT_HOME\openjdk-12.0.2_windows-x64_bin.zip<br><br>
bitsadmin /RESUME download_openjdk_12<br>
bitsadmin /INFO download_openjdk_12 /VERBOSE<br>
bitsadmin /COMPLETE download_openjdk_12<br>

Expand-Archive -Path "$PROJECT_HOME\openjdk-12.0.2_windows-x64_bin.zip" -DestinationPath ".\"<br>

$env:JAVA_HOME = "$PROJECT_HOME\jdk-12.0.2"<br>
$env:Path = "$env:JAVA_HOME\bin" + ";$env:Path"<br>

## Install Scala<br>
Compatibility Reference -- https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html<br>

bitsadmin /CREATE /DOWNLOAD download_scala_2_13_4<br>
bitsadmin /ADDFILE download_scala_2_13_4 "https://downloads.lightbend.com/scala/2.13.4/scala-2.13.4.msi" $PROJECT_HOME\scala-2.13.4.msi<br>
bitsadmin /RESUME download_scala_2_13_4<br>
bitsadmin /INFO download_scala_2_13_4 /VERBOSE<br>
bitsadmin /COMPLETE download_scala_2_13_4<br>

.\scala-2.13.4.msi<br>

$env:SCALA_HOME =  "C:\Program Files (x86)\scala"<br>
$env:Path = "$env:SCALA_HOME\bin" + ";$env:Path"<br>

## Build a Scala skeleton project using a downloaded Maven plugin<br>
Reference -- http://maven.apache.org/guides/getting-started/maven-in-five-minutes.html<br>

bitsadmin /CREATE /DOWNLOAD download_maven<br>
bitsadmin /ADDFILE download_maven "https://mirrors.ocf.berkeley.edu/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.zip" $PROJECT_HOME\apache-maven-3.6.3-bin.zip<br>
bitsadmin /RESUME download_maven<br>
bitsadmin /INFO download_maven /VERBOSE<br>
bitsadmin /COMPLETE download_maven<br>

Expand-Archive -Path "$PROJECT_HOME\apache-maven-3.6.3-bin.zip" -DestinationPath ".\"<br>

$env:Path += ";$PROJECT_HOME\apache-maven-3.6.3\bin"<br>
mvn -v<br>

$APP_NAME="jms-connector-app"<br>
mkdir $APP_NAME<br>

mvn archetype:generate "-DarchetypeGroupId=net.alchim31.maven" "-DarchetypeArtifactId=scala-archetype-simple" "-DartifactId=$APP_NAME" "-DgroupId=com.statisticalfx.jms" "-DinteractiveMode=false"<br>

cd $APP_NAME<br>

## Change pom.xml<br>
Edit the Java version in <maven.compiler.source>, using Java 12 in this example<br>
A massive jar is built with all of the dependencies self-contained including the Scala runtime<br>
mvn package<br>

## How to initialize environment<br>
powershell<br>
$PROJECT_HOME="C:\Users\nomea\Documents\Projects\jms"<br>
$env:JAVA_HOME = "$PROJECT_HOME\jdk-12.0.2"<br>
$env:Path = "$env:JAVA_HOME\bin" + ";$env:Path"<br>
$env:SCALA_HOME =  "C:\Program Files (x86)\scala"<br>
$env:Path = "$env:SCALA_HOME\bin" + ";$env:Path"<br>
$env:Path += ";$PROJECT_HOME\apache-maven-3.6.3\bin"<br>
$APP_NAME="jms-connector-app"<br>

## How to build<br>
cd $PROJECT_HOME\$APP_NAME<br>
mvn package<br>

## How to run<br>
Create messages in AMQ for testing<br>
java -cp target/jms-connector-app-1.0-SNAPSHOT-jar-with-dependencies.jar com.statisticalfx.jms.OneMessagePerFileInDirectoryApp<br><br>
Write messages from AMQ into S3<br>
java -cp target/jms-connector-app-1.0-SNAPSHOT-jar-with-dependencies.jar com.statisticalfx.jms.ProcessJmsQueueApp<br>

