FROM docker.elastic.co/beats/filebeat:8.9.0
COPY filebeat.yml /usr/share/filebeat/filebeat.yml
USER root
RUN chown root:filebeat /usr/share/filebeat/filebeat.yml
RUN chmod go-w /usr/share/filebeat/filebeat.yml
ENTRYPOINT ["/usr/share/filebeat/filebeat", "-e"]
