IF DB_NAME() NOT LIKE N'educria[_]sqlserver[_]teste[_]%'
    OR NOT EXISTS (SELECT 1 FROM sys.extended_properties
                   WHERE class=0 AND name=N'EducriaAmbiente' AND CONVERT(nvarchar(30),value)=N'descartavel')
    THROW 51034, N'O dicionário acadêmico exige banco descartável identificado.', 1;

-- Somente metadados do modelo físico. A tabela de controle do Flyway fica fora do domínio.
SELECT COUNT(DISTINCT t.object_id) AS tabelas, COUNT_BIG(*) AS atributos
FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id
WHERE SCHEMA_NAME(t.schema_id)=N'dbo' AND t.name<>N'flyway_schema_history';

-- Cada regra composta é identificada pelo nome do objeto.
SELECT t.name AS tabela, c.name AS coluna,
       CONCAT(ty.name, CASE WHEN ty.name IN (N'nvarchar',N'varchar') THEN
           CONCAT(N'(', CASE WHEN c.max_length=-1 THEN N'max'
                            ELSE CONVERT(nvarchar(8),c.max_length / CASE WHEN ty.name=N'nvarchar' THEN 2 ELSE 1 END) END,N')')
           WHEN ty.name IN (N'datetime2',N'datetimeoffset') THEN CONCAT(N'(',c.scale,N')') END) AS tipo,
       CASE WHEN c.is_nullable=1 THEN N'Sim' ELSE N'Não' END AS aceita_null,
       CONCAT(CASE WHEN c.is_identity=1 THEN CONCAT(N'IDENTITY(',CONVERT(bigint,ic.seed_value),N',',
                                                    CONVERT(bigint,ic.increment_value),N')') END,
              dc.definition) AS valor_padrao_ou_identidade,
       COALESCE(indices.regras,N'') AS pk_unique,
       COALESCE(fks.regras,N'') AS referencias,
       COALESCE(cks.regras,N'') AS checks
FROM sys.tables t
JOIN sys.columns c ON c.object_id=t.object_id
JOIN sys.types ty ON ty.user_type_id=c.user_type_id
LEFT JOIN sys.identity_columns ic ON ic.object_id=t.object_id AND ic.column_id=c.column_id
LEFT JOIN sys.default_constraints dc ON dc.object_id=c.default_object_id
OUTER APPLY (
    SELECT STRING_AGG(CONVERT(nvarchar(max),CONCAT(CASE WHEN i.is_primary_key=1 THEN N'PK ' ELSE N'UNIQUE ' END,
           CASE WHEN i.is_primary_key=0 AND i.is_unique_constraint=0 THEN i.name ELSE N'(' + chaves.colunas + N')' END,
           CASE WHEN i.has_filter=1 THEN CONCAT(N' WHERE ',i.filter_definition) END)) COLLATE DATABASE_DEFAULT,N'; ') AS regras
    FROM sys.indexes i
    JOIN sys.index_columns k ON k.object_id=i.object_id AND k.index_id=i.index_id
    CROSS APPLY (
        SELECT STRING_AGG(CONVERT(nvarchar(max),cc.name) COLLATE DATABASE_DEFAULT,N', ')
               WITHIN GROUP (ORDER BY kc.key_ordinal) AS colunas
        FROM sys.index_columns kc JOIN sys.columns cc ON cc.object_id=kc.object_id AND cc.column_id=kc.column_id
        WHERE kc.object_id=i.object_id AND kc.index_id=i.index_id AND kc.key_ordinal>0
    ) chaves
    WHERE i.object_id=t.object_id AND i.is_unique=1 AND k.column_id=c.column_id AND k.key_ordinal>0
) indices
OUTER APPLY (
    SELECT STRING_AGG(CONVERT(nvarchar(max),CONCAT(OBJECT_NAME(k.referenced_object_id),N'.',
           COL_NAME(k.referenced_object_id,k.referenced_column_id), N' [',fk.delete_referential_action_desc,N']',
           CASE WHEN qtd.componentes>1 THEN CONCAT(N' ',fk.name) END)) COLLATE DATABASE_DEFAULT,N'; ') AS regras
    FROM sys.foreign_key_columns k JOIN sys.foreign_keys fk ON fk.object_id=k.constraint_object_id
    CROSS APPLY (SELECT COUNT(*) AS componentes FROM sys.foreign_key_columns k2
                 WHERE k2.constraint_object_id=k.constraint_object_id) qtd
    WHERE k.parent_object_id=t.object_id AND k.parent_column_id=c.column_id
) fks
OUTER APPLY (
    SELECT STRING_AGG(CONVERT(nvarchar(max),ch.definition) COLLATE DATABASE_DEFAULT,N'; ') AS regras
    FROM sys.check_constraints ch
    WHERE ch.parent_object_id=t.object_id AND (ch.parent_column_id=c.column_id OR
        (ch.parent_column_id=0 AND EXISTS (SELECT 1 FROM sys.sql_expression_dependencies d
            WHERE d.referencing_id=ch.object_id AND d.referenced_id=t.object_id AND d.referenced_minor_id=c.column_id)))
) cks
WHERE SCHEMA_NAME(t.schema_id)=N'dbo' AND t.name<>N'flyway_schema_history'
ORDER BY t.name,c.column_id;

-- Lista completa dos CHECKs de tabela, inclusive os que combinam colunas.
SELECT OBJECT_NAME(parent_object_id) AS tabela, name AS restricao, definition AS regra
FROM sys.check_constraints WHERE parent_column_id=0 ORDER BY tabela, restricao;

-- Chaves, inclusões e filtros dos índices explícitos, inclusive os índices de pontuação e grupos.
SELECT t.name AS tabela,i.name AS indice,i.is_unique AS unico,i.filter_definition AS filtro,
       c.name AS coluna,k.key_ordinal AS ordem,k.is_descending_key AS decrescente,k.is_included_column AS incluida
FROM sys.tables t JOIN sys.indexes i ON i.object_id=t.object_id
JOIN sys.index_columns k ON k.object_id=i.object_id AND k.index_id=i.index_id
JOIN sys.columns c ON c.object_id=k.object_id AND c.column_id=k.column_id
WHERE t.schema_id=SCHEMA_ID(N'dbo') AND t.name<>N'flyway_schema_history'
  AND i.is_primary_key=0 AND i.is_unique_constraint=0 AND i.name IS NOT NULL
ORDER BY t.name,i.name,k.is_included_column,k.key_ordinal,k.index_column_id;
