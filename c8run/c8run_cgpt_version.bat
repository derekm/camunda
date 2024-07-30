@echo off

REM set constants
set CAMUNDA_VERSION=8.6.0-alpha3
set CAMUNDA_CONNECTORS_VERSION=8.6.0-alpha3
set ELASTICSEARCH_VERSION=8.13.4
set EXPECTED_JAVA_VERSION=21

set BASEDIR=%~dp0
set PARENTDIR=%BASEDIR%\..
set DEPLOYMENT_DIR=%PARENTDIR%\configuration\resources
set WEBAPPS_PATH=%BASEDIR%\webapps\
set REST_PATH=%BASEDIR%\rest\
set SWAGGER_PATH=%BASEDIR%\swaggerui
set EXAMPLE_PATH=%BASEDIR%\example
set PID_PATH=%BASEDIR%\run.pid
set ELASTIC_PID_PATH=%BASEDIR%\elasticsearch.pid
set CONNECTORS_PID_PATH=%BASEDIR%\connectors.pid
set OPTIONS_HELP=Options:
REM   --webapps    - Enables the Camunda Platform Webapps
REM   --rest       - Enables the REST API
REM   --swaggerui  - Enables the Swagger UI
REM   --example    - Enables the example application
REM   --config     - Applies the specified configuration file
REM   --detached   - Starts Camunda Run as a detached process

REM Configuration file defaults overridden because upstream config doesn't export to elasticsearch
set ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_CLASSNAME=io.camunda.zeebe.exporter.ElasticsearchExporter
set ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_URL=http://localhost:9200
set ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_INDEX_PREFIX=zeebe-record

for /f "tokens=*" %%i in ('wmic os get osarchitecture') do set architectureRaw=%%i
if "%architectureRaw%"=="64-bit" (
    set architecture=x86_64
) else (
    set architecture=aarch64
)

for /f "tokens=*" %%i in ('ver') do set unameOut=%%i
if "%unameOut%"=="Microsoft Windows [Version 10.0.19042.1052]" (
    set machine=Windows
) else (
    set machine=UNKNOWN
)

REM set environment parameters
set optionalComponentChosen=false
set restChosen=false
set swaggeruiChosen=false
set detachProcess=false
set classPath=%PARENTDIR%\configuration\userlib\,%PARENTDIR%\configuration\keystore\

