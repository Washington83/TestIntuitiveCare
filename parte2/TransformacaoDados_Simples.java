import java.io.*;
import java.util.*;

/**
 * =============================================================================
 * PARTE 2: VALIDAÇÃO E TRANSFORMAÇÃO DE DADOS
 * =============================================================================
 * 
 * O QUE ESTE PROGRAMA FAZ:
 * 1. Valida se CNPJs estão corretos (usando matemática)
 * 2. Junta dados de duas fontes diferentes (como JOIN em banco de dados)
 * 3. Calcula totais e médias por operadora
 * 
 * CONCEITOS NOVOS (mas simples):
 * - HashMap: como um dicionário (chave → valor)
 * - Validação matemática de CNPJ
 * - Cálculos estatísticos básicos (soma, média)
 * 
 * NÃO SE ASSUSTE! Tudo é feito passo a passo com explicações.
 * =============================================================================
 */
public class TransformacaoDados_Simples {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  PARTE 2: VALIDAÇÃO E TRANSFORMAÇÃO       ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            // ETAPA 1: Validar CNPJs
            System.out.println("[ETAPA 1] Validando CNPJs...");
            validarCNPJs();
            System.out.println();
            
            // ETAPA 2: Juntar dados (como JOIN em SQL)
            System.out.println("[ETAPA 2] Enriquecendo dados...");
            enriquecerDados();
            System.out.println();
            
            // ETAPA 3: Calcular agregações
            System.out.println("[ETAPA 3] Calculando agregações...");
            calcularAgregacoes();
            System.out.println();
            
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║      TRANSFORMAÇÃO CONCLUÍDA!              ║");
            System.out.println("╚════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("❌ ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * ==========================================================================
     * ETAPA 1: VALIDAR CNPJs
     * ==========================================================================
     * 
     * CNPJ é como o CPF da empresa: 14 dígitos + validação matemática.
     * Exemplo: 12.345.678/0001-90
     * 
     * Os 2 últimos dígitos (90) são calculados matematicamente a partir
     * dos 12 primeiros. Se a conta não bater, o CNPJ é inválido.
     * 
     * TRADE-OFF: O que fazer com CNPJ inválido?
     * - Opção A: Deletar o registro → perde informação
     * - Opção B: Tentar corrigir → pode criar erro
     * - Opção C: Manter mas marcar (ESCOLHI ESSA) → permite análise depois
     * ==========================================================================
     */
    private static void validarCNPJs() {
        System.out.println("   Testando validação de CNPJs:");
        
        // Testa CNPJs diferentes
        String[] cnpjsTeste = {
            "12.345.678/0001-90",  // Pode ser válido
            "00.000.000/0000-00",  // Inválido (todos zeros)
            "11.111.111/0001-11"   // Inválido (repetido)
        };
        
        for (String cnpj : cnpjsTeste) {
            boolean valido = validarCNPJ(cnpj);
            String status = valido ? "✓ VÁLIDO" : "✗ INVÁLIDO";
            System.out.println("   " + cnpj + " → " + status);
        }
    }
    
    /**
     * Valida CNPJ usando algoritmo oficial da Receita Federal
     * 
     * COMO FUNCIONA:
     * 1. Remove pontos e traços (deixa só números)
     * 2. Verifica se tem 14 dígitos
     * 3. Verifica se não é sequência óbvia (00000000000000, etc)
     * 4. Calcula os dígitos verificadores e compara
     */
    private static boolean validarCNPJ(String cnpj) {
        // PASSO 1: Remove formatação (deixa só números)
        cnpj = cnpj.replaceAll("[^0-9]", "");
        // Exemplo: "12.345.678/0001-90" vira "12345678000190"
        
        // PASSO 2: Verifica tamanho
        if (cnpj.length() != 14) {
            return false;  // CNPJ tem que ter 14 dígitos
        }
        
        // PASSO 3: Verifica se não é sequência óbvia
        if (cnpj.equals("00000000000000") || 
            cnpj.equals("11111111111111") ||
            cnpj.equals("22222222222222")) {
            return false;  // Esses CNPJs não existem
        }
        
        // PASSO 4: Valida primeiro dígito verificador
        // (Matemática complicada, mas é só seguir a receita!)
        
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        
        // Multiplica cada dígito pelo peso correspondente e soma
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(cnpj.charAt(i));
            soma += digito * pesos1[i];
        }
        
        // Calcula o dígito verificador
        int resto = soma % 11;
        int digitoVerificador1 = (resto < 2) ? 0 : (11 - resto);
        
        // Compara com o dígito real do CNPJ
        if (digitoVerificador1 != Character.getNumericValue(cnpj.charAt(12))) {
            return false;
        }
        
        // PASSO 5: Valida segundo dígito verificador
        // (Processo similar, mas com pesos diferentes)
        
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        soma = 0;
        
        for (int i = 0; i < 13; i++) {
            int digito = Character.getNumericValue(cnpj.charAt(i));
            soma += digito * pesos2[i];
        }
        
        resto = soma % 11;
        int digitoVerificador2 = (resto < 2) ? 0 : (11 - resto);
        
        if (digitoVerificador2 != Character.getNumericValue(cnpj.charAt(13))) {
            return false;
        }
        
        // Se chegou aqui, CNPJ é válido!
        return true;
    }
    
