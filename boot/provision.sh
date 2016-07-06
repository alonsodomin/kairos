#!/usr/bin/env bash

set -e

function install_devtools() {
    echo "Installing OpenJDK 8..."
    sudo yum install -y java-1.8.0-openjdk-devel &> /dev/null

    echo "Installing nodejs..."
    #sudo apt-get install -y nodejs npm &> /dev/null
    sudo yum install -y gcc-c++ make &> /dev/null
    sudo curl --silent --location https://rpm.nodesource.com/setup_4.x | bash - &> /dev/null
    sudo yum install -y nodejs &> /dev/null

    echo "Installing PhantomJS..."
    sudo npm install -g phantomjs-prebuilt &> /dev/null

    echo "Installing SBT..."
    curl --silent https://bintray.com/sbt/rpm/rpm > bintray-sbt-rpm.repo
    sudo mv bintray-sbt-rpm.repo /etc/yum.repos.d/
    sudo yum install -y sbt &> /dev/null

    echo "Installing SASS..."
    sudo yum install -y ruby-devel &> /dev/null
    sudo gem install compass &> /dev/null

    echo "DEV tools installed into host VM! Moving on..."
    touch ~/.devtools_provisioned
}

########## PROGRAM STARTS HERE ##############

echo "Updating VM..."
sudo yum update -y &> /dev/null

# Dev tools take ages to install, only do so on first build
test -f ~/.devtools_provisioned || install_devtools
