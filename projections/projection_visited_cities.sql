CREATE TABLE IF NOT EXISTS public.visited_cities(
    city VARCHAR(255) NOT NULL,
    count BIGINT NOT NULL,
    PRIMARY KEY (city));