if "%1"=="start" (
    shift
    REM setup the JVM
    if "%JAVA%"=="" (
        if not "%JAVA_HOME%"=="" (
            echo Setting JAVA property to "%JAVA_HOME%\bin\java"
            set JAVA="%JAVA_HOME%\bin\java"
        ) else (
            echo JAVA_HOME is not set. Unexpected results may occur.
            echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
            set JAVA=java
        )
    )

    for /f "tokens=2 delims==" %%i in ('%JAVA% -version 2>&1') do set JAVA_VERSION=%%i
    echo Java version is %JAVA_VERSION%
    if "%JAVA_VERSION%" lss "%EXPECTED_JAVA_VERSION%" (
        echo You must use at least JDK %EXPECTED_JAVA_VERSION% to start Camunda Platform Run.
        exit /b 1
    )

    if not "%JAVA_OPTS%"=="" (
        echo JAVA_OPTS: %JAVA_OPTS%
    )

    REM inspect arguments
    :inspectArgs
    if not "%1"=="" (
        if "%1"=="--webapps" (
            set optionalComponentChosen=true
            set classPath=%WEBAPPS_PATH%,%classPath%
            echo WebApps enabled
        ) else if "%1"=="--rest" (
            set optionalComponentChosen=true
            set restChosen=true
            set classPath=%REST_PATH%,%classPath%
            echo REST API enabled
        ) else if "%1"=="--swaggerui" (
            set optionalComponentChosen=true
            set swaggeruiChosen=true
            set classPath=%SWAGGER_PATH%,%classPath%
            echo Swagger UI enabled
        ) else if "%1"=="--example" (
            set optionalComponentChosen=true
            set classPath=%EXAMPLE_PATH%,%classPath%
            echo Invoice Example included - needs to be enabled in application configuration as well
        ) else if "%1"=="--config" (
            shift
            if "%1"=="" (
                echo %OPTIONS_HELP%
                exit /b 0
            )
            set configuration=%1%
        ) else if "%1"=="--detached" (
            set detachProcess=true
            echo Camunda Run will start in the background. Use the shutdown.bat script to stop it
        ) else if "%1"=="--help" (
            echo %OPTIONS_HELP%
            exit /b 0
        ) else (
            exit /b 1
        )
        shift
        goto inspectArgs
    )

    REM Retrieve elasticsearch
    mkdir "%PARENTDIR%\log"
    if not exist "elasticsearch-%ELASTICSEARCH_VERSION%" (
        powershell -Command "Invoke-WebRequest -Uri 'https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-%ELASTICSEARCH_VERSION%-%PLATFORM%-%architecture%.zip' -OutFile 'elasticsearch.zip'"
        powershell -Command "Expand-Archive -Path 'elasticsearch.zip' -DestinationPath '%PARENTDIR%'"
    )

    if not exist "camunda-zeebe-%CAMUNDA_VERSION%" (
        powershell -Command "Invoke-WebRequest -Uri 'https://github.com/camunda/camunda/releases/download/%CAMUNDA_VERSION%/camunda-zeebe-%CAMUNDA_VERSION%.zip' -OutFile 'camunda.zip'"
        powershell -Command "Expand-Archive -Path 'camunda.zip' -DestinationPath '%PARENTDIR%'"
    )

    set connectorsFileName=connector-runtime-bundle-%CAMUNDA_CONNECTORS_VERSION%-with-dependencies.jar
    if not exist "%connectorsFileName%" (
        powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/io/camunda/connector/connector-runtime-bundle/%CAMUNDA_CONNECTORS_VERSION%/%connectorsFileName%' -OutFile '%connectorsFileName%'"
    )

    REM limit the java heapspace used by ElasticSearch to 1GB
    set ES_JAVA_OPTS=-Xms1g -Xmx1g

    echo.
    echo Starting Elasticsearch %ELASTICSEARCH_VERSION%...
    echo (Hint: you can find the log output in the 'elasticsearch.log' file in the 'log' folder of your distribution.)
    echo.
    set ELASTICSEARCH_LOG_FILE=%PARENTDIR%\log\elasticsearch.log
    powershell -Command "Start-Process -NoNewWindow -FilePath '%PARENTDIR%\elasticsearch-%ELASTICSEARCH_VERSION%\bin\elasticsearch.bat' -ArgumentList '-E', 'xpack.ml.enabled=false', '-E', 'xpack.security.enabled=false' -RedirectStandardOutput '%ELASTICSEARCH_LOG_FILE%' -RedirectStandardError '%ELASTICSEARCH_LOG_FILE%' -PassThru" > "%ELASTIC_PID_PATH%"

    REM check if a Camunda Run instance is already in operation
    if exist "%PID_PATH%" (
        echo.
        echo A Camunda Run instance is already in operation (process id %PID_PATH%).

        echo Please stop it or remove the file %PID_PATH%.
        exit /b 1
    )

    set classPath=%classPath:~0,-1%

    if not "%configuration%"=="" (
        if "%configuration:~0,1%"=="\" (
            set extraArgs=--spring.config.location=%configuration%
        ) else (
            set extraArgs=--spring.config.location=%cd%\%configuration%
        )
    )

    if "%detachProcess%"=="true" (
        cd /d %PARENTDIR%\camunda-zeebe-%CAMUNDA_VERSION%
        echo %JAVA% -cp "%PARENTDIR%\*,%PARENTDIR%\custom_connectors\*,.\camunda-zeebe-%CAMUNDA_VERSION%\lib\*" "io.camunda.connector.runtime.app.ConnectorRuntimeApplication" --spring.config.location=./connectors-application.properties >> %PARENTDIR%\log\connectors.log 2>>&1 &
        start /b %JAVA% -cp "%PARENTDIR%\*,%PARENTDIR%\custom_connectors\*,.\camunda-zeebe-%CAMUNDA_VERSION%\lib\*" "io.camunda.connector.runtime.app.ConnectorRuntimeApplication" --spring.config.location=./connectors-application.properties >> %PARENTDIR%\log\connectors.log 2>>&1 &
        echo %! > "%CONNECTORS_PID_PATH%"
        start /b cmd /c "%PARENTDIR%\camunda-zeebe-%CAMUNDA_VERSION%\bin\camunda %extraArgs% >> %PARENTDIR%\log\camunda.log 2>>&1"
        echo %! > "%PID_PATH%"
    ) else (
        %JAVA% -cp "%PARENTDIR%\*,%PARENTDIR%\custom_connectors\*,.\camunda-zeebe-%CAMUNDA_VERSION%\lib\*" "io.camunda.connector.runtime.app.ConnectorRuntimeApplication" --spring.config.location=./connectors-application.properties >> %PARENTDIR%\log\connectors.log 2>>&1 &
        echo %! > "%CONNECTORS_PID_PATH%"
        cd /d %PARENTDIR%\camunda-zeebe-%CAMUNDA_VERSION%
        %PARENTDIR%\camunda-zeebe-%CAMUNDA_VERSION%\bin\camunda %extraArgs% 2>&1 | tee %PARENTDIR%\log\camunda.log
    )

) else if "%1"=="stop" (

    if exist "%PID_PATH%" (
        REM stop Camunda Run if the process is still running
        taskkill /F /PID %PID_PATH%
        REM remove process ID file
        del "%PID_PATH%"
        echo Camunda Run is shutting down.
    ) else (
        echo There is no instance of Camunda Run to shut down.
    )
    if exist "%ELASTIC_PID_PATH%" (
        REM stop Elasticsearch if the process is still running
        taskkill /F /PID %ELASTIC_PID_PATH%
        REM remove process ID file
        del "%ELASTIC_PID_PATH%"
        echo Elasticsearch is shutting down.
    ) else (
        echo There is no instance of Elasticsearch to shut down.
    )
    if exist "%CONNECTORS_PID_PATH%" (
        taskkill /F /PID %CONNECTORS_PID_PATH%
        del "%CONNECTORS_PID_PATH%"
        echo Connectors is shutting down.
    ) else (
        echo There is no instance of Connectors to shut down.
    )

) else if "%1"=="" (
    echo Usage: run.bat [start^|stop] (options...) %OPTIONS_HELP%
)