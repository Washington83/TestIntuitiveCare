"""
API REST para consulta de dados de operadoras de saúde

TRADE-OFF: Framework Flask vs FastAPI
- Decisão: Flask
- PRÓS: Mais simples, mais documentação, comunidade maior
- CONTRAS: Não tem validação automática de tipos como FastAPI
- JUSTIFICATIVA: Para um projeto de teste com escopo limitado,
  Flask é mais do que suficiente e mais fácil para iniciantes

TRADE-OFF: Paginação
- Decisão: Offset-based pagination
- PRÓS: Simples de implementar, funciona com qualquer ordenação
- CONTRAS: Performance degrada em offsets muito grandes
- JUSTIFICATIVA: Volume de dados é pequeno, não teremos milhares de páginas

TRADE-OFF: Cache
- Decisão: Cache em memória com timeout de 5 minutos para /estatisticas
- PRÓS: Reduz carga no banco, resposta instantânea
- CONTRAS: Dados podem ficar ligeiramente desatualizados
- JUSTIFICATIVA: Estatísticas não mudam com frequência (dados trimestrais)
"""

from flask import Flask, jsonify, request
from flask_cors import CORS
import sqlite3
from datetime import datetime, timedelta
from functools import wraps

app = Flask(__name__)
CORS(app)  # Permite requisições do frontend Vue.js

# Configuração do banco de dados
DATABASE = 'dados_ans.db'

# Cache simples em memória
cache_estatisticas = {
    'dados': None,
    'timestamp': None,
    'timeout': 300  # 5 minutos em segundos
}


def get_db_connection():
    """
    Cria conexão com o banco de dados SQLite
    
    NOTA: Em produção, usaríamos PostgreSQL.
    SQLite é usado aqui por simplicidade (não requer instalação).
    """
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row  # Permite acessar colunas por nome
    return conn


def init_db():
    """
    Inicializa o banco de dados com dados de exemplo
    """
    conn = get_db_connection()
    cursor = conn.cursor()
    
    # Cria tabela de operadoras
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS operadoras (
            cnpj TEXT PRIMARY KEY,
            razao_social TEXT NOT NULL,
            registro_ans TEXT,
            modalidade TEXT,
            uf TEXT
        )
    ''')
    
    # Cria tabela de despesas
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS despesas (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cnpj TEXT NOT NULL,
            trimestre INTEGER NOT NULL,
            ano INTEGER NOT NULL,
            valor REAL NOT NULL,
            FOREIGN KEY (cnpj) REFERENCES operadoras(cnpj)
        )
    ''')
    
    # Insere dados de exemplo
    operadoras_exemplo = [
        ('12.345.678/0001-90', 'Operadora Saúde Bem Estar Ltda', '123456', 'Medicina de Grupo', 'SP'),
        ('98.765.432/0001-10', 'Plano Saúde Total S.A.', '654321', 'Cooperativa Médica', 'RJ'),
        ('11.222.333/0001-44', 'Assistência Médica Premium', '111222', 'Seguradora', 'MG'),
        ('44.555.666/0001-77', 'Saúde Plus Operadora', '444555', 'Autogestão', 'SP'),
        ('77.888.999/0001-33', 'Vida Saudável Planos', '777888', 'Medicina de Grupo', 'RJ'),
    ]
    
    cursor.executemany(
        'INSERT OR IGNORE INTO operadoras VALUES (?, ?, ?, ?, ?)',
        operadoras_exemplo
    )
    
    # Insere despesas de exemplo
    despesas_exemplo = []
    for cnpj, _, _, _, _ in operadoras_exemplo:
        for trimestre in [1, 2, 3]:
            for ano in [2024]:
                valor = 100000.00 + (hash(cnpj + str(trimestre)) % 500000)
                despesas_exemplo.append((cnpj, trimestre, ano, valor))
    
    cursor.executemany(
        'INSERT OR IGNORE INTO despesas (cnpj, trimestre, ano, valor) VALUES (?, ?, ?, ?)',
        despesas_exemplo
    )
    
    conn.commit()
    conn.close()
    print("✓ Banco de dados inicializado com sucesso!")


