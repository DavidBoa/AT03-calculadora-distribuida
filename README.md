# AT03 – Calculadora Distribuída

Atividade **AT03 – Invocação Remota e Comunicação Indireta** (Sistemas Distribuídos e Redes de Comunicação).

A ideia é implementar a mesma calculadora remota usando paradigmas de comunicação diferentes e depois compará-los. Este README descreve os projetos de **Sockets** e **RMI**.


## Estrutura do repositório

```
.
├── JavaServerSocket_Calculadora/   Servidor da calculadora com Sockets
├── JavaClientSocket_Calculadora/   Cliente da calculadora com Sockets
└── ExemploRMI/                     Servidor e cliente da calculadora com RMI
```

## O que a calculadora faz

Os dois projetos (Sockets e RMI) oferecem as mesmas funcionalidades, com o mesmo menu no console:

| Opção | Ação |
|---|---|
| 1 | Somar |
| 2 | Subtrair |
| 3 | Dividir (recusa divisão por zero) |
| 4 | Multiplicar |
| 5 | Expressão, **abordagem 1**: o cliente interpreta a expressão e chama o servidor a cada operação básica |
| 6 | Expressão, **abordagem 2**: o cliente envia a expressão inteira e o servidor calcula |
| 0 | Sair |

As expressões aceitam `+ - * /`, parênteses, números decimais (com ponto) e sinal negativo, respeitando a precedência. Exemplo: `(10 + 15) * 4` resulta em `100.0`.

Em ambos os projetos, **quem executa as contas é sempre o servidor**. Na abordagem 1, o cliente só decide a ordem das operações.

### Interpretação de expressões

O arquivo `AvaliadorExpressao.java` (presente nos dois projetos, idêntico) lê a expressão por descida recursiva (expressão → termo → fator). Ele não faz contas: delega cada operação a um `Operador`.

- **Abordagem 1 (cliente):** o `Operador` faz uma chamada de rede ao servidor para cada operação.
- **Abordagem 2 (servidor):** o `Operador` calcula localmente, na memória do servidor.

Para `(10+15)*4`, a abordagem 1 gera **2** chamadas ao servidor (soma e multiplicação) e a abordagem 2 gera **1**.

## Requisitos

- JDK 11 ou superior (o projeto RMI foi configurado para Java 8 e roda normalmente em versões mais novas).
- **VS Code** com a extensão **Extension Pack for Java** (Microsoft), ou **Eclipse**. No Eclipse, os projetos podem ser importados em **File > Import > Existing Projects into Workspace**, pois têm os arquivos de configuração dele.
- Para usar os comandos de terminal (`javac` e `java`), o JDK precisa estar no PATH do sistema. Para conferir, rode `javac -version`.

## Como executar (resumo)

Em todos os projetos, **suba o servidor primeiro** e depois o cliente. Há duas formas:

- **Pelo editor (VS Code ou Eclipse):** abra o arquivo e clique em **Run** (no VS Code, o botão aparece acima do `main` ou é o triângulo no canto superior direito; no Eclipse, **Run As > Java Application**). Cada programa abre o seu próprio terminal ou console, e é nele que você digita as opções do menu do cliente.
- **Pelo terminal:** compilar com `javac *.java` e executar com `java NomeDaClasse`, como mostrado em cada projeto abaixo.

## Projeto 1: Sockets

### Como executar

**Pelo VS Code ou Eclipse** :

1. Abra a pasta `JavaServerSocket_Calculadora`, abra `CalculadoraServerSocket.java` e clique em **Run**. No console deve aparecer `Servidor no ar`.
2. Abra a pasta `JavaClientSocket_Calculadora` (no VS Code, em outra janela: **File > New Window**), abra `src/CalculadoraClientSocket.java` e clique em **Run**. O menu aparece no terminal desse programa.

Para apontar o cliente para outro computador, é preciso passar o IP como argumento. No Eclipse isso fica em **Run > Run Configurations > Arguments**. No VS Code, o mais simples é usar o terminal, como abaixo.

**Pelo terminal**, suba o **servidor primeiro**, depois o cliente, cada um em um terminal.

```bash
# Terminal 1 – servidor
cd JavaServerSocket_Calculadora
javac *.java
java CalculadoraServerSocket

# Terminal 2 – cliente
cd JavaClientSocket_Calculadora/src
javac *.java
java CalculadoraClientSocket              # servidor na mesma máquina (localhost)
java CalculadoraClientSocket 192.168.0.11 # servidor em outra máquina (informe o IP)
```

O servidor escuta na porta **9090**.

### Arquivos

| Arquivo | Papel |
|---|---|
| `CalculadoraServerSocket.java` | Servidor: aceita conexões, lê a requisição, calcula e responde |
| `Calculadora.java` | Regras da calculadora (soma, subtração, multiplicação, divisão, expressão) |
| `CalculadoraClientSocket.java` | Cliente com menu: envia requisições e mostra os resultados |
| `AvaliadorExpressao.java` | Interpretador de expressões (usado pelo servidor e pelo cliente) |

