import java.rmi.RemoteException;                    
import java.rmi.registry.LocateRegistry;            
import java.rmi.registry.Registry;                  
import java.rmi.server.UnicastRemoteObject;         

/**
 * Objeto remoto + servidor da calculadora.
 * - Implementa ICalculadora: os metodos publicos abaixo sao os que o cliente invoca pela rede.
 * - O main() cria o Registry, exporta o objeto e o registra com o nome "calculadora".
 * Diferente do socket, aqui NAO ha protocolo de texto: o RMI converte parametros e resultados sozinho.
 */
public class Calculadora implements ICalculadora {

	private static final int PORTA_REGISTRY = 1099;     // Porta onde o Registry escuta (padrao do RMI)
	private static final int PORTA_OBJETO = 1100;       // Porta onde o objeto remoto recebe as chamadas

	private static int chamadas = 0;                    // Contador de chamadas recebidas

	// Registra no console cada chamada remota recebida.
	// synchronized porque o RMI atende varios clientes em threads diferentes ao mesmo tempo.
	private static synchronized void registrar(String metodo) {
		System.out.println("Metodo " + metodo + " chamado " + chamadas++);   // Mostra o nome do metodo e o numero da chamada
	}

	// Unico ponto onde a aritmetica e feita; usado pelos metodos remotos e pelo avaliador de expressoes
	private double operar(char op, double a, double b) {
		switch (op) {                                   // Escolhe a operacao pelo simbolo
			case '+': return a + b;                     
			case '-': return a - b;                     
			case '*': return a * b;                     
			case '/':                                   
				if (b == 0) {                           // Em double, x/0 daria Infinity; coloquei erro explicito
					throw new ArithmeticException("divisao por zero");
				}
				return a / b;                           
			default:                                    
				throw new IllegalArgumentException("operador desconhecido: " + op);
		}
	}

	public double soma(double a, double b) throws RemoteException {
		registrar("soma");                              
		return operar('+', a, b);                       
	}

	public double subtrai(double a, double b) throws RemoteException {
		registrar("subtrai");                           
		return operar('-', a, b);                       
	}

	public double multiplica(double a, double b) throws RemoteException {
		registrar("multiplica");                        
		return operar('*', a, b);                       
	}

	public double divide(double a, double b) throws RemoteException {
		registrar("divide");                            
		return operar('/', a, b);                       // Calcula (ou lanca ArithmeticException se b == 0)
	}

	// Abordagem 2: o servidor interpreta a expressao e faz todas as contas localmente
	public double calculaExpressao(String expressao) throws RemoteException {
		registrar("calculaExpressao");                  // Uma unica chamada remota, independente do tamanho da expressao
		try {                                           // O avaliador declara "throws Exception", entao precisa tratar
			// O avaliador le o texto e chama operar() a cada conta; nenhuma dessas contas passa pela rede
			return new AvaliadorExpressao(expressao, (op, a, b) -> operar(op, a, b)).avaliar();
		} catch (RuntimeException e) {                  // Expressao invalida ou divisao por zero
			throw e;                                    // Repassa; o RMI entrega a mesma excecao ao cliente
		} catch (Exception e) {                         // Qualquer outra excecao verificada (nao esperada)
			throw new IllegalStateException(e.getMessage());
		}
	}

	public static void main(String[] args) throws RemoteException {
		if (args.length > 0) {                          // Se o IP deste servidor foi passado como argumento
			// Informa ao RMI qual IP colocar no objeto entregue aos clientes (necessario com duas maquinas)
			System.setProperty("java.rmi.server.hostname", args[0]);
		}
		Calculadora calculadora = new Calculadora();    // Cria o objeto que sera acessado remotamente
		Registry reg = null;                            // Referencia ao Registry
		// Exporta o objeto na porta 1100 e obtem o "stub": o representante que sera entregue aos clientes
		ICalculadora stub = (ICalculadora) UnicastRemoteObject.exportObject(calculadora, PORTA_OBJETO);
		try {                                           // Tenta criar um Registry novo
			System.out.println("Creating registry...");
			reg = LocateRegistry.createRegistry(PORTA_REGISTRY);    // Cria o Registry na porta 1099
		} catch (Exception e) {                         // Falhou, por exemplo porque ja existe um Registry
			try {
				reg = LocateRegistry.getRegistry(PORTA_REGISTRY);   // Usa o Registry que ja esta rodando
			} catch (Exception e1) {                    // Nao foi possivel nem localizar
				System.exit(0);                         // Encerra o programa
			}
		}
		reg.rebind("calculadora", stub);                // Registra o stub com o nome que os clientes vao procurar
		System.out.println("Servidor no ar");           
	}
}
