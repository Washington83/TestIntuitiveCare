-- =====================================================================
-- PARTE 3: TESTE DE BANCO DE DADOS E ANÁLISE
-- =====================================================================
-- Banco de dados: PostgreSQL 14+
-- 
-- TRADE-OFF PRINCIPAL: Normalização vs Desnormalização
-- Decisão: Uso de DUAS ESTRATÉGIAS
-- - Tabelas normalizadas para integridade e evitar redundância
-- - Tabela agregada desnormalizada para performance em consultas analíticas
-- 
-- JUSTIFICATIVA:
-- - Volume de dados esperado: Médio (milhares de registros, não milhões)
-- - Frequência de atualização: Trimestral (baixa)
-- - Queries analíticas: Frequentes
-- - CONCLUSÃO: Normalização para dados operacionais + materialização 
--   para análises é o melhor dos dois mundos
-- =====================================================================

-- =====================================================================
-- 3.2: CRIAÇÃO DAS TABELAS (DDL)
-- =====================================================================

-- Tabela de Operadoras (Cadastro)
-- Armazena dados cadastrais únicos de cada operadora
CREATE TABLE IF NOT EXISTS operadoras (
    cnpj VARCHAR(18) PRIMARY KEY,  -- Formato: XX.XXX.XXX/XXXX-XX
    razao_social VARCHAR(200) NOT NULL,
    registro_ans VARCHAR(20),
    modalidade VARCHAR(50),
    uf CHAR(2),
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint para UF válida
    CONSTRAINT ck_uf CHECK (uf ~ '^[A-Z]{2}$' OR uf IS NULL)
);

-- Índices para melhorar performance de buscas
CREATE INDEX idx_operadoras_razao_social ON operadoras(razao_social);
CREATE INDEX idx_operadoras_uf ON operadoras(uf);
CREATE INDEX idx_operadoras_registro_ans ON operadoras(registro_ans);

-- Comentários para documentação
COMMENT ON TABLE operadoras IS 'Cadastro de operadoras de planos de saúde';
COMMENT ON COLUMN operadoras.cnpj IS 'CNPJ formatado da operadora (PK)';
COMMENT ON COLUMN operadoras.registro_ans IS 'Número de registro na ANS';


-- Tabela de Despesas Consolidadas
-- Armazena todas as despesas por trimestre
--
-- TRADE-OFF: Tipo de dado para valores monetários
-- Decisão: DECIMAL(15,2)
-- - DECIMAL: Precisão exata (não tem erros de arredondamento como FLOAT)
-- - 15 dígitos: Suporta até 999 trilhões (suficiente para qualquer operadora)
-- - 2 casas decimais: Centavos
-- JUSTIFICATIVA: Valores financeiros NUNCA devem usar FLOAT devido a 
-- imprecisão. DECIMAL garante cálculos exatos.
CREATE TABLE IF NOT EXISTS despesas_consolidadas (
    id SERIAL PRIMARY KEY,
    cnpj VARCHAR(18) NOT NULL,
    razao_social VARCHAR(200) NOT NULL,
    trimestre SMALLINT NOT NULL,  -- 1, 2, 3, ou 4
    ano SMALLINT NOT NULL,        -- YYYY
    valor_despesas DECIMAL(15,2) NOT NULL,
    observacoes TEXT,
    data_importacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key para operadoras (com ON DELETE CASCADE)
    CONSTRAINT fk_despesas_operadora 
        FOREIGN KEY (cnpj) 
        REFERENCES operadoras(cnpj) 
        ON DELETE CASCADE,
    
    -- Constraints de validação
    CONSTRAINT ck_trimestre CHECK (trimestre BETWEEN 1 AND 4),
    CONSTRAINT ck_ano CHECK (ano BETWEEN 2000 AND 2100),
    CONSTRAINT ck_valor_positivo CHECK (valor_despesas >= 0),
    
    -- Índice único composto: uma operadora não pode ter despesa duplicada no mesmo período
    CONSTRAINT uk_despesa_periodo UNIQUE (cnpj, ano, trimestre)
);

