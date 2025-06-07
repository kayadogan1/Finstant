#!/bin/sh

echo "Index init başlatılıyor"
until curl -s http://elasticsearch:9200/_cluster/health | grep -q '"status":"yellow"'; do
  echo "Elasticsearch hazır değil, bekleniyor..."
  sleep 5
done

echo "Elasticsearch hazır, index oluşturuluyor"
curl -X PUT http://elasticsearch:9200/rate-index -H "Content-Type: application/json" --data-binary @/rate-index.json
echo "Index oluşturma işlemi tamamlandı"

echo "Kibana import servisi başlatıldı"
until curl -s http://kibana:5601/api/status | grep -q '"level":"available"'; do
  echo "Kibana hazır değil, bekleniyor..."
  sleep 5
done

echo "Kibana hazır, saved objects import ediliyor"
curl -X POST http://kibana:5601/api/saved_objects/_import?overwrite=true \
  -H "kbn-xsrf: true" \
  -F file=@/finstant-dashboard.ndjson

echo "Import işlemi tamamlandı"