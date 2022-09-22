-- liquibase formatted sql

-- changeset golenevav:1
CREATE TABLE notification_task
(
    id           SERIAL PRIMARY KEY,
    chat_id      BIGINT       NOT NULL,
    notification VARCHAR(255) NOT NULL,
    timestamp    TIMESTAMP    NOT NULL
);

ALTER TABLE notification_task
    owner to "Alexey";
