import java.sql.*;

/**
 * Classe para executar queries no banco de dados PostgreSQL
 * 
 * IMPORTANTE: Este código requer o driver JDBC do PostgreSQL
 * Adicionar ao classpath: postgresql-42.x.x.jar
 * Download: https://jdbc.postgresql.org/download.html
 */
public class ConsultasBancoDados {
    
    // Configurações de conexão
    private static final String URL = "jdbc:postgresql://localhost:5432/ans_database";
    private static final String USUARIO = "postgres";
    private static final String SENHA = "sua_senha_aqui";
    
    public static void main(String[] args) {
        ConsultasBancoDados consultas = new ConsultasBancoDados();
        
        try {
            System.out.println("=== CONECTANDO AO BANCO DE DADOS ===\n");
            
            Connection conn = consultas.conectar();
            
            // Executa as 3 queries principais
            System.out.println("=== QUERY 1: Top 5 Crescimento ===");
            consultas.executarQuery1(conn);
            
            System.out.println("\n=== QUERY 2: Distribuição por UF ===");
            consultas.executarQuery2(conn);
            
            System.out.println("\n=== QUERY 3: Operadoras Acima da Média ===");
            consultas.executarQuery3(conn);
            
            conn.close();
            System.out.println("\n=== CONSULTAS CONCLUÍDAS ===");
            
        } catch (Exception e) {
            System.err.println("ERRO: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Estabelece conexão com o banco de dados
     */
    private Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }
    
    /**
     * Query 1: Top 5 operadoras com maior crescimento
     */
    private void executarQuery1(Connection conn) throws SQLException {
        String sql = 
            "WITH trimestres_disponiveis AS ( " +
            "  SELECT MIN(ano * 10 + trimestre) as primeiro_periodo, " +
            "         MAX(ano * 10 + trimestre) as ultimo_periodo " +
            "  FROM despesas_consolidadas " +
            "), " +
            "despesas_por_periodo AS ( " +
            "  SELECT d.cnpj, d.razao_social, " +
            "         MAX(CASE WHEN d.ano * 10 + d.trimestre = td.primeiro_periodo " +
            "                  THEN d.valor_despesas END) as valor_primeiro, " +
            "         MAX(CASE WHEN d.ano * 10 + d.trimestre = td.ultimo_periodo " +
            "                  THEN d.valor_despesas END) as valor_ultimo " +
            "  FROM despesas_consolidadas d " +
            "  CROSS JOIN trimestres_disponiveis td " +
            "  GROUP BY d.cnpj, d.razao_social " +
            "  HAVING MAX(CASE WHEN d.ano * 10 + d.trimestre = td.primeiro_periodo THEN 1 END) = 1 " +
            "     AND MAX(CASE WHEN d.ano * 10 + d.trimestre = td.ultimo_periodo THEN 1 END) = 1 " +
            ") " +
            "SELECT cnpj, razao_social, valor_primeiro, valor_ultimo, " +
            "       ROUND(((valor_ultimo - valor_primeiro) / NULLIF(valor_primeiro, 0)) * 100, 2) as crescimento " +
            "FROM despesas_por_periodo " +
            "WHERE valor_primeiro > 0 " +
            "ORDER BY crescimento DESC " +
            "LIMIT 5";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int pos = 1;
            while (rs.next()) {
                System.out.println(String.format(
                    "%d. %s (CNPJ: %s)\n   Crescimento: %.2f%% (de R$ %.2f para R$ %.2f)",
                    pos++,
                    rs.getString("razao_social"),
                    rs.getString("cnpj"),
                    rs.getDouble("crescimento"),
                    rs.getDouble("valor_primeiro"),
                    rs.getDouble("valor_ultimo")
                ));
            }
        }
    }
    
    /**
     * Query 2: Distribuição por UF
     */
    private void executarQuery2(Connection conn) throws SQLException {
        String sql = 
            "SELECT o.uf, " +
            "       COUNT(DISTINCT d.cnpj) as numero_operadoras, " +
            "       SUM(d.valor_despesas) as total_despesas, " +
            "       ROUND(SUM(d.valor_despesas) / NULLIF(COUNT(DISTINCT d.cnpj), 0), 2) as media_por_operadora " +
            "FROM despesas_consolidadas d " +
            "INNER JOIN operadoras o ON d.cnpj = o.cnpj " +
            "WHERE o.uf IS NOT NULL " +
            "GROUP BY o.uf " +
            "ORDER BY total_despesas DESC " +
            "LIMIT 5";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int pos = 1;
            while (rs.next()) {
                System.out.println(String.format(
                    "%d. %s - R$ %.2f total (%d operadoras, média R$ %.2f por operadora)",
                    pos++,
                    rs.getString("uf"),
                    rs.getDouble("total_despesas"),
                    rs.getInt("numero_operadoras"),
                    rs.getDouble("media_por_operadora")
                ));
            }
        }
    }
    
    /**
     * Query 3: Operadoras acima da média em 2+ trimestres
     */
    private void executarQuery3(Connection conn) throws SQLException {
        String sql = 
            "WITH media_geral AS ( " +
            "  SELECT AVG(valor_despesas) as media FROM despesas_consolidadas " +
            "), " +
            "operadoras_acima_media AS ( " +
            "  SELECT d.cnpj, d.razao_social, " +
            "         CASE WHEN d.valor_despesas > mg.media THEN 1 ELSE 0 END as acima_media, " +
            "         d.valor_despesas " +
            "  FROM despesas_consolidadas d CROSS JOIN media_geral mg " +
            "), " +
            "contagem_trimestres AS ( " +
            "  SELECT cnpj, razao_social, " +
            "         SUM(acima_media) as trimestres_acima_media, " +
            "         COUNT(*) as total_trimestres, " +
            "         ROUND(AVG(valor_despesas), 2) as media_operadora " +
            "  FROM operadoras_acima_media " +
            "  GROUP BY cnpj, razao_social " +
            ") " +
            "SELECT cnpj, razao_social, trimestres_acima_media, total_trimestres, media_operadora " +
            "FROM contagem_trimestres " +
            "WHERE trimestres_acima_media >= 2 " +
            "ORDER BY trimestres_acima_media DESC, media_operadora DESC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int contador = 0;
            while (rs.next()) {
                contador++;
                System.out.println(String.format(
                    "- %s (CNPJ: %s)\n  Acima da média em %d de %d trimestres (média: R$ %.2f)",
                    rs.getString("razao_social"),
                    rs.getString("cnpj"),
                    rs.getInt("trimestres_acima_media"),
                    rs.getInt("total_trimestres"),
                    rs.getDouble("media_operadora")
                ));
            }
            System.out.println("\nTotal: " + contador + " operadoras");
        }
    }
}
