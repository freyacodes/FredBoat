FROM postgres:10.0
ENV POSTGRES_USER fredboat
COPY postgres-healthcheck.sh /usr/local/bin/
COPY initdb.sh /docker-entrypoint-initdb.d/
HEALTHCHECK CMD ["postgres-healthcheck.sh"]
