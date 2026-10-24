import java.io.BufferedReader;                  
import java.io.DataOutputStream;                
import java.io.IOException;                     
import java.io.InputStreamReader;               
import java.net.Socket;                         
import java.util.Scanner;                       

/**
 * Cliente TCP da calculadora, com menu textual.
 *
 * Uso: java CalculadoraClientSocket [ip_do_servidor]   (padrao: localhost)
 *
 * Opcoes do menu:
 *   1-4  operacao basica enviada ao servidor
 *   5    expressao - ABORDAGEM 1: o cliente interpreta e faz varias chamadas ao servidor
 *   6    expressao - ABORDAGEM 2: o cliente envia a expressao inteira e o servidor calcula
 */
public class CalculadoraClientSocket {

    private static String host = "localhost";       
    private static final int PORTA = 9090;          
    private static final int OP_EXPRESSAO = 5;      

    
    private static String enviar(String... linhas) throws IOException {
        // try-with-resources: o socket e fechado automaticamente ao sair do bloco
        try (Socket clientSocket = new Socket(host, PORTA)) {
            // Fluxo de saida para enviar dados ao servidor
            DataOutputStream socketSaidaServer = new DataOutputStream(clientSocket.getOutputStream());
            for (String linha : linhas) {                       // Percorre cada campo da requisicao
                socketSaidaServer.writeBytes(linha + "\n");     // Envia o campo terminado em quebra de linha
            }
            socketSaidaServer.flush();                          // Forca o envio imediato pela rede
            // Leitor de linhas sobre o fluxo de entrada do socket (resposta do servidor)
            BufferedReader messageFromServer = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            String result = messageFromServer.readLine();       // Le a unica linha de resposta
            if (result == null) {                               // Servidor fechou sem responder
                throw new IOException("servidor encerrou a conexao sem resposta");
            }
            if (result.startsWith("ERRO:")) {                   // Servidor sinalizou erro (ex.: divisao por zero)
                throw new ArithmeticException(result.substring(5).trim());
            }
            return result;                                      // Devolve o resultado como texto
        }
    }

    // Executa UMA operacao basica no servidor
    private static double operacaoRemota(char op, double a, double b) throws IOException {
        int codigo;                                             
        switch (op) {                                           // Converte o simbolo para o codigo numerico
            case '+': codigo = 1; break;                        // 1 = somar
            case '-': codigo = 2; break;                        // 2 = subtrair
            case '/': codigo = 3; break;                        // 3 = dividir
            case '*': codigo = 4; break;                        // 4 = multiplicar
            default: throw new IllegalArgumentException("operador desconhecido: " + op);
        }
        // Envia codigo, operando 1 e operando 2 e converte a resposta para numero
        return Double.parseDouble(enviar("" + codigo, "" + a, "" + b));
    }

    // Converte o texto digitado em numero, aceitando virgula como separador decimal
    private static double lerNumero(Scanner teclado, String rotulo) {
        System.out.print(rotulo);                               // Pede o valor ao usuario
        return Double.parseDouble(teclado.nextLine().trim().replace(',', '.'));
    }

    public static void main(String[] args) {
        if (args.length > 0) {                                  // Se o IP do servidor foi passado como argumento
            host = args[0];                                     // Usa o IP informado
        }
        Scanner teclado = new Scanner(System.in);              
        while (true) {                                          
            System.out.println("\n=== Calculadora Distribuida (Sockets) - servidor: " + host + " ===");
            System.out.println("1 - Somar");
            System.out.println("2 - Subtrair");
            System.out.println("3 - Dividir");
            System.out.println("4 - Multiplicar");
            System.out.println("5 - Expressao (cliente interpreta e chama o servidor por operacao)");
            System.out.println("6 - Expressao (servidor calcula tudo)");
            System.out.println("0 - Sair");
            System.out.print("Opcao: ");
            String opcao = teclado.nextLine().trim();           
            if (opcao.equals("0")) {                            
                break;                                          
            }
            try {                                               // Erros de uma operacao nao encerram o programa
                if (opcao.matches("[1-4]")) {                   // Operacoes basicas 1 a 4
                    double a = lerNumero(teclado, "Operando 1: ");  // Le o primeiro operando
                    double b = lerNumero(teclado, "Operando 2: ");  // Le o segundo operando
                    char op = "+-/*".charAt(Integer.parseInt(opcao) - 1);   // 1:+ 2:- 3:/ 4:*
                    System.out.println("resultado=" + operacaoRemota(op, a, b));
                } else if (opcao.equals("5") || opcao.equals("6")) {    // Expressoes
                    System.out.print("Expressao (ex.: (10+15)*4): ");
                    String expressao = teclado.nextLine();      // Le a expressao digitada
                    if (opcao.equals("5")) {                    // ABORDAGEM 1: interpretacao no cliente
                        // O avaliador percorre a expressao e, a cada operacao basica, chama operacaoRemota (rede)
                        double r = new AvaliadorExpressao(expressao, CalculadoraClientSocket::operacaoRemota).avaliar();
                        System.out.println("resultado=" + r);
                    } else {                                    // ABORDAGEM 2: calculo no servidor
                        // Envia o codigo de expressao e o texto; o servidor interpreta e devolve o resultado
                        System.out.println("resultado=" + enviar("" + OP_EXPRESSAO, expressao.trim()));
                    }
                } else {                                        // Qualquer outra entrada
                    System.out.println("Opcao invalida.");
                }
            } catch (IOException e) {                           // Falha de rede (servidor fora do ar, IP errado)
                System.out.println("Erro de comunicacao: " + e.getMessage());
            } catch (Exception e) {                             // Erro de calculo/entrada (div. por zero, expressao invalida)
                System.out.println("Erro: " + e.getMessage());
            }
        }
        teclado.close();                                        // Libera o leitor do teclado
        System.out.println("Cliente encerrado.");
    }
}
