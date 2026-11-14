@echo off
echo ========================================================
echo Compiling Vertex Banking System...
echo ========================================================

if not exist bin mkdir bin

javac -d bin -sourcepath src src/main/VertexBankingApp.java src/test/VertexTestRunner.java
if %ERRORLEVEL% EQU 0 (
    copy /y src\vertex_banking.properties bin\vertex_banking.properties >nul
    echo Compilation Successful! Output classes generated in bin/
) else (
    echo Compilation Failed with error code %ERRORLEVEL%
)
pause