    /**
     * ==========================================================================
     * ETAPA 2: ENRIQUECER DADOS
     * ==========================================================================
     * 
     * "Enriquecer" = adicionar mais informações aos dados.
     * 
     * Temos 2 fontes de dados:
     * 1. Arquivo de despesas (CNPJ, valor, trimestre)
     * 2. Arquivo cadastral (CNPJ, nome, UF, modalidade)
     * 
     * Queremos juntar os dois usando o CNPJ como "chave".
     * É como fazer JOIN em banco de dados!
     * 
     * TRADE-OFF: Como fazer o JOIN?
     * - Opção A: Loop dentro de loop → MUITO LENTO (O(n²))
     * - Opção B: HashMap (ESCOLHI ESSA) → RÁPIDO (O(n))
     * - Opção C: Banco de dados → adiciona complexidade
     * 
     * HashMap é como um dicionário: dado uma "chave", retorna o "valor".
     * Exemplo: dado CNPJ "12345678000190", retorna dados cadastrais.
     * ==========================================================================
     */
    private static void enriquecerDados() {
        // PASSO 1: Criar "dicionário" de dados cadastrais
        // HashMap<chave, valor> = HashMap<CNPJ, DadosCadastrais>
        
        HashMap<String, DadosCadastrais> cadastro = new HashMap<>();
        
        // Adiciona alguns dados de exemplo
        // put() = adiciona no HashMap
        cadastro.put("12345678000190", new DadosCadastrais(
            "Operadora Saúde SP",
            "SP",
            "Medicina de Grupo"
        ));
        
        cadastro.put("98765432000110", new DadosCadastrais(
            "Plano Saúde RJ",
            "RJ",
            "Cooperativa Médica"
        ));
        
        // PASSO 2: Buscar dados cadastrais de uma operadora
        String cnpjProcurado = "12345678000190";
        
        // get() = busca no HashMap (muito rápido!)
        DadosCadastrais dados = cadastro.get(cnpjProcurado);
        
        if (dados != null) {
            System.out.println("   ✓ Encontrado: " + dados.razaoSocial + " (" + dados.uf + ")");
        } else {
            System.out.println("   ✗ CNPJ não encontrado no cadastro");
        }
        
        // PASSO 3: Estatísticas do JOIN
        int comMatch = 0;
        int semMatch = 0;
        
        String[] cnpjsParaTestar = {"12345678000190", "98765432000110", "99999999999999"};
        
        for (String cnpj : cnpjsParaTestar) {
            if (cadastro.containsKey(cnpj)) {
                comMatch++;
            } else {
                semMatch++;
            }
        }
        
        System.out.println("   • Registros com match: " + comMatch);
        System.out.println("   • Registros SEM match: " + semMatch);
    }
    
    /**
     * ==========================================================================
     * ETAPA 3: CALCULAR AGREGAÇÕES
     * ==========================================================================
     * 
     * Agregação = juntar vários registros e calcular algo (soma, média, etc).
     * 
     * Exemplo: Se uma operadora tem despesas em 3 trimestres:
     * - Trimestre 1: R$ 1.000.000
     * - Trimestre 2: R$ 1.200.000  
     * - Trimestre 3: R$ 1.100.000
     * 
     * Agregações:
     * - Total: R$ 3.300.000
     * - Média: R$ 1.100.000
     * 
     * ==========================================================================
     */
    private static void calcularAgregacoes() {
        // Dados de exemplo de uma operadora em 3 trimestres
        double[] despesas = {1000000.0, 1200000.0, 1100000.0};
        
        // CÁLCULO 1: Total (soma de tudo)
        double total = 0;
        for (double valor : despesas) {
            total += valor;  // total = total + valor
        }
        
        // CÁLCULO 2: Média (total dividido pela quantidade)
        double media = total / despesas.length;
        
        // CÁLCULO 3: Desvio padrão (o quanto varia da média)
        // Passos:
        // 1. Para cada valor, calcular (valor - media)²
        // 2. Somar tudo
        // 3. Dividir pela quantidade
        // 4. Tirar raiz quadrada
        
        double somaQuadrados = 0;
        for (double valor : despesas) {
            double diferenca = valor - media;
            somaQuadrados += diferenca * diferenca;
        }
        double desvioPadrao = Math.sqrt(somaQuadrados / despesas.length);
        
        // Mostra resultados
        System.out.println("   Operadora: Exemplo Ltda");
        System.out.println("   • Total de despesas: R$ " + String.format("%.2f", total));
        System.out.println("   • Média por trimestre: R$ " + String.format("%.2f", media));
        System.out.println("   • Desvio padrão: R$ " + String.format("%.2f", desvioPadrao));
        System.out.println();
        
        // Salva em arquivo CSV
        salvarAgregacao(total, media, desvioPadrao);
    }
    
    /**
     * Salva agregações em arquivo CSV
     */
    private static void salvarAgregacao(double total, double media, double desvio) 
            throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter("despesas_agregadas.csv"))) {
            writer.println("RazaoSocial;UF;TotalDespesas;MediaPorTrimestre;DesvioPadrao");
            writer.println(String.format("Exemplo Ltda;SP;%.2f;%.2f;%.2f", 
                total, media, desvio));
        }
        System.out.println("   ✓ Arquivo salvo: despesas_agregadas.csv");
    }
}

/**
 * =============================================================================
 * CLASSE AUXILIAR: DadosCadastrais
 * =============================================================================
 * 
 * Representa dados cadastrais de uma operadora.
 * Apenas 3 informações simples: nome, UF, modalidade.
 * =============================================================================
 */
class DadosCadastrais {
    String razaoSocial;  // Nome da empresa
    String uf;           // Estado (SP, RJ, MG, etc)
    String modalidade;   // Tipo (Medicina de Grupo, Cooperativa, etc)
    
    public DadosCadastrais(String razaoSocial, String uf, String modalidade) {
        this.razaoSocial = razaoSocial;
        this.uf = uf;
        this.modalidade = modalidade;
    }
}
