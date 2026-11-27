@echo off
cd /d "%~dp0"
echo Starting Vertex Banking System...
java -cp "bin;lib/*" main.VertexBankingApp
pause
