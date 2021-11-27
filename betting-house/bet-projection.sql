DROP TABLE IF EXISTS public.bet_wallet_market;

CREATE TABLE IF NOT EXISTS public.bet_wallet_market(
    betId VARCHAR(255) NOT NULL,
    walletId VARCHAR(255) NOT NULL,
    marketId VARCHAR(255) NOT NULL,
    odds REAL NOT NULL,
    stake INTEGER NOT NULL,
    result INTEGER NOT NULL,
    PRIMARY KEY (betId));