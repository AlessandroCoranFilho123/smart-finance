PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS meta
(
    id             TEXT PRIMARY KEY,
    nome           TEXT    NOT NULL,
    alvo_centavos  INTEGER,
    atual_centavos INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS transacao
(
    id             TEXT PRIMARY KEY,
    descricao      TEXT    NOT NULL,
    valor_centavos INTEGER NOT NULL,
    tipo           TEXT    NOT NULL,
    data           TEXT    NOT NULL,
    meta_id        TEXT,
    categoria      TEXT,
    FOREIGN KEY (meta_id) REFERENCES meta (id) ON DELETE SET NULL
);
