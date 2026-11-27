@echo off
cd /d "%~dp0"
echo Running Vertex Banking Automated Test Suite (T01 - T18)...
java -cp "bin;lib/*" test.VertexTestRunner
pause
