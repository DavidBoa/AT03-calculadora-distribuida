import java.rmi.NotBoundException;                  
import java.rmi.RemoteException;                    
import java.rmi.registry.LocateRegistry;            
import java.rmi.registry.Registry;                  
import java.util.Scanner;                           

/**
 * Cliente RMI da calculadora, com menu textual.
 *
 * Uso: java CalculadoraCliente [ip_do_servidor]   (padrao: localhost)
 *
 * Opcoes do menu:
 *   1-4  operacao basica executada no servidor
 *   5    expressao - ABORDAGEM 1: o cliente interpreta e chama o servidor a cada operacao
 *   6    expressao - ABORDAGEM 2: o cliente envia a expressao inteira e o servidor calcula
 */
public class CalculadoraCliente {

	private static final int PORTA_REGISTRY = 1099;     // Porta do Registry (a mesma usada pelo servidor)
	private static ICalculadora calc;                   // Representante (stub) do objeto remoto; chamar seus metodos vai ao servidor

	// Executa UMA operacao basica no servidor (usada pelas opcoes 1-4 e pela abordagem 1)
	private static double operacaoRemota(char op, double a, double b) throws RemoteException {
		switch (op) {                                   // Escolhe o metodo remoto pelo simbolo do operador
			case '+': return calc.soma(a, b);           
			case '-': return calc.subtrai(a, b);        
			case '*': return calc.multiplica(a, b);     
			case '/': return calc.divide(a, b);         
			default: throw new IllegalArgumentException("operador desconhecido: " + op);
		}
	}

	// Converte o texto digitado em numero, aceitando virgula como separador decimal
	private static double lerNumero(Scanner teclado, String rotulo) {
		System.out.print(rotulo);                       // Pede o valor ao usuario
		return Double.parseDouble(teclado.nextLine().trim().replace(',', '.'));
	}

	public static void main(String[] args) {
		String host = "localhost";                      // Maquina onde o servidor esta (padrao: a propria)
		if (args.length > 0) {                          // Se o IP do servidor foi passado como argumento
			host = args[0];                             // Usa o IP informado
		}
		try {                                           // Conexao inicial com o servidor
			Registry reg = LocateRegistry.getRegistry(host, PORTA_REGISTRY);    // Localiza o Registry do servidor
			calc = (ICalculadora) reg.lookup("calculadora");    // Procura o objeto pelo nome e recebe o stub
		} catch (RemoteException | NotBoundException e) {       // Servidor fora do ar ou objeto nao registrado
			System.out.println(e);                      // Mostra o motivo
			System.exit(0);                             // Sem servidor nao ha o que fazer
		}
		Scanner teclado = new Scanner(System.in);       
		while (true) {                                  
			System.out.println("\n=== Calculadora Distribuida (RMI) - servidor: " + host + " ===");
			System.out.println("1 - Somar");
			System.out.println("2 - Subtrair");
			System.out.println("3 - Dividir");
			System.out.println("4 - Multiplicar");
			System.out.println("5 - Expressao (cliente interpreta e chama o servidor por operacao)");
			System.out.println("6 - Expressao (servidor calcula tudo)");
			System.out.println("0 - Sair");
			System.out.print("Opcao: ");
			String opcao = teclado.nextLine().trim();   // Le a opcao escolhida
			if (opcao.equals("0")) {                    
				break;                                  
			}
			try {                                       // Erros de uma operacao nao encerram o programa
				if (opcao.matches("[1-4]")) {           // Operacoes basicas 1 a 4
					double a = lerNumero(teclado, "Operando 1: ");  // Le o primeiro operando
					double b = lerNumero(teclado, "Operando 2: ");  // Le o segundo operando
					char op = "+-/*".charAt(Integer.parseInt(opcao) - 1);   // 1:+ 2:- 3:/ 4:*
					System.out.println("resultado=" + operacaoRemota(op, a, b));
				} else if (opcao.equals("5") || opcao.equals("6")) {    // Expressoes
					System.out.print("Expressao (ex.: (10+15)*4): ");
					String expressao = teclado.nextLine();  // Le a expressao digitada
					if (opcao.equals("5")) {            // ABORDAGEM 1: interpretacao no cliente
						// O avaliador percorre a expressao e, a cada operacao basica, chama operacaoRemota (rede)
						double r = new AvaliadorExpressao(expressao, CalculadoraCliente::operacaoRemota).avaliar();
						System.out.println("resultado=" + r);
					} else {                            // ABORDAGEM 2: calculo no servidor
						// Uma unica chamada remota: o servidor interpreta o texto e devolve o resultado
						System.out.println("resultado=" + calc.calculaExpressao(expressao));
					}
				} else {                                // Qualquer outra entrada
					System.out.println("Opcao invalida.");
				}
			} catch (RemoteException e) {               // Falha de rede (servidor caiu no meio do uso)
				System.out.println("Erro de comunicacao: " + e.getMessage());
			} catch (Exception e) {                     // Erro de conta/entrada (div. por zero, expressao invalida)
				System.out.println("Erro: " + e.getMessage());
			}
		}
		teclado.close();                                
		System.out.println("Cliente encerrado.");
	}
}
