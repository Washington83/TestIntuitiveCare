import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe para transformação e validação de dados
 * Valida CNPJs, enriquece dados e gera agregações
 * 
 * TRADE-OFF PRINCIPAL: Validação de CNPJ inválido
 * - Opção escolhida: Manter registro mas marcar como "CNPJ_INVALIDO"
 * - PRÓS: Não perde dados, permite análise posterior
 * - CONTRAS: CSV final pode ter dados "sujos"
 * - JUSTIFICATIVA: Em ambiente real, analista precisa decidir o destino desses dados
 */
public class TransformacaoDados {
    
    public static void main(String[] args) {
        TransformacaoDados transformacao = new TransformacaoDados();
        
        try {
            System.out.println("=== INICIANDO TRANSFORMAÇÃO E VALIDAÇÃO DE DADOS ===\n");
            
            // 2.1 - Validação
            System.out.println("ETAPA 2.1: Validando dados...");
            List<DadosValidados> dadosValidados = transformacao.validarDados();
            System.out.println("  -> Dados validados: " + dadosValidados.size() + " registros\n");
            
            // 2.2 - Enriquecimento
            System.out.println("ETAPA 2.2: Enriquecendo dados com cadastro ANS...");
            List<DadosEnriquecidos> dadosEnriquecidos = transformacao.enriquecerDados(dadosValidados);
            System.out.println("  -> Dados enriquecidos: " + dadosEnriquecidos.size() + " registros\n");
            
            // 2.3 - Agregação
            System.out.println("ETAPA 2.3: Gerando agregações...");
            transformacao.gerarAgregacoes(dadosEnriquecidos);
            
            System.out.println("\n=== TRANSFORMAÇÃO CONCLUÍDA ===");
            
        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * ETAPA 2.1: Valida dados do CSV consolidado
     */
    private List<DadosValidados> validarDados() throws Exception {
        List<DadosValidados> dadosValidados = new ArrayList<>();
        
        // Simula leitura do consolidado_despesas.csv
        // Em produção, leria o arquivo real gerado na Parte 1
        
        // Exemplos de validação para diferentes cenários
        
        // CASO 1: CNPJ válido
        DadosValidados d1 = new DadosValidados(
            "12.345.678/0001-90",
            "Operadora Saúde Bem Estar Ltda",
            "3", "2024",
            1500000.50
        );
        if (validarCNPJ(d1.cnpj)) {
            d1.statusValidacao = "VALIDO";
        } else {
            d1.statusValidacao = "CNPJ_INVALIDO";
        }
        dadosValidados.add(d1);
        
        // CASO 2: CNPJ inválido (mantém mas marca)
        DadosValidados d2 = new DadosValidados(
            "00.000.000/0000-00",
            "Empresa com CNPJ Inválido",
            "3", "2024",
            500000.00
        );
        if (validarCNPJ(d2.cnpj)) {
            d2.statusValidacao = "VALIDO";
        } else {
            d2.statusValidacao = "CNPJ_INVALIDO";
            System.out.println("  [AVISO] CNPJ inválido encontrado: " + d2.cnpj + " - Registro mantido com marcação");
        }
        dadosValidados.add(d2);
        
        // CASO 3: Razão Social vazia
        DadosValidados d3 = new DadosValidados(
            "98.765.432/0001-10",
            "",
            "2", "2024",
            250000.00
        );
        if (d3.razaoSocial == null || d3.razaoSocial.trim().isEmpty()) {
            d3.statusValidacao = "RAZAO_SOCIAL_VAZIA";
            System.out.println("  [AVISO] Razão Social vazia para CNPJ: " + d3.cnpj);
        } else if (validarCNPJ(d3.cnpj)) {
            d3.statusValidacao = "VALIDO";
        }
        dadosValidados.add(d3);
        
        // CASO 4: Valor negativo
        DadosValidados d4 = new DadosValidados(
            "11.222.333/0001-44",
            "Plano Médico XYZ",
            "1", "2024",
            -1000.00
        );
        if (d4.valorDespesas < 0) {
            d4.statusValidacao = "VALOR_NEGATIVO";
            System.out.println("  [AVISO] Valor negativo encontrado para: " + d4.razaoSocial);
        }
        dadosValidados.add(d4);
        
        return dadosValidados;
    }
    
    /**
     * Valida CNPJ (formato e dígitos verificadores)
     * Algoritmo oficial da Receita Federal
     */
    private boolean validarCNPJ(String cnpj) {
        // Remove formatação
        cnpj = cnpj.replaceAll("[^0-9]", "");
        
        // Verifica se tem 14 dígitos
        if (cnpj.length() != 14) return false;
        
        // Verifica CNPJs inválidos conhecidos
        if (cnpj.equals("00000000000000") || 
            cnpj.equals("11111111111111") ||
            cnpj.equals("22222222222222")) {
            return false;
        }
        
        // Valida primeiro dígito verificador
        int soma = 0;
        int[] peso1 = {5,4,3,2,9,8,7,6,5,4,3,2};
        for (int i = 0; i < 12; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * peso1[i];
        }
        int digito1 = (soma % 11 < 2) ? 0 : 11 - (soma % 11);
        
        if (digito1 != Character.getNumericValue(cnpj.charAt(12))) {
            return false;
        }
        
        // Valida segundo dígito verificador
        soma = 0;
        int[] peso2 = {6,5,4,3,2,9,8,7,6,5,4,3,2};
        for (int i = 0; i < 13; i++) {
            soma += Character.getNumericValue(cnpj.charAt(i)) * peso2[i];
        }
        int digito2 = (soma % 11 < 2) ? 0 : 11 - (soma % 11);
        
        return digito2 == Character.getNumericValue(cnpj.charAt(13));
    }
    
    /**
     * ETAPA 2.2: Enriquece dados com informações cadastrais
     * 
     * TRADE-OFF: Estratégia de JOIN
     * - Opção escolhida: HashMap (join em memória)
     * - PRÓS: Rápido para volumes médios (até 100k registros), fácil de implementar
     * - CONTRAS: Usa mais memória que processamento linha a linha
     * - JUSTIFICATIVA: Dados da ANS não são tão grandes, cabe em memória
     */
    private List<DadosEnriquecidos> enriquecerDados(List<DadosValidados> dadosValidados) throws Exception {
        // Simula leitura do cadastro de operadoras
        Map<String, DadosCadastrais> cadastro = carregarCadastro();
        
        List<DadosEnriquecidos> dadosEnriquecidos = new ArrayList<>();
        int semMatch = 0;
        int comMatch = 0;
        
        for (DadosValidados dado : dadosValidados) {
            String cnpjLimpo = dado.cnpj.replaceAll("[^0-9]", "");
            DadosCadastrais cadastral = cadastro.get(cnpjLimpo);
            
            DadosEnriquecidos enriquecido;
            if (cadastral != null) {
                // Tem match no cadastro
                enriquecido = new DadosEnriquecidos(
                    dado.cnpj,
                    dado.razaoSocial,
                    dado.trimestre,
                    dado.ano,
                    dado.valorDespesas,
                    cadastral.registroANS,
                    cadastral.modalidade,
                    cadastral.uf,
                    dado.statusValidacao
                );
                comMatch++;
            } else {
                // Sem match no cadastro
                enriquecido = new DadosEnriquecidos(
                    dado.cnpj,
                    dado.razaoSocial,
                    dado.trimestre,
                    dado.ano,
                    dado.valorDespesas,
                    "NAO_ENCONTRADO",
                    "NAO_ENCONTRADO",
                    "NAO_ENCONTRADO",
                    dado.statusValidacao + ";SEM_CADASTRO"
                );
                semMatch++;
            }
            
            dadosEnriquecidos.add(enriquecido);
        }
        
        System.out.println("  >>> Estatísticas do JOIN:");
        System.out.println("      - Registros com match no cadastro: " + comMatch);
        System.out.println("      - Registros SEM match no cadastro: " + semMatch);
        
        // Salva resultado enriquecido
        salvarDadosEnriquecidos(dadosEnriquecidos);
        
        return dadosEnriquecidos;
    }
    
    /**
     * Carrega dados cadastrais das operadoras
     * (Simulado - em produção leria o CSV real da ANS)
     */
    private Map<String, DadosCadastrais> carregarCadastro() {
        Map<String, DadosCadastrais> cadastro = new HashMap<>();
        
        // Simula dados cadastrais
        cadastro.put("12345678000190", new DadosCadastrais(
            "123456", "Medicina de Grupo", "SP"
        ));
        
        cadastro.put("98765432000110", new DadosCadastrais(
            "654321", "Cooperativa Médica", "RJ"
        ));
        
        cadastro.put("11222333000144", new DadosCadastrais(
            "111222", "Seguradora", "MG"
        ));
        
        return cadastro;
    }
    
    /**
     * Salva dados enriquecidos em CSV
     */
    private void salvarDadosEnriquecidos(List<DadosEnriquecidos> dados) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter("dados_enriquecidos.csv"))) {
            writer.println("CNPJ;RazaoSocial;Trimestre;Ano;ValorDespesas;RegistroANS;Modalidade;UF;Status");
            
            for (DadosEnriquecidos dado : dados) {
                writer.println(dado.toCSV());
            }
        }
    }
    
    /**
     * ETAPA 2.3: Gera agregações por operadora e UF
     * 
     * TRADE-OFF: Estratégia de agregação
     * - Opção escolhida: Stream API do Java 8+
     * - PRÓS: Código limpo, legível, funcional
     * - CONTRAS: Um pouco mais lento que loops tradicionais
     * - JUSTIFICATIVA: Legibilidade e manutenibilidade são mais importantes aqui
     */
    private void gerarAgregacoes(List<DadosEnriquecidos> dados) throws Exception {
        
        // Agrupa por Razão Social + UF
        Map<String, List<DadosEnriquecidos>> grupos = dados.stream()
            .filter(d -> !d.uf.equals("NAO_ENCONTRADO")) // Remove os sem UF
            .collect(Collectors.groupingBy(d -> d.razaoSocial + "|" + d.uf));
        
        List<DadosAgregados> agregados = new ArrayList<>();
        
        for (Map.Entry<String, List<DadosEnriquecidos>> entry : grupos.entrySet()) {
            String[] chave = entry.getKey().split("\\|");
            String razaoSocial = chave[0];
            String uf = chave[1];
            List<DadosEnriquecidos> registros = entry.getValue();
            
            // Calcula estatísticas
            double total = registros.stream()
                .mapToDouble(d -> d.valorDespesas)
                .sum();
            
            double media = registros.stream()
                .mapToDouble(d -> d.valorDespesas)
                .average()
                .orElse(0.0);
            
            double desvioPadrao = calcularDesvioPadrao(registros, media);
            
            agregados.add(new DadosAgregados(
                razaoSocial, uf, total, media, desvioPadrao, registros.size()
            ));
        }
        
        // TRADE-OFF: Ordenação
        // Opção escolhida: Collections.sort() em memória
        // PRÓS: Simples e direto
        // CONTRAS: Para milhões de registros, poderia ser lento
        // JUSTIFICATIVA: Volume de agregados é pequeno (muito menor que dados originais)
        agregados.sort((a, b) -> Double.compare(b.totalDespesas, a.totalDespesas));
        
        // Salva resultado
        salvarAgregacoes(agregados);
        
        System.out.println("  >>> Top 5 operadoras por despesa total:");
        for (int i = 0; i < Math.min(5, agregados.size()); i++) {
            DadosAgregados ag = agregados.get(i);
            System.out.println(String.format("      %d. %s (%s): R$ %.2f", 
                i+1, ag.razaoSocial, ag.uf, ag.totalDespesas));
        }
    }
    
    /**
     * Calcula desvio padrão das despesas
     */
    private double calcularDesvioPadrao(List<DadosEnriquecidos> dados, double media) {
        double somaQuadrados = dados.stream()
            .mapToDouble(d -> Math.pow(d.valorDespesas - media, 2))
            .sum();
        
        return Math.sqrt(somaQuadrados / dados.size());
    }
    
    /**
     * Salva agregações em CSV
     */
    private void salvarAgregacoes(List<DadosAgregados> agregados) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter("despesas_agregadas.csv"))) {
            writer.println("RazaoSocial;UF;TotalDespesas;MediaPorTrimestre;DesvioPadrao;NumeroTrimestres");
            
            for (DadosAgregados ag : agregados) {
                writer.println(ag.toCSV());
            }
        }
        
        System.out.println("\n  -> Arquivo gerado: despesas_agregadas.csv");
    }
}

