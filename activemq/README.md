powershell <br>

$PROJECT_HOME="C:\Users\nomea\Documents\Projects\jms" <br>
$ARTEMIS_HOME="$PROJECT_HOME\activemq-artemis" <br>

cd $PROJECT_HOME <br>

# Clone Artemis Git repo if needed <br>
Windows uses LF, Unix style is CRLF for line endings <br>
Reference -- https://stackoverflow.com/questions/10418975/how-to-change-line-ending-settings <br>

git config --global core.autocrlf input <br>
git clone https://github.com/apache/activemq-artemis <br>

mkdir $ARTEMIS_HOME\docker <br>
Copy-Item -Path $PROJECT_HOME\activemq-artemis\artemis-docker\Dockerfile-* -Destination $ARTEMIS_HOME\docker <br>
Copy-Item -Path $PROJECT_HOME\activemq-artemis\artemis-docker\docker-run.sh -Destination $ARTEMIS_HOME\docker <br>

bitsadmin /CREATE /DOWNLOAD download_artemis <br>
bitsadmin /ADDFILE download_artemis "https://www.apache.org/dyn/closer.cgi?filename=activemq/activemq-artemis/2.16.0/apache-artemis-2.16.0-bin.zip&action=download" $ARTEMIS_HOME\docker\apache-artemis-2.16.0-bin.zip <br>
bitsadmin /RESUME download_artemis <br>
bitsadmin /INFO download_artemis /VERBOSE <br>
bitsadmin /COMPLETE download_artemis <br>

Expand-Archive -Path "$ARTEMIS_HOME\docker\apache-artemis-2.16.0-bin.zip" -DestinationPath ".\" <br>

# Build Docker image <br>
Change the Dockerfile to use backward slashes for Windows paths for the docker-run.sh reference <br>
Change "ADD . /opt/activemq-artemis" To "ADD ./apache-artemis-2.16.0 /opt/activemq-artemis" <br>
Change "COPY ./docker/docker-run.sh /" To "COPY docker-run.sh /" <br>

cd $ARTEMIS_HOME\docker <br>
docker build -f Dockerfile-centos -t artemis-centos . <br>

# Run the Docker image <br>
docker run --rm -it -p 61616:61616 -p 8161:8161 artemis-centos <br>

