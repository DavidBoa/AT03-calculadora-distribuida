import java.rmi.Remote;                         
import java.rmi.RemoteException;                

/**
 * Contrato da calculadora remota: tudo que o cliente pode pedir ao servidor.
 * O cliente so enxerga esta interface; a implementacao fica na classe Calculadora (no servidor).
 */
public interface ICalculadora extends Remote {         // Todo objeto remoto precisa estender Remote

	public double soma(double a, double b) throws RemoteException;          

	public double subtrai(double a, double b) throws RemoteException;       

	public double multiplica(double a, double b) throws RemoteException;    

	// Divisao: a / b. Se b for zero, o servidor lanca ArithmeticException, que o RMI repassa ao cliente
	public double divide(double a, double b) throws RemoteException;

	// Abordagem 2: o servidor recebe a expressao inteira (ex.: "(10+15)*4") e devolve o resultado
	public double calculaExpressao(String expressao) throws RemoteException;
}