# =====================================================================
# ROTAS DA API
# =====================================================================

@app.route('/api/operadoras', methods=['GET'])
def listar_operadoras():
    """
    GET /api/operadoras
    Lista todas as operadoras com paginação
    
    Parâmetros:
    - page: número da página (padrão: 1)
    - limit: registros por página (padrão: 10, máx: 100)
    - busca: filtro por razão social ou CNPJ (opcional)
    
    TRADE-OFF: Estrutura de resposta
    - Decisão: Retornar dados + metadados
    - PRÓS: Frontend tem todas as informações para paginação
    - CONTRAS: Response um pouco maior
    - JUSTIFICATIVA: Metadados são essenciais para boa UX
    """
    # Parâmetros de paginação
    page = int(request.args.get('page', 1))
    limit = min(int(request.args.get('limit', 10)), 100)  # Máx 100
    offset = (page - 1) * limit
    
    # Parâmetro de busca (opcional)
    busca = request.args.get('busca', '').strip()
    
    conn = get_db_connection()
    
    # Monta query com filtro opcional
    where_clause = ""
    params = []
    
    if busca:
        where_clause = "WHERE razao_social LIKE ? OR cnpj LIKE ?"
        params = [f'%{busca}%', f'%{busca}%']
    
    # Busca dados
    query = f'''
        SELECT cnpj, razao_social, registro_ans, modalidade, uf
        FROM operadoras
        {where_clause}
        ORDER BY razao_social
        LIMIT ? OFFSET ?
    '''
    params.extend([limit, offset])
    
    operadoras = conn.execute(query, params).fetchall()
    
    # Conta total de registros
    count_query = f'SELECT COUNT(*) as total FROM operadoras {where_clause}'
    total = conn.execute(count_query, params[:len(params)-2] if busca else []).fetchone()['total']
    
    conn.close()
    
    # Formata resposta
    return jsonify({
        'data': [dict(op) for op in operadoras],
        'page': page,
        'limit': limit,
        'total': total,
        'total_pages': (total + limit - 1) // limit  # Arredonda para cima
    })


@app.route('/api/operadoras/<cnpj>', methods=['GET'])
def detalhes_operadora(cnpj):
    """
    GET /api/operadoras/{cnpj}
    Retorna detalhes de uma operadora específica
    """
    conn = get_db_connection()
    
    operadora = conn.execute(
        'SELECT * FROM operadoras WHERE cnpj = ?',
        (cnpj,)
    ).fetchone()
    
    conn.close()
    
    if operadora is None:
        return jsonify({'erro': 'Operadora não encontrada'}), 404
    
    return jsonify(dict(operadora))


@app.route('/api/operadoras/<cnpj>/despesas', methods=['GET'])
def historico_despesas(cnpj):
    """
    GET /api/operadoras/{cnpj}/despesas
    Retorna histórico de despesas da operadora
    """
    conn = get_db_connection()
    
    # Verifica se operadora existe
    operadora = conn.execute(
        'SELECT razao_social FROM operadoras WHERE cnpj = ?',
        (cnpj,)
    ).fetchone()
    
    if operadora is None:
        conn.close()
        return jsonify({'erro': 'Operadora não encontrada'}), 404
    
    # Busca histórico
    despesas = conn.execute('''
        SELECT trimestre, ano, valor
        FROM despesas
        WHERE cnpj = ?
        ORDER BY ano DESC, trimestre DESC
    ''', (cnpj,)).fetchall()
    
    conn.close()
    
    return jsonify({
        'cnpj': cnpj,
        'razao_social': operadora['razao_social'],
        'despesas': [dict(d) for d in despesas]
    })