-- Índices para performance
CREATE INDEX idx_despesas_periodo ON despesas_consolidadas(ano, trimestre);
CREATE INDEX idx_despesas_cnpj ON despesas_consolidadas(cnpj);
CREATE INDEX idx_despesas_valor ON despesas_consolidadas(valor_despesas DESC);

COMMENT ON TABLE despesas_consolidadas IS 'Despesas trimestrais das operadoras';
COMMENT ON COLUMN despesas_consolidadas.valor_despesas IS 'Valor em R$ com 2 casas decimais';


-- Tabela de Dados Agregados (Desnormalizada para performance)
-- Pre-calcula agregações para acelerar consultas analíticas
CREATE TABLE IF NOT EXISTS despesas_agregadas (
    id SERIAL PRIMARY KEY,
    razao_social VARCHAR(200) NOT NULL,
    uf CHAR(2) NOT NULL,
    total_despesas DECIMAL(18,2) NOT NULL,
    media_por_trimestre DECIMAL(15,2) NOT NULL,
    desvio_padrao DECIMAL(15,2),
    numero_trimestres INTEGER NOT NULL,
    data_calculo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Índice único: uma combinação operadora+UF só pode existir uma vez
    CONSTRAINT uk_agregado UNIQUE (razao_social, uf)
);

CREATE INDEX idx_agregados_total ON despesas_agregadas(total_despesas DESC);
CREATE INDEX idx_agregados_uf ON despesas_agregadas(uf);

COMMENT ON TABLE despesas_agregadas IS 'Agregações pré-calculadas de despesas por operadora e UF';


-- =====================================================================
-- 3.3: IMPORTAÇÃO DE DADOS DOS CSVs
-- =====================================================================

-- Preparação: Limpar tabelas existentes (cuidado em produção!)
-- TRUNCATE TABLE despesas_agregadas CASCADE;
-- TRUNCATE TABLE despesas_consolidadas CASCADE;
-- TRUNCATE TABLE operadoras CASCADE;

-- IMPORTANTE: Esses comandos assumem que os CSVs estão em /tmp/
-- Ajuste o caminho conforme necessário

-- Importar dados cadastrais das operadoras
-- Origem: CSV de dados cadastrais da ANS
COPY operadoras (cnpj, razao_social, registro_ans, modalidade, uf)
FROM '/tmp/operadoras_cadastro.csv'
DELIMITER ';'
CSV HEADER
ENCODING 'UTF8';

-- Tratamento de inconsistências durante importação:
-- 1. Valores NULL em campos obrigatórios: Usar COALESCE na query
-- 2. Strings em campos numéricos: Usar NULLIF + CAST
-- 3. Datas em formatos inconsistentes: Usar TO_DATE com múltiplos formatos

-- Importar despesas consolidadas com tratamento de erros
CREATE TEMP TABLE temp_despesas (
    cnpj TEXT,
    razao_social TEXT,
    trimestre TEXT,
    ano TEXT,
    valor_despesas TEXT,
    observacoes TEXT
);

COPY temp_despesas
FROM '/tmp/consolidado_despesas.csv'
DELIMITER ';'
CSV HEADER
ENCODING 'UTF8';

-- Inserir com conversão e validação
INSERT INTO despesas_consolidadas (cnpj, razao_social, trimestre, ano, valor_despesas, observacoes)
SELECT 
    COALESCE(NULLIF(TRIM(cnpj), ''), 'INVALIDO') as cnpj,
    COALESCE(NULLIF(TRIM(razao_social), ''), 'NÃO INFORMADO') as razao_social,
    COALESCE(CAST(NULLIF(TRIM(trimestre), '') AS SMALLINT), 0) as trimestre,
    COALESCE(CAST(NULLIF(TRIM(ano), '') AS SMALLINT), 0) as ano,
    COALESCE(
        CAST(
            REPLACE(REPLACE(NULLIF(TRIM(valor_despesas), ''), '.', ''), ',', '.') 
            AS DECIMAL(15,2)
        ), 
        0.00
    ) as valor_despesas,
    observacoes
