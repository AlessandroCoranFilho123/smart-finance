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
    comentario     TEXT    NOT NULL DEFAULT '',
    valor_centavos INTEGER NOT NULL,
    tipo           TEXT    NOT NULL,
    data           TEXT    NOT NULL,
    meta_id        TEXT,
    categoria      TEXT,
    FOREIGN KEY (meta_id) REFERENCES meta (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_transacao_data_desc ON transacao (data DESC);
CREATE INDEX IF NOT EXISTS idx_transacao_tipo_data ON transacao (tipo, data);
CREATE INDEX IF NOT EXISTS idx_transacao_categoria_data ON transacao (categoria, data);
CREATE INDEX IF NOT EXISTS idx_transacao_meta_id ON transacao (meta_id);
CREATE INDEX IF NOT EXISTS idx_meta_nome ON meta (nome COLLATE NOCASE);

CREATE VIRTUAL TABLE IF NOT EXISTS transacao_fts USING fts5(
    descricao,
    comentario,
    categoria,
    tipo,
    data,
    content='transacao',
    content_rowid='rowid',
    tokenize='unicode61 remove_diacritics 2'
);

CREATE TRIGGER IF NOT EXISTS transacao_ai AFTER INSERT ON transacao BEGIN
    INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
    VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
END;

CREATE TRIGGER IF NOT EXISTS transacao_ad AFTER DELETE ON transacao BEGIN
    INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
    VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
END;

CREATE TRIGGER IF NOT EXISTS transacao_au AFTER UPDATE ON transacao BEGIN
    INSERT INTO transacao_fts(transacao_fts, rowid, descricao, comentario, categoria, tipo, data)
    VALUES ('delete', old.rowid, old.descricao, old.comentario, COALESCE(old.categoria, ''), old.tipo, old.data);
    INSERT INTO transacao_fts(rowid, descricao, comentario, categoria, tipo, data)
    VALUES (new.rowid, new.descricao, new.comentario, COALESCE(new.categoria, ''), new.tipo, new.data);
END;

INSERT INTO transacao_fts(transacao_fts) VALUES ('rebuild');
