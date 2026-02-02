#!/bin/bash
# Script de compilação e execução rápida do projeto

echo "======================================"
echo "TESTE TÉCNICO - INTUITIVE CARE"
echo "======================================"
echo ""

# Cores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função de ajuda
function show_help {
    echo "Uso: ./run.sh [opção]"
    echo ""
    echo "Opções:"
    echo "  parte1    - Executa Parte 1 (Integração ANS)"
    echo "  parte2    - Executa Parte 2 (Transformação de Dados)"
    echo "  parte3    - Executa Parte 3 (Consultas SQL via Java)"
    echo "  backend   - Executa Parte 4 - Backend (API Flask)"
    echo "  frontend  - Abre Parte 4 - Frontend (Dashboard Vue.js)"
    echo "  tudo      - Executa todas as partes em sequência"
    echo "  ajuda     - Mostra esta mensagem"
    echo ""
}

# Parte 1
function run_parte1 {
    echo -e "${YELLOW}[PARTE 1]${NC} Compilando e executando IntegracaoANS.java..."
    cd parte1
    javac IntegracaoANS.java
    if [ $? -eq 0 ]; then
        java IntegracaoANS
        echo -e "${GREEN}✓ Parte 1 concluída!${NC}"
    else
        echo -e "${RED}✗ Erro na compilação${NC}"
    fi
    cd ..
}

# Parte 2
function run_parte2 {
    echo -e "${YELLOW}[PARTE 2]${NC} Compilando e executando TransformacaoDados.java..."
    cd parte2
    javac TransformacaoDados.java
    if [ $? -eq 0 ]; then
        java TransformacaoDados
        echo -e "${GREEN}✓ Parte 2 concluída!${NC}"
    else
        echo -e "${RED}✗ Erro na compilação${NC}"
    fi
    cd ..
}

# Parte 3
function run_parte3 {
    echo -e "${YELLOW}[PARTE 3]${NC} Executando consultas SQL..."
    echo ""
    echo "OPÇÃO A: Executar via PostgreSQL direto"
    echo "  psql -d ans_database -f parte3/queries_sql.sql"
    echo ""
    echo "OPÇÃO B: Executar via Java (requer driver JDBC)"
    cd parte3
    javac ConsultasBancoDados.java 2>/dev/null
    if [ $? -eq 0 ]; then
        java ConsultasBancoDados
    else
        echo -e "${YELLOW}Nota: Para executar via Java, baixe o driver JDBC PostgreSQL${NC}"
        echo "https://jdbc.postgresql.org/download.html"
    fi
    cd ..
}

# Backend
function run_backend {
    echo -e "${YELLOW}[PARTE 4 - BACKEND]${NC} Iniciando servidor Flask..."
    cd parte4/backend
    
    # Verifica se Flask está instalado
    if ! python3 -c "import flask" 2>/dev/null; then
        echo -e "${YELLOW}Instalando dependências...${NC}"
        pip3 install -r requirements.txt
    fi
    
    echo -e "${GREEN}Servidor iniciando em http://localhost:5000${NC}"
    echo "Pressione Ctrl+C para parar"
    python3 api_server.py
    cd ../..
}

# Frontend
function run_frontend {
    echo -e "${YELLOW}[PARTE 4 - FRONTEND]${NC} Abrindo dashboard..."
    cd parte4/frontend
    
    # Tenta abrir o navegador
    if command -v xdg-open &> /dev/null; then
        xdg-open index.html
    elif command -v open &> /dev/null; then
        open index.html
    else
        echo "Abra manualmente: $(pwd)/index.html"
    fi
    
    # Opcionalmente, inicia servidor HTTP
    echo ""
    echo "Ou rode um servidor HTTP local:"
    echo "  python3 -m http.server 8080"
    cd ../..
}

# Executa tudo
function run_all {
    run_parte1
    echo ""
    run_parte2
    echo ""
    run_parte3
    echo ""
    echo -e "${GREEN}========================${NC}"
    echo -e "${GREEN}TODAS AS PARTES CONCLUÍDAS!${NC}"
    echo -e "${GREEN}========================${NC}"
    echo ""
    echo "Para iniciar a interface web:"
    echo "  ./run.sh backend   (em um terminal)"
    echo "  ./run.sh frontend  (em outro terminal)"
}

# Main
case "$1" in
    parte1)
        run_parte1
        ;;
    parte2)
        run_parte2
        ;;
    parte3)
        run_parte3
        ;;
    backend)
        run_backend
        ;;
    frontend)
        run_frontend
        ;;
    tudo)
        run_all
        ;;
    ajuda|help|--help|-h)
        show_help
        ;;
    *)
        show_help
        ;;
esac
