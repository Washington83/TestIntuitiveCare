import java.io.*;
import java.util.*;

/**
 * =============================================================================
 * PARTE 1: INTEGRAÇÃO COM API DA ANS
 * =============================================================================
 * 
 * O QUE ESTE PROGRAMA FAZ:
 * 1. Simula o download de dados de operadoras de saúde
 * 2. Junta dados de 3 trimestres diferentes
 * 3. Salva tudo em um arquivo CSV
 * 
 * CONCEITOS DE JAVA USADOS (você já conhece):
 * - Classes e Objetos (POO básico)
 * - ArrayList (lista dinâmica)
 * - FileWriter (escrever em arquivo)
 * - PrintWriter (escrever texto formatado)
 * 
 * TRADE-OFF PRINCIPAL: Por que processar trimestre por trimestre?
 * - Opção A: Carregar tudo de uma vez → Mais rápido MAS pode travar
 * - Opção B: Um trimestre por vez (ESCOLHI ESSA) → Mais lento MAS sempre funciona
 * - Justificativa: Para iniciante, é melhor algo que sempre funciona
 * =============================================================================
 */
public class IntegracaoANS_Simples {
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  PARTE 1: INTEGRAÇÃO COM DADOS DA ANS     ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            // PASSO 1: Criar lista para guardar todos os dados
            // ArrayList é como uma lista que cresce sozinha
            ArrayList<RegistroDespesa> todosDados = new ArrayList<>();
            
            // PASSO 2: Processar cada trimestre
            System.out.println("[PASSO 1] Processando trimestres...");
            todosDados.addAll(processar2024_Trimestre1());
            todosDados.addAll(processar2024_Trimestre2());
            todosDados.addAll(processar2024_Trimestre3());
            
            System.out.println("   ✓ Total de registros: " + todosDados.size());
            System.out.println();
            
            // PASSO 3: Salvar em arquivo CSV
            System.out.println("[PASSO 2] Salvando arquivo CSV...");
            salvarCSV(todosDados);
            System.out.println("   ✓ Arquivo criado: consolidado_despesas.csv");
            System.out.println();
            
