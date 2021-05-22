DROP TABLE IF EXISTS public.cargos_per_container;

CREATE TABLE IF NOT EXISTS public.cargos_per_container(
    containerId VARCHAR(255) NOT NULL,
    cargos BIGINT NOT NULL,
    PRIMARY KEY (containerId));