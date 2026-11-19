INSERT INTO materias (nome) VALUES ('Português'), ('Ciências')
ON CONFLICT (nome) DO NOTHING;