// Classes auxiliares
class DadosValidados {
    String cnpj;
    String razaoSocial;
    String trimestre;
    String ano;
    double valorDespesas;
    String statusValidacao = "";
    
    DadosValidados(String cnpj, String razaoSocial, String trimestre, 
                   String ano, double valorDespesas) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.trimestre = trimestre;
        this.ano = ano;
        this.valorDespesas = valorDespesas;
    }
}

class DadosCadastrais {
    String registroANS;
    String modalidade;
    String uf;
    
    DadosCadastrais(String registroANS, String modalidade, String uf) {
        this.registroANS = registroANS;
        this.modalidade = modalidade;
        this.uf = uf;
    }
}

class DadosEnriquecidos {
    String cnpj;
    String razaoSocial;
    String trimestre;
    String ano;
    double valorDespesas;
    String registroANS;
    String modalidade;
    String uf;
    String status;
    
    DadosEnriquecidos(String cnpj, String razaoSocial, String trimestre, String ano,
                      double valorDespesas, String registroANS, String modalidade,
                      String uf, String status) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.trimestre = trimestre;
        this.ano = ano;
        this.valorDespesas = valorDespesas;
        this.registroANS = registroANS;
        this.modalidade = modalidade;
        this.uf = uf;
        this.status = status;
    }
    
    String toCSV() {
        return String.format("%s;%s;%s;%s;%.2f;%s;%s;%s;%s",
            cnpj, razaoSocial, trimestre, ano, valorDespesas,
            registroANS, modalidade, uf, status);
    }
}

class DadosAgregados {
    String razaoSocial;
    String uf;
    double totalDespesas;
    double mediaPorTrimestre;
    double desvioPadrao;
    int numeroTrimestres;
    
    DadosAgregados(String razaoSocial, String uf, double totalDespesas,
                   double mediaPorTrimestre, double desvioPadrao, int numeroTrimestres) {
        this.razaoSocial = razaoSocial;
        this.uf = uf;
        this.totalDespesas = totalDespesas;
        this.mediaPorTrimestre = mediaPorTrimestre;
        this.desvioPadrao = desvioPadrao;
        this.numeroTrimestres = numeroTrimestres;
    }
    
    String toCSV() {
        return String.format("%s;%s;%.2f;%.2f;%.2f;%d",
            razaoSocial, uf, totalDespesas, mediaPorTrimestre, 
            desvioPadrao, numeroTrimestres);
    }
}
