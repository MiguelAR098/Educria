FROM mcr.microsoft.com/mssql/server:2022-CU28-ubuntu-22.04
COPY banco.sh criar-banco.sql validar-banco.sql /opt/educria/
ENTRYPOINT ["bash", "/opt/educria/banco.sh", "iniciar"]