FROM temp_despesas
WHERE TRIM(cnpj) != ''  -- Ignora linhas com CNPJ vazio
ON CONFLICT (cnpj, ano, trimestre) DO NOTHING;  -- Ignora duplicatas

DROP TABLE temp_despesas;

-- Importar dados agregados
COPY despesas_agregadas (razao_social, uf, total_despesas, media_por_trimestre, desvio_padrao, numero_trimestres)
FROM '/tmp/despesas_agregadas.csv'
DELIMITER ';'
CSV HEADER
ENCODING 'UTF8';


-- =====================================================================
-- 3.4: QUERIES ANALÍTICAS
-- =====================================================================

-- ---------------------------------------------------------------------
-- QUERY 1: Top 5 operadoras com maior crescimento percentual
-- ---------------------------------------------------------------------
-- Desafio: Operadoras podem não ter dados em todos os trimestres
-- Solução: Usar apenas operadoras com dados no primeiro E último trimestre
--
-- TRADE-OFF: Como calcular crescimento?
-- Opção A: (ultimo - primeiro) / primeiro * 100
-- Opção B: Usar apenas operadoras com todos os 3 trimestres
-- Opção C: Usar média dos trimestres intermediários
-- 
-- Decisão: Opção A (mais simples e direta)
-- JUSTIFICATIVA: É a forma mais comum e intuitiva de calcular crescimento
-- Operadoras sem dados no período são automaticamente excluídas

WITH trimestres_disponiveis AS (
    -- Identifica primeiro e último trimestre no dataset
    SELECT 
        MIN(ano * 10 + trimestre) as primeiro_periodo,
        MAX(ano * 10 + trimestre) as ultimo_periodo
    FROM despesas_consolidadas
),
despesas_por_periodo AS (
    -- Pega valor do primeiro e último período de cada operadora
    SELECT 
        d.cnpj,
        d.razao_social,
        MAX(CASE 
            WHEN d.ano * 10 + d.trimestre = td.primeiro_periodo 
            THEN d.valor_despesas 
        END) as valor_primeiro,
        MAX(CASE 
            WHEN d.ano * 10 + d.trimestre = td.ultimo_periodo 
            THEN d.valor_despesas 
        END) as valor_ultimo
    FROM despesas_consolidadas d
    CROSS JOIN trimestres_disponiveis td
    GROUP BY d.cnpj, d.razao_social
    HAVING 
        -- Garante que tem dados no primeiro E no último período
        MAX(CASE WHEN d.ano * 10 + d.trimestre = td.primeiro_periodo THEN 1 END) = 1
        AND MAX(CASE WHEN d.ano * 10 + d.trimestre = td.ultimo_periodo THEN 1 END) = 1
)
SELECT 
    cnpj,
    razao_social,
    valor_primeiro as despesa_inicial,
    valor_ultimo as despesa_final,
    ROUND(
        ((valor_ultimo - valor_primeiro) / NULLIF(valor_primeiro, 0)) * 100, 
        2
    ) as crescimento_percentual
FROM despesas_por_periodo
WHERE valor_primeiro > 0  -- Evita divisão por zero e valores negativos
ORDER BY crescimento_percentual DESC
LIMIT 5;


-- ---------------------------------------------------------------------
-- QUERY 2: Distribuição de despesas por UF (Top 5 estados)
-- ---------------------------------------------------------------------
-- Desafio adicional: Calcular média por operadora em cada UF

SELECT 
    o.uf,
    COUNT(DISTINCT d.cnpj) as numero_operadoras,
    SUM(d.valor_despesas) as total_despesas,
    ROUND(
        SUM(d.valor_despesas) / NULLIF(COUNT(DISTINCT d.cnpj), 0), 
        2
    ) as media_por_operadora,
    ROUND(
        AVG(d.valor_despesas), 
        2
    ) as media_por_trimestre
