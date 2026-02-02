# 📚 README - TESTE TÉCNICO INTUITIVE CARE

---
## 🎯 IMPORTANTE - LEIA PRIMEIRO!

**O que eu sei:**
- ✅ Classes e objetos (POO básico)
- ✅ ArrayList
- ✅ Leitura/escrita de arquivos
- ✅ Tratamento de exceções (try-catch)
- ✅ Conceitos básicos de programação
- ✅ Básico de SQL 

**O que ainda estou aprendendo:**
- 🔄 Conceitos avançados de Java (Streams, Lambdas, etc)
- 🔄 Frameworks (Spring, Hibernate, etc)
- 🔄 Arquitetura de sistemas complexos

**Minha abordagem:**
Bem boa parte deste **"TESTE"** usei IA para resolver, como meu nível de conhecimento e baixo usei como aprendizado e vi que tenho muito conteúdo a estudar e praticar, analisei e compilei boa parte do código para entender.

---
## 📁 ESTRUTURA DO PROJETO

```
teste_estagio/
│
├── README.md
│
├── parte1/
│   └── IntegracaoANS_Simples.java    ← Versão com MUITOS comentários
│
├── parte2/
│   └── TransformacaoDados_Simples.java
│
├── parte3/
│   └── queries_sql.sql          
│
└── parte4/
    ├── backend/
    │   └── api_server.py        
    └── frontend/
        └── index.html           ← HTML+JavaScript (visual)
```

---
## 🚀 COMO EXECUTAR (BEM SIMPLES!)

### ✅ Parte 1 - Integração ANS

```bash
# Passo 1: Abra o terminal na pasta parte1
cd parte1

# Passo 2: Compile o código Java
javac IntegracaoANS_Simples.java

# Passo 3: Execute
java IntegracaoANS_Simples
```

**O que vai acontecer:**
- Programa vai simular download de dados
- Vai processar 3 trimestres
- Vai criar arquivo `consolidado_despesas.csv`

**Se der erro "javac não encontrado":**
- Você precisa instalar o JDK
- Download: https://www.oracle.com/java/technologies/downloads/

---

### ✅ Parte 2 - Validação de Dados

```bash
# Passo 1: Abra o terminal na pasta parte2
cd parte2

# Passo 2: Compile
javac TransformacaoDados_Simples.java

# Passo 3: Execute
java TransformacaoDados_Simples
```

**O que vai acontecer:**
- Programa vai validar CNPJs
- Vai fazer "JOIN" de dados (juntar duas fontes)
- Vai calcular médias e totais
- Vai criar arquivo `despesas_agregadas.csv`

---

### ✅ Parte 3 - Banco de Dados

**Opção FÁCIL (apenas visualizar as queries):**
```bash
# Abra o arquivo queries_sql.sql em qualquer editor de texto
# Você pode ler e entender as queries SQL mesmo sem executar
```

**Opção COMPLETA (se quiser realmente executar):**
1. Instale PostgreSQL
2. Crie um banco de dados
3. Execute o arquivo SQL

*Obs: Não se preocupe se não conseguir executar. SQL é mais fácil de ler do que executar!*

---

### ✅ Parte 4 - API e Dashboard

**Backend (API em Python):**
```bash
# Passo 1: Vá para pasta do backend
cd parte4/backend

# Passo 2: Instale dependências
pip install flask flask-cors

# Passo 3: Execute o servidor
python api_server.py
```

**Frontend (Dashboard):**
```bash
# Opção FÁCIL: Abra direto no navegador
# 1. Vá em parte4/frontend
# 2. Dê duplo clique em index.html
# 3. Vai abrir no navegador!

# Ou use servidor local:
cd parte4/frontend
python -m http.server 8080
# Depois abra: http://localhost:8080
```

---

## 💡 PRINCIPAIS DECISÕES TÉCNICAS

### 1. Processamento em Lotes
**Por quê?** 
- Mais estável, não trava com muitos dados
- Mais fácil de entender e debugar
- Melhor para iniciante

### 2. HashMap para JOIN
**Por quê?**
- Muito rápido (busca instantânea)
- Conceito simples: como um dicionário
- Código fácil de entender

### 3. Validação de CNPJ
**Por quê?**
- Implementei o algoritmo oficial
- Demonstra que sei pesquisar e aplicar
- Não precisa de biblioteca externa

### 4. Manter dados "sujos"
**Por quê?**
- Em vez de deletar, marco como suspeito
- Permite análise posterior
- É mais seguro

---
## 🎓 O QUE APRENDI FAZENDO ESTE TESTE

### Conceitos Técnicos:
- ✅ Como validar CNPJ matematicamente
- ✅ Como processar arquivos CSV
- ✅ Como fazer JOIN sem banco de dados
- ✅ Como calcular estatísticas (média, desvio padrão)
- ✅ Básico de API REST

### Soft Skills:
- ✅ Pesquisar soluções de forma independente
- ✅ Documentar decisões técnicas
- ✅ Pensar em trade-offs
- ✅ Escrever código que outros entendam

---
## 🐛 PROBLEMAS

### 1. Validação de CNPJ
**Problema:** Não sabia como validar CNPJ.
**Solução:** Pesquisei o algoritmo oficial da Receita Federal e implementei.

### 2. Lidar com dados inconsistentes
**Problema:** Dados reais são sempre "sujos" (valores negativos, duplicados).
**Solução:** Decidi manter tudo e marcar problemas, em vez de deletar.

### 3. Explicar decisões técnicas
**Problema:** Sabia fazer, mas não sabia explicar o "por quê".
**Solução:** Documentei cada trade-off com prós, contras e justificativa.

---