@app.route('/api/estatisticas', methods=['GET'])
def estatisticas():
    """
    GET /api/estatisticas
    Retorna estatísticas agregadas
    
    TRADE-OFF: Cache vs Cálculo em tempo real
    - Decisão: Cache de 5 minutos
    - PRÓS: Performance excelente, reduz carga no DB
    - CONTRAS: Dados podem estar até 5 min desatualizados
    - JUSTIFICATIVA: Dados são atualizados trimestralmente,
      então delay de 5 min é irrelevante
    """
    global cache_estatisticas
    
    # Verifica se cache é válido
    agora = datetime.now()
    if (cache_estatisticas['dados'] is not None and 
        cache_estatisticas['timestamp'] is not None):
        
        tempo_decorrido = (agora - cache_estatisticas['timestamp']).total_seconds()
        if tempo_decorrido < cache_estatisticas['timeout']:
            print(f"✓ Usando cache (idade: {int(tempo_decorrido)}s)")
            return jsonify(cache_estatisticas['dados'])
    
    # Cache inválido ou expirado, recalcula
    print("✓ Recalculando estatísticas...")
    
    conn = get_db_connection()
    
    # Total de despesas
    total_despesas = conn.execute(
        'SELECT SUM(valor) as total FROM despesas'
    ).fetchone()['total'] or 0
    
    # Média de despesas
    media_despesas = conn.execute(
        'SELECT AVG(valor) as media FROM despesas'
    ).fetchone()['media'] or 0
    
    # Top 5 operadoras
    top_operadoras = conn.execute('''
        SELECT 
            o.cnpj,
            o.razao_social,
            SUM(d.valor) as total_despesas
        FROM operadoras o
        INNER JOIN despesas d ON o.cnpj = d.cnpj
        GROUP BY o.cnpj, o.razao_social
        ORDER BY total_despesas DESC
        LIMIT 5
    ''').fetchall()
    
    # Distribuição por UF
    distribuicao_uf = conn.execute('''
        SELECT 
            o.uf,
            COUNT(DISTINCT o.cnpj) as num_operadoras,
            SUM(d.valor) as total_despesas
        FROM operadoras o
        INNER JOIN despesas d ON o.cnpj = d.cnpj
        GROUP BY o.uf
        ORDER BY total_despesas DESC
    ''').fetchall()
    
    conn.close()
    
    # Monta resposta
    resultado = {
        'resumo': {
            'total_despesas': round(total_despesas, 2),
            'media_despesas': round(media_despesas, 2),
            'num_operadoras': len(top_operadoras)
        },
        'top_operadoras': [dict(op) for op in top_operadoras],
        'distribuicao_uf': [dict(uf) for uf in distribuicao_uf]
    }
    
    # Atualiza cache
    cache_estatisticas['dados'] = resultado
    cache_estatisticas['timestamp'] = agora
    
    return jsonify(resultado)


@app.route('/api/health', methods=['GET'])
def health_check():
    """
    GET /api/health
    Verifica se a API está funcionando
    """
    return jsonify({
        'status': 'ok',
        'timestamp': datetime.now().isoformat(),
        'versao': '1.0.0'
    })


# =====================================================================
# TRATAMENTO DE ERROS
# =====================================================================

@app.errorhandler(404)
def not_found(error):
    """
    TRADE-OFF: Mensagens de erro genéricas vs específicas
    - Decisão: Mensagens específicas em desenvolvimento, genéricas em produção
    - JUSTIFICATIVA: Ajuda no debug mas não expõe detalhes internos
    """
    return jsonify({'erro': 'Recurso não encontrado'}), 404


@app.errorhandler(500)
def internal_error(error):
    return jsonify({'erro': 'Erro interno do servidor'}), 500


# =====================================================================
# MAIN
# =====================================================================

if __name__ == '__main__':
    print("=== Servidor API REST para Dados ANS ===\n")
    
    # Inicializa banco de dados
    init_db()
    
    print("\n=== Rotas Disponíveis ===")
    print("GET  /api/operadoras          - Lista operadoras (paginado)")
    print("GET  /api/operadoras/{cnpj}   - Detalhes de uma operadora")
    print("GET  /api/operadoras/{cnpj}/despesas - Histórico de despesas")
    print("GET  /api/estatisticas        - Estatísticas agregadas")
    print("GET  /api/health              - Health check")
    
    print("\n=== Iniciando servidor na porta 5000 ===\n")
    
    # Inicia servidor
    app.run(debug=True, host='0.0.0.0', port=5000)
