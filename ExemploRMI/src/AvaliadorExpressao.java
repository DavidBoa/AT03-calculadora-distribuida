/**
 * Avaliador de expressoes aritmeticas (descida recursiva).
 * Suporta + - * / , parenteses, numeros decimais (com ponto) e sinal unario (-5, +5).
 *
 * Gramatica:
 *   expressao := termo (('+' | '-') termo)*
 *   termo     := fator (('*' | '/') fator)*
 *   fator     := '-' fator | '+' fator | '(' expressao ')' | numero
 *
 * A classe NAO sabe como a conta e feita: cada operacao basica e delegada a um "Operador".
 *  - No SERVIDOR (abordagem 2) o Operador calcula localmente.
 *  - No CLIENTE (abordagem 1) o Operador faz uma chamada de rede ao servidor para cada operacao.
 * Este mesmo arquivo e usado nos dois projetos.
 */
public class AvaliadorExpressao {

    // Interface que representa "como executar uma operacao basica" (soma, subtracao, etc.)
    public interface Operador {
        // Aplica o operador ('+', '-', '*' ou '/') sobre os dois valores e devolve o resultado
        double aplicar(char operador, double a, double b) throws Exception;
    }

    // Expressao a ser avaliada, ja sem espacos em branco
    private final String texto;
    // Posicao atual de leitura dentro do texto
    private int pos;
    // Estrategia usada para executar cada operacao basica
    private final Operador operador;

    // Construtor: recebe a expressao e o Operador que executara as contas
    public AvaliadorExpressao(String expressao, Operador operador) {
        // Remove todos os espacos para simplificar a leitura caractere a caractere
        this.texto = expressao.replaceAll("\\s+", "");
        // Comeca a leitura na primeira posicao
        this.pos = 0;
        // Guarda o Operador recebido
        this.operador = operador;
    }

    // Metodo publico: avalia a expressao inteira e devolve o resultado
    public double avaliar() throws Exception {
        // Expressao vazia nao e valida
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("expressao vazia");
        }
        // Avalia a expressao a partir do inicio do texto
        double valor = expressao();
        // Se sobrou texto sem ser lido, a expressao esta malformada (ex.: "2+3)")
        if (pos < texto.length()) {
            throw new IllegalArgumentException("caractere inesperado: '" + texto.charAt(pos) + "'");
        }
        // Devolve o resultado final
        return valor;
    }

    // expressao := termo (('+' | '-') termo)*   -> soma e subtracao (menor precedencia)
    private double expressao() throws Exception {
        // Le o primeiro termo
        double valor = termo();
        // Enquanto o proximo caractere for + ou -, continua acumulando
        while (pos < texto.length() && (texto.charAt(pos) == '+' || texto.charAt(pos) == '-')) {
            // Guarda o operador lido e avanca a posicao
            char op = texto.charAt(pos++);
            // Le o termo da direita
            double direita = termo();
            // Delega a operacao ao Operador (local ou remoto)
            valor = operador.aplicar(op, valor, direita);
        }
        // Devolve o valor acumulado
        return valor;
    }

    // termo := fator (('*' | '/') fator)*   -> multiplicacao e divisao (maior precedencia)
    private double termo() throws Exception {
        // Le o primeiro fator
        double valor = fator();
        // Enquanto o proximo caractere for * ou /, continua acumulando
        while (pos < texto.length() && (texto.charAt(pos) == '*' || texto.charAt(pos) == '/')) {
            // Guarda o operador lido e avanca a posicao
            char op = texto.charAt(pos++);
            // Le o fator da direita
            double direita = fator();
            // Delega a operacao ao Operador (local ou remoto)
            valor = operador.aplicar(op, valor, direita);
        }
        // Devolve o valor acumulado
        return valor;
    }

    // fator := '-' fator | '+' fator | '(' expressao ')' | numero
    private double fator() throws Exception {
        // Se acabou o texto onde se esperava um valor, a expressao esta incompleta (ex.: "2+")
        if (pos >= texto.length()) {
            throw new IllegalArgumentException("expressao incompleta");
        }
        // Olha o caractere atual sem consumi-lo
        char c = texto.charAt(pos);
        // Sinal negativo unario: -x e calculado como 0 - x
        if (c == '-') {
            // Consome o '-'
            pos++;
            // Aplica 0 - fator usando o Operador
            return operador.aplicar('-', 0, fator());
        }
        // Sinal positivo unario: +x e simplesmente x
        if (c == '+') {
            // Consome o '+'
            pos++;
            // Devolve o fator sem alteracao
            return fator();
        }
        // Parenteses: avalia a subexpressao interna
        if (c == '(') {
            // Consome o '('
            pos++;
            // Avalia tudo que esta dentro dos parenteses
            double valor = expressao();
            // Exige o ')' correspondente
            if (pos >= texto.length() || texto.charAt(pos) != ')') {
                throw new IllegalArgumentException("parentese nao fechado");
            }
            // Consome o ')'
            pos++;
            // Devolve o valor da subexpressao
            return valor;
        }
        // Caso contrario, deve ser um numero: marca onde ele comeca
        int inicio = pos;
        // Avanca enquanto forem digitos ou ponto decimal
        while (pos < texto.length() && (Character.isDigit(texto.charAt(pos)) || texto.charAt(pos) == '.')) {
            pos++;
        }
        // Se nada foi lido, o caractere nao e valido nessa posicao (ex.: letras, "*3")
        if (inicio == pos) {
            throw new IllegalArgumentException("caractere inesperado: '" + c + "'");
        }
        // Tenta converter o trecho lido para numero
        try {
            return Double.parseDouble(texto.substring(inicio, pos));
        } catch (NumberFormatException e) {
            // Trata casos como "1.2.3"
            throw new IllegalArgumentException("numero invalido: " + texto.substring(inicio, pos));
        }
    }
}
