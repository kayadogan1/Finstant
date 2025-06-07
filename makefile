.PHONY: up

up:
	mvn package
	docker-compose up
