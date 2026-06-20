@echo off
:: ============================================
::  Shared environment config for all scripts
::  Edit these paths for your machine
:: ============================================

if not defined JAVA_HOME set JAVA_HOME=E:\Prog\Java\jdk-17

:: Proxy settings (leave empty if not behind a proxy)
set PROXY=http://proxy-server.bms.com:8080
