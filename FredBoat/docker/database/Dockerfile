FROM postgres:10.0
ENV POSTGRES_USER fredboat
COPY initdb.sh /usr/local/bin/
COPY run.sh /usr/local/bin/
ENTRYPOINT ["/bin/bash", "run.sh"]
