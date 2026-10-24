/**
 * Regras de negocio da calculadora (independente de rede).
 * A classe CalculadoraServerSocket apenas recebe os dados do socket e chama estes metodos.
 */
public class Calculadora {

    public String sayHello(String nome, String sobrenome) {
        return "Fala " + nome + " " + sobrenome;
    }

    public double soma(double oper1, double oper2) {
        return oper1 + oper2;
    }

    public double subtrai(double oper1, double oper2) {
        return oper1 - oper2;
    }

    public double multiplica(double oper1, double oper2) {
        return oper1 * oper2;
    }

    // (recusa divisao por zero)
    public double divide(double oper1, double oper2) {
        // Em double, x/0 daria Infinity; prefiro sinalizar erro explicito
        if (oper2 == 0) {
            throw new ArithmeticException("divisao por zero");
        }
        return oper1 / oper2;
    }

    // Abordagem 2: o servidor recebe a expressao pronta e calcula tudo localmente
    public double calculaExpressao(String expressao) throws Exception {
        // O avaliador chama aplicar() para cada operacao basica encontrada na expressao
        return new AvaliadorExpressao(expressao, (op, a, b) -> aplicar(op, a, b)).avaliar();
    }

    // Traduz o simbolo do operador para o metodo correspondente desta classe
    private double aplicar(char operador, double a, double b) {
        switch (operador) {
            case '+': return soma(a, b);
            case '-': return subtrai(a, b);
            case '*': return multiplica(a, b);
            case '/': return divide(a, b);
            default: throw new IllegalArgumentException("operador desconhecido: " + operador);
        }
    }
}
