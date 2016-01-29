@echo off
REM
REM Licensed to the Apache Software Foundation (ASF) under one
REM or more contributor license agreements.  See the NOTICE file
REM distributed with this work for additional information
REM regarding copyright ownership.  The ASF licenses this file
REM to you under the Apache License, Version 2.0 (the
REM "License"); you may not use this file except in compliance
REM with the License.  You may obtain a copy of the License at
REM
REM   http://www.apache.org/licenses/LICENSE-2.0
REM
REM Unless required by applicable law or agreed to in writing,
REM software distributed under the License is distributed on an
REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
REM KIND, either express or implied.  See the License for the
REM specific language governing permissions and limitations
REM under the License.
REM

REM Install the MQ Light API to the local Maven repository
call mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=mqlight-api-%version%.jar -DpomFile=mqlight/pom.xml

REM Install the MQ Light API samples to the local Maven repository
call mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=mqlight-api-samples-%version%.jar -DpomFile=mqlight-samples/pom.xml

REM Install the MQ Light project to the local Maven repository
REM This is necessary in order for other Maven projects depending on MQ Light to be able to build in offline mode 
call mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=pom.xml -DpomFile=pom.xml

REM Install the required dependencies to the local Maven repository
call mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:get -Dartifact=com.ibm.mqlight:mqlight-api:%version%