FROM despesas_consolidadas d
INNER JOIN operadoras o ON d.cnpj = o.cnpj
WHERE o.uf IS NOT NULL
GROUP BY o.uf
ORDER BY total_despesas DESC
LIMIT 5;


-- ---------------------------------------------------------------------
-- QUERY 3: Operadoras acima da média em pelo menos 2 trimestres
-- ---------------------------------------------------------------------
-- TRADE-OFF: Performance vs Legibilidade
-- Opção A: Subquery correlacionada (mais legível)
-- Opção B: CTE com JOIN (mais rápido)
-- Opção C: Window functions (mais moderno)
--
-- Decisão: Opção C (Window functions)
-- JUSTIFICATIVA: Melhor performance + código limpo
-- PostgreSQL otimiza bem window functions

WITH media_geral AS (
    -- Calcula média geral de despesas
    SELECT AVG(valor_despesas) as media
    FROM despesas_consolidadas
),
operadoras_acima_media AS (
    -- Para cada registro, verifica se está acima da média
    SELECT 
        d.cnpj,
        d.razao_social,
        d.ano,
        d.trimestre,
        d.valor_despesas,
        mg.media,
        CASE 
            WHEN d.valor_despesas > mg.media THEN 1 
            ELSE 0 
        END as acima_media
    FROM despesas_consolidadas d
    CROSS JOIN media_geral mg
),
contagem_trimestres AS (
    -- Conta quantos trimestres cada operadora ficou acima da média
    SELECT 
        cnpj,
        razao_social,
        SUM(acima_media) as trimestres_acima_media,
        COUNT(*) as total_trimestres,
        ROUND(AVG(valor_despesas), 2) as media_operadora,
        (SELECT media FROM media_geral) as media_geral
    FROM operadoras_acima_media
    GROUP BY cnpj, razao_social
)
SELECT 
    cnpj,
    razao_social,
    trimestres_acima_media,
    total_trimestres,
    media_operadora,
    ROUND(media_geral, 2) as media_geral,
    ROUND(
        ((media_operadora - media_geral) / NULLIF(media_geral, 0)) * 100,
        2
    ) as percentual_acima_media
FROM contagem_trimestres
WHERE trimestres_acima_media >= 2
ORDER BY trimestres_acima_media DESC, media_operadora DESC;


-- =====================================================================
-- QUERIES AUXILIARES (Úteis para validação e análise)
-- =====================================================================

-- Verificar integridade dos dados importados
SELECT 
    'Operadoras' as tabela,
    COUNT(*) as total_registros
FROM operadoras
UNION ALL
SELECT 
    'Despesas Consolidadas' as tabela,
    COUNT(*) as total_registros
FROM despesas_consolidadas
UNION ALL
SELECT 
    'Despesas Agregadas' as tabela,
    COUNT(*) as total_registros
FROM despesas_agregadas;


-- Identificar possíveis problemas nos dados
SELECT 
    'CNPJs sem cadastro' as problema,
    COUNT(DISTINCT d.cnpj) as quantidade
FROM despesas_consolidadas d
LEFT JOIN operadoras o ON d.cnpj = o.cnpj
WHERE o.cnpj IS NULL
UNION ALL
SELECT 
    'Despesas negativas' as problema,
    COUNT(*) as quantidade
FROM despesas_consolidadas
WHERE valor_despesas < 0
UNION ALL
SELECT 
    'Trimestres inválidos' as problema,
    COUNT(*) as quantidade
FROM despesas_consolidadas
WHERE trimestre NOT BETWEEN 1 AND 4;


-- =====================================================================
-- MANUTENÇÃO E OTIMIZAÇÃO
-- =====================================================================

-- Atualizar estatísticas para melhor performance
ANALYZE operadoras;
ANALYZE despesas_consolidadas;
ANALYZE despesas_agregadas;

-- Vacuum para liberar espaço (executar periodicamente)
-- VACUUM ANALYZE;
