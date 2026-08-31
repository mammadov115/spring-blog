.PHONY: docker-dev-up docker-dev-stop docker-prod-up docker-prod-stop spring-run spring-build docker-dev-logs docker-prod-logs clean


docker-dev-up:
	docker compose -f docker/compose.dev.yml --env-file project/.env.dev up -d


docker-dev-stop:
	docker compose -f docker/compose.dev.yml down


docker-prod-up:
	docker compose -f docker/compose.prod.yml --env-file project/.env.prod up -d --build


docker-prod-stop:
	docker compose -f docker/compose.prod.yml down


spring-run:
	export $$(cat project/.env.dev | xargs) && ./mvnw spring-boot:run



spring-build:
	./mvnw package -DskipTests


docker-dev-logs:
	docker compose -f docker/compose.dev.yml logs -f

docker-prod-logs:
	docker compose -f docker/compose.prod.yml logs -f


clean:
	./mvnw clean
	docker compose -f docker/compose.dev.yml down -v