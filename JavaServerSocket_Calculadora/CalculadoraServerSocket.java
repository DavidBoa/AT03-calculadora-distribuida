import java.io.BufferedReader;                  
import java.io.DataOutputStream;                
import java.io.IOException;                     
import java.io.InputStreamReader;               
import java.net.ServerSocket;                   
import java.net.Socket;                         

/**
 * Servidor TCP da calculadora.
 *
 * PROTOCOLO (texto, uma requisicao por conexao, cada campo em uma linha terminada por '\n'):
 *   Operacao basica:  codigo ("1"=somar "2"=subtrair "3"=dividir "4"=multiplicar), oper1, oper2
 *   Expressao:        "5", expressao (ex.: (10+15)*4)
 *   Resposta:         uma linha com o resultado, ou "ERRO: <mensagem>"
 */
public class CalculadoraServerSocket {

    private static final int PORTA = 9090;

    public static void main(String[] args) {
        ServerSocket welcomeSocket;                         
        Calculadora calc = new Calculadora();               // Objeto com as regras da calculadora
        try {                                               // Bloco que captura falhas de rede ao abrir a porta
            welcomeSocket = new ServerSocket(PORTA);        
            int i = 0;                                      
            System.out.println("Servidor no ar");           
            while (true) {                                  
                Socket connectionSocket = welcomeSocket.accept();   // Bloqueia ate um cliente conectar
                i++;                                        
                System.out.println("Nova conexao (" + i + ")");     
                atender(connectionSocket, calc);            // Processa a requisicao deste cliente
            }
        } catch (IOException e) {                           // Falha ao abrir a porta (ex.: porta em uso)
            e.printStackTrace();                            // Mostra o erro no console
        }
    }

    // Le a requisicao de um cliente, calcula e envia a resposta
    private static void atender(Socket connectionSocket, Calculadora calc) {
        // try-with-resources: garante que o socket do cliente seja fechado ao final
        try (Socket s = connectionSocket) {
            // Cria um leitor de linhas sobre o fluxo de entrada (bytes) do socket
            BufferedReader socketEntrada = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String result;                                  // Texto que sera enviado de volta ao cliente
            try {                                           // Erros de calculo viram mensagem de erro, nao derrubam o servidor
                String operacao = socketEntrada.readLine(); // 1a linha: codigo da operacao
                if (operacao == null) {                     // Cliente fechou sem enviar nada
                    throw new IllegalArgumentException("requisicao vazia");
                }
                if (operacao.equals("5")) {                 // Codigo 5: expressao completa (abordagem 2)
                    String expressao = socketEntrada.readLine();    // 2a linha: texto da expressao
                    if (expressao == null) {                // Cliente nao enviou a expressao
                        throw new IllegalArgumentException("expressao ausente");
                    }
                    result = "" + calc.calculaExpressao(expressao); // Servidor calcula a expressao inteira
                } else {                                    // Demais codigos: operacao basica com dois operandos
                    String oper1 = socketEntrada.readLine();        // 2a linha: primeiro operando
                    String oper2 = socketEntrada.readLine();        // 3a linha: segundo operando
                    if (oper1 == null || oper2 == null) {   // Faltou algum operando
                        throw new IllegalArgumentException("operandos ausentes");
                    }
                    double a = Double.parseDouble(oper1);   // Converte o texto do 1o operando para numero
                    double b = Double.parseDouble(oper2);   // Converte o texto do 2o operando para numero
                    double r;                               // Resultado numerico da operacao
                    switch (operacao) {                     // Escolhe a operacao pelo codigo recebido
                        case "1": r = calc.soma(a, b); break;       // 1 = somar
                        case "2": r = calc.subtrai(a, b); break;    // 2 = subtrair
                        case "3": r = calc.divide(a, b); break;     // 3 = dividir
                        case "4": r = calc.multiplica(a, b); break; // 4 = multiplicar
                        default: throw new IllegalArgumentException("operacao desconhecida: " + operacao);
                    }
                    result = "" + r;                        // Converte o resultado para texto
                }
            } catch (Exception e) {                         // Qualquer erro na leitura/calculo
                result = "ERRO: " + e.getMessage();         
            }
            // Cria o fluxo de saida do socket para responder ao cliente
            DataOutputStream socketOutput = new DataOutputStream(s.getOutputStream());
            socketOutput.writeBytes(result + '\n');         // Envia a resposta terminada em quebra de linha
            System.out.println(result);                     
            socketOutput.flush();                           // Forca o envio imediato dos bytes pela rede
        } catch (IOException e) {                           // Falha de rede com este cliente especifico
            e.printStackTrace();                            // Mostra o erro, mas o servidor continua no ar
        }
    }
}