            // PASSO 4: Mostrar estatísticas
            mostrarEstatisticas(todosDados);
            
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("║         PROCESSAMENTO CONCLUÍDO!           ║");
            System.out.println("╚════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("❌ ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processa dados do 1º trimestre de 2024
     * (Em produção real, baixaria da internet. Aqui estou simulando)
     */
    private static ArrayList<RegistroDespesa> processar2024_Trimestre1() {
        System.out.println("   → Processando 2024 - 1º Trimestre");
        
        ArrayList<RegistroDespesa> dados = new ArrayList<>();
        
        // Cria alguns registros de exemplo
        dados.add(new RegistroDespesa(
            "12.345.678/0001-90",           // CNPJ
            "Operadora Saúde SP",            // Nome da empresa
            1,                               // Trimestre
            2024,                            // Ano
            1500000.50                       // Valor em reais
        ));
        
        dados.add(new RegistroDespesa(
            "98.765.432/0001-10",
            "Plano Saúde RJ",
            1,
            2024,
            2300000.00
        ));
        
        // Exemplo de dado com problema (valor zerado)
        dados.add(new RegistroDespesa(
            "11.222.333/0001-44",
            "Assistência MG",
            1,
            2024,
            0.0                              // ⚠️ Valor suspeito!
        ));
        
        return dados;
    }
    
    /**
     * Processa dados do 2º trimestre de 2024
     */
    private static ArrayList<RegistroDespesa> processar2024_Trimestre2() {
        System.out.println("   → Processando 2024 - 2º Trimestre");
        
        ArrayList<RegistroDespesa> dados = new ArrayList<>();
        
        dados.add(new RegistroDespesa(
            "12.345.678/0001-90",
            "Operadora Saúde SP",
            2,
            2024,
            1600000.75
        ));
        
        dados.add(new RegistroDespesa(
            "98.765.432/0001-10",
            "Plano Saúde RJ",
            2,
            2024,
            2400000.00
        ));
        
        // Exemplo de dado com problema (valor negativo)
        dados.add(new RegistroDespesa(
            "11.222.333/0001-44",
            "Assistência MG",
            2,
            2024,
            -5000.00                         // ⚠️ Valor negativo!
        ));
        
        return dados;
    }
    
    /**
     * Processa dados do 3º trimestre de 2024
     */
    private static ArrayList<RegistroDespesa> processar2024_Trimestre3() {
        System.out.println("   → Processando 2024 - 3º Trimestre");
        
        ArrayList<RegistroDespesa> dados = new ArrayList<>();
        
        dados.add(new RegistroDespesa(
            "12.345.678/0001-90",
            "Operadora Saúde SP",
            3,
            2024,
            1750000.25
        ));
        
        dados.add(new RegistroDespesa(
            "98.765.432/0001-10",
            "Plano Saúde RJ",
            3,
            2024,
            2500000.00
        ));
        
        dados.add(new RegistroDespesa(
            "11.222.333/0001-44",
            "Assistência MG",
            3,
            2024,
            800000.00
        ));
        
        return dados;
    }
    
    /**
     * Salva todos os dados em arquivo CSV
     * 
     * CSV = Comma Separated Values (valores separados por vírgula)
     * É como uma planilha do Excel, mas em texto simples
     */
    private static void salvarCSV(ArrayList<RegistroDespesa> dados) throws Exception {
        // FileWriter = classe para escrever em arquivo
        // PrintWriter = facilita escrever linhas de texto
        // try-with-resources = fecha o arquivo automaticamente
        
        try (PrintWriter writer = new PrintWriter(new FileWriter("consolidado_despesas.csv"))) {
            
            // Escreve o cabeçalho (primeira linha)
            writer.println("CNPJ;RazaoSocial;Trimestre;Ano;ValorDespesas");
            
            // Escreve cada registro
            // for-each: percorre todos os itens da lista
            for (RegistroDespesa reg : dados) {
                writer.println(reg.paraCSV());
            }
        }
        // Arquivo é fechado automaticamente aqui
    }
    
    /**
     * Mostra estatísticas dos dados processados
     */
    private static void mostrarEstatisticas(ArrayList<RegistroDespesa> dados) {
        System.out.println("[ESTATÍSTICAS]");
        
        // Conta problemas encontrados
        int valoresNegativos = 0;
        int valoresZerados = 0;
        
        for (RegistroDespesa reg : dados) {
            if (reg.valor < 0) {
                valoresNegativos++;
            }
            if (reg.valor == 0) {
                valoresZerados++;
            }
        }
        
        System.out.println("   • Total de registros: " + dados.size());
        System.out.println("   • Valores negativos encontrados: " + valoresNegativos);
        System.out.println("   • Valores zerados encontrados: " + valoresZerados);
        System.out.println();
    }
}

/**
 * =============================================================================
 * CLASSE AUXILIAR: RegistroDespesa
 * =============================================================================
 * 
 * Esta classe representa UM REGISTRO de despesa.
 * É como uma "ficha" com as informações de uma operadora em um trimestre.
 * 
 * CONCEITO DE POO: Encapsulamento
 * - Agrupa dados relacionados (CNPJ, nome, valor, etc)
 * - Fica mais fácil de trabalhar com os dados
 * =============================================================================
 */
class RegistroDespesa {
    // Atributos (características do objeto)
    String cnpj;           // CPF da empresa
    String razaoSocial;    // Nome da empresa
    int trimestre;         // Qual trimestre (1, 2, 3 ou 4)
    int ano;               // Qual ano
    double valor;          // Quanto gastou (em reais)
    
    /**
     * Construtor: método especial que cria o objeto
     * É chamado quando fazemos: new RegistroDespesa(...)
     */
    public RegistroDespesa(String cnpj, String razaoSocial, int trimestre, 
                           int ano, double valor) {
        this.cnpj = cnpj;
        this.razaoSocial = razaoSocial;
        this.trimestre = trimestre;
        this.ano = ano;
        this.valor = valor;
    }
    
    /**
     * Converte o objeto para formato CSV
     * Exemplo: "12.345.678/0001-90;Operadora SP;1;2024;1500000.50"
     */
    public String paraCSV() {
        // String.format = cria texto formatado (como printf em C)
        // %.2f = número com 2 casas decimais
        return String.format("%s;%s;%d;%d;%.2f",
            cnpj, razaoSocial, trimestre, ano, valor);
    }
}