### Protocolo

Texto sobre TCP, **uma requisição por conexão**, um campo por linha (terminada em `\n`).

| Requisição | Linhas enviadas pelo cliente |
|---|---|
| Operação básica | código (`1` somar, `2` subtrair, `3` dividir, `4` multiplicar), operando 1, operando 2 |
| Expressão | `5`, expressão (ex.: `(10+15)*4`) |

A resposta é uma linha com o resultado (ex.: `100.0`) ou `ERRO: <mensagem>`.

Exemplo de operação básica (`10 + 15`):

```
cliente -> servidor:   1
                       10.0
                       15.0
servidor -> cliente:   25.0
```

## Projeto 2: RMI

### Como executar

**Pelo VS Code ou Eclipse** (sem digitar comandos), com a pasta `ExemploRMI` aberta:

1. Abra `src/Calculadora.java` e clique em **Run**. No console deve aparecer `Servidor no ar`.
2. Abra `src/CalculadoraCliente.java` e clique em **Run**. O menu aparece no terminal desse programa.

Os dois programas ficam rodando ao mesmo tempo, cada um no seu terminal. No VS Code, alterne entre eles pela lista de terminais, no lado direito do painel inferior. Para usar o IP de outra máquina, prefira o terminal, como abaixo.

**Pelo terminal**, suba o **servidor primeiro**, depois o cliente.

```bash
cd ExemploRMI/src
javac *.java

# Terminal 1 – servidor
java Calculadora

# Terminal 2 – cliente
java CalculadoraCliente              # servidor na mesma máquina
java CalculadoraCliente 192.168.0.11 # servidor em outra máquina
```

Com duas máquinas, informe também o IP no servidor (`java Calculadora 192.168.0.11`), para o RMI entregar ao cliente o endereço correto.

Portas: **1099** (RMI Registry) e **1100** (objeto remoto).

### Arquivos

| Arquivo | Papel |
|---|---|
| `ICalculadora.java` | Interface remota: os métodos que o cliente pode chamar |
| `Calculadora.java` | Objeto remoto e servidor (o `main` cria o Registry e registra o objeto como `calculadora`) |
| `CalculadoraCliente.java` | Cliente com menu: localiza o objeto no Registry e chama os métodos |
| `AvaliadorExpressao.java` | Interpretador de expressões (igual ao dos Sockets) |

### Como funciona

1. O servidor exporta o objeto `Calculadora` e o registra no Registry com o nome `calculadora`.
2. O cliente consulta o Registry (`lookup`) e recebe um **stub**, que implementa `ICalculadora`.
3. Chamar `calc.soma(10, 15)` no cliente executa o método no servidor. O RMI cuida de serializar os dados e de transportá-los.
4. Erros do servidor (divisão por zero, expressão inválida) chegam ao cliente como exceções Java.

O servidor registra no console cada chamada remota (`Metodo soma chamado 0`), o que permite ver a diferença entre as abordagens: a opção 5 gera várias linhas e a opção 6 gera uma.

## Como testar

Com o servidor no ar, rode o cliente e experimente:

| Entrada | Resultado esperado |
|---|---|
| Opção 1, operandos `10` e `15` | `resultado=25.0` |
| Opção 3, operandos `5` e `0` | `Erro: divisao por zero` |
| Opção 5 ou 6, `(10+15)*4` | `resultado=100.0` |
| Opção 5 ou 6, `2+3*4` | `resultado=14.0` |
| Opção 5 ou 6, `8/(4-4)` | `Erro: divisao por zero` |
| Opção 5 ou 6, `2*(3` | `Erro: parentese nao fechado` |

Para comparar as abordagens, observe o console do servidor:

| Expressão | Opção 5 (cliente interpreta) | Opção 6 (servidor calcula) |
|---|---|---|
| `(10+15)*4` | 2 chamadas | 1 chamada |
| `2*3+4*5` | 3 chamadas | 1 chamada |
| `2*(3` (inválida) | 0 chamadas (o cliente detecta o erro) | 1 chamada |

## Sockets x RMI

| | Sockets | RMI |
|---|---|---|
| Como o cliente pede uma conta | Monta e envia uma mensagem de texto | Chama um método (`calc.soma(a, b)`) |
| Protocolo | Definido pelo programador | Fornecido pelo RMI |
| Erros | Texto `ERRO:` interpretado pelo cliente | Exceções Java |
| Atendimento de clientes | Um por vez (servidor sequencial) | Uma thread por chamada |
| Interoperabilidade | Alta (qualquer linguagem que fale o protocolo) | Apenas Java |
| Tráfego na rede | Texto legível | Serialização binária |

## Limitações conhecidas

- O servidor de Sockets atende um cliente por vez.
- O avaliador remove os espaços antes de ler a expressão, então `2 3` é lido como `23`.
- Números decimais usam ponto (`1.5`); no menu de operações básicas a vírgula também é aceita.
- O RMI exige cliente e servidor em Java e a mesma interface nos dois lados